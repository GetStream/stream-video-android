package io.getstream.webrtc.noise.cancellation

enum class NoiseCancellationModel(
    val filename: String,
) {

    FullBand(filename = "c6.f.s.ced125.kw"),
    WideBand(filename = "c5.s.w.c9ac8f.kw"),
    NarrowBand(filename = "c5.n.s.20949d.kw"),
    VAD(filename = "VAD_model.kw"),

}