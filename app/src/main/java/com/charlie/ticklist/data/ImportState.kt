package com.charlie.ticklist.data

sealed interface ImportState {

    data object Idle : ImportState

    data class Running(
        val currentFile: Int,
        val totalFiles: Int,
        val currentName: String
    ) : ImportState {
        val progress: Float
            get() {
                if (totalFiles <= 0) {
                    return 0f
                }

                return currentFile.toFloat() /
                        totalFiles.toFloat()
            }
    }

    data class Completed(
        val collectionName: String
    ) : ImportState

    data class Failed(
        val message: String
    ) : ImportState
}
