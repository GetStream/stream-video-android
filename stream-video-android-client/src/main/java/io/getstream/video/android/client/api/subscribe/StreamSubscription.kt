package io.getstream.video.android.client.api.subscribe

/** Type alias for the subscription IDs. **/
public interface StreamSubscription {

    /**
     * The subscription ID.
     */
    public val id: Int

    /**
     * Cancels the subscription.
     */
    public fun cancel() : Result<Unit>
}
