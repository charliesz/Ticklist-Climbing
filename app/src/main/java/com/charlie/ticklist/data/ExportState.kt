package com.charlie.ticklist.data

sealed interface ExportState {

    data object Idle : ExportState

    data class Running(
        val currentFile: Int,
        val totalFiles: Int,
        val currentName: String
    ) : ExportState {
        val progress: Float
            get() {
                if (totalFiles <= 0) {
                    return 0f
                }

                return currentFile.toFloat() /
                        totalFiles.toFloat()
            }
    }

    data object Completed : ExportState

    data class Failed(
        val message: String
    ) : ExportState
}
