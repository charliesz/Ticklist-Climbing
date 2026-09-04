package com.charlie.ticklist.data

sealed interface ProgressTransferState {

    data object Idle : ProgressTransferState

    data class Preview(
        val value: ProgressTransferPreview
    ) : ProgressTransferState

    data class Completed(
        val value: ProgressTransferPreview
    ) : ProgressTransferState

    data class Failed(
        val message: String
    ) : ProgressTransferState
}
