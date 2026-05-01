# SFU Reconnection Logic

Cross-platform reference for the unified reconnection flow used by the Android SDK.
iOS and React Native SDKs should implement equivalent behavior.

---

## Reconnection Strategies

The SFU communicates a `WebsocketReconnectStrategy` that determines how the client should recover:

| Strategy | What it does | Session impact |
|----------|-------------|----------------|
| **FAST** | Reconnects the SFU WebSocket and restarts ICE (publisher + subscriber) | Reuses existing session — no participant churn |
| **REJOIN** | Creates a new session with a new session ID and joins as a new participant | Old session is cleaned up; SFU transfers state via `previous_session_id` |
| **MIGRATE** | Moves to a different SFU edge (load balancing / failover) | New session on a new SFU; old SFU session is torn down |
| **DISCONNECT** | Server-initiated disconnection — client should leave the call | Call is terminated |
| **UNSPECIFIED** | No strategy provided — treated as FAST | Same as FAST |

---

## Reconnection Triggers

There are three independent triggers that can start a reconnection. All of them funnel into a single reconnection loop. A mutex ensures only one loop runs at a time — concurrent triggers are dropped.

### 1. SFU WebSocket Error (OkHttp detects WS failure)

- **When:** OkHttp reports a WebSocket error (connection reset, timeout, server crash, etc.)
- **State transition:** SFU socket moves to `DisconnectedTemporarily`
- **Behavior:** If the device has network connectivity, immediately starts the reconnect loop with the strategy provided by the SFU or use UNSPECIFIED if nothing is specified. If network is down, the trigger is skipped (the loop is not started).

### 2. HealthMonitor Timeout (no SFU events received)

- **When:** The SFU health check monitor detects that no events have been received within the expected interval.
- **State transition:** SFU socket moves to `WebSocketEventLost`
- **Behavior:** Always starts the reconnect loop with FAST strategy, regardless of network state.

### 3. Network Recovery (device regains connectivity)

- **When:** Android's `ConnectivityManager` reports that network is available again after being down.
- **Behavior:** Starts the reconnect loop with FAST if the downtime is within the SFU's fast-reconnect deadline, otherwise starts with REJOIN.

---

## The Reconnect Loop

Once triggered, the reconnect loop runs with the following logic:

```
┌─────────────────────────────────────────────────┐
│              RECONNECT LOOP START                │
│         (mutex ensures single instance)          │
└──────────────────────┬──────────────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │ Check exit     │──── Connected / Disconnected / ReconnectingFailed → EXIT
              │ conditions     │──── loopIteration >= MAX_RECONNECT_ATTEMPTS (10) → GIVE UP
              │                │──── elapsed > leaveAfterDisconnectSeconds (30s) → GIVE UP
              └───────┬────────┘
                      │
                      ▼
              ┌────────────────┐
              │ Execute the    │
              │ current        │
              │ strategy       │
              └───────┬────────┘
                      │
            ┌─────────┼──────────────┬──────────────────┐
            ▼         ▼              ▼                   ▼
         Success   PeerConn      Precon-              Failed
            │      Stale         dition                  │
            │         │          Not Met                  │
            ▼         ▼             ▼                     ▼
          EXIT    Escalate     GIVE UP            Decide next
                  to REJOIN                       strategy
                                                     │
                                          ┌──────────┼──────────┐
                                          ▼                     ▼
                                   Escalate to            Keep current
                                   REJOIN if:             strategy
                                   • was MIGRATE OR
                                   • past SFU deadline OR
                                   • FAST attempts >= 3
                                          │                     │
                                          └──────────┬──────────┘
                                                     │
                                                     ▼
                                              delay(500ms)
                                              loop back ↑
```

### Escalation Rules

Strategies only escalate upward, never downward:

- **FAST / UNSPECIFIED** → retries as FAST until escalation conditions are met → **REJOIN**
- **MIGRATE** → if it fails → **REJOIN**
- **REJOIN** → retries as REJOIN (never downgrades to FAST)
- **DISCONNECT** → leaves the call immediately (no retry)

### Escalation from FAST to REJOIN happens when ANY of these is true:

1. The loop has run **3 or more iterations** (`MAX_FAST_RECONNECT_ATTEMPTS`)
2. The elapsed time exceeds the **SFU's fast-reconnect deadline** (`fastReconnectDeadlineSeconds`, provided in `JoinCallResponse`)
3. A peer connection is in **CLOSED** state (the underlying object is disposed and cannot be restarted with ICE — only a full REJOIN with new peer connections can recover)

---

## Terminal States

### Give Up → Leave

When the reconnect loop exhausts all attempts or times out, it sets the connection state to `ReconnectingFailed` and **leaves the call**. This ensures no zombie calls remain.

### Network-Down Safety Net

If the network drops and the reconnect loop was never started (because the trigger was skipped due to no connectivity), a separate timer (`leaveAfterDisconnectSeconds`) starts counting. If the network does not return within this window, the client leaves the call. This is the only safety net for the case where the reconnect loop never runs.

---

## Constants

| Constant | Default Value | Source | Purpose |
|----------|--------------|--------|---------|
| `MAX_FAST_RECONNECT_ATTEMPTS` | 3 | Client | Max FAST attempts before escalating to REJOIN |
| `MAX_RECONNECT_ATTEMPTS` | 10 | Client | Absolute cap on total loop iterations (all strategies combined) |
| `RECONNECT_DELAY_MS` | 500 ms | Client | Delay between consecutive reconnect attempts |
| `fastReconnectDeadlineSeconds` | ~10 s (default) | SFU (via `JoinCallResponse`) | Time window in which the SFU preserves session state for fast reconnect |
| `leaveAfterDisconnectSeconds` | 30 s | Client (configurable via `StreamVideoBuilder`) | Max time the client waits before leaving the call after network loss |
| `DEFAULT_SOCKET_TIMEOUT` | 10 s | Client | Timeout for SFU WebSocket connection handshake |

---

## ICE Restart After Fast Reconnect

After a successful FAST reconnect (SFU WebSocket re-established), the client **unconditionally** restarts ICE on both:

1. **Publisher** peer connection — calls `restartIce()` to renegotiate the outbound media path
2. **Subscriber** peer connection — sends an ICE restart request to the SFU

This happens regardless of the current peer connection state (CONNECTED, FAILED, DISCONNECTED, etc.). The reason: even if the peer connection appears healthy, the old ICE candidates may point to a dead network path after a network change (e.g., WiFi → cellular, or new IP after WiFi reconnect). Proactively restarting ICE ensures fresh candidates are gathered and media flows over the new path.

> **Note:** If a peer connection is in the **CLOSED** state (disposed), ICE restart is not possible. This is detected *before* the restart step and escalates to a full REJOIN instead.

---

## Concurrency

- A **mutex** (`tryLock`) ensures only one reconnect loop runs at a time. Concurrent triggers are dropped with a log message.
- The mutex is released in a `finally` block to guarantee cleanup even on exceptions or coroutine cancellation.
- The `reconnectAttempts` counter (sent to the SFU as telemetry) only increments for REJOIN and MIGRATE — strategies that create a new session.
