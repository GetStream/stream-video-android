package io.getstream.video.android.core.model

public sealed class RejectReason {

    public abstract val alias: String

    public data object Busy : RejectReason() {
        public override val alias: String = "busy"
    }

    public data object Cancel : RejectReason() {
        public override val alias: String = "cancel"
    }

    public data object Decline : RejectReason() {
        public override val alias: String = "decline"
    }

    public data class Custom(public override val alias: String) : RejectReason()

}