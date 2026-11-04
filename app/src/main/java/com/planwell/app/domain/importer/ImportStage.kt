package com.planwell.app.domain.importer

sealed class ImportStage {
    data class Progress(val label: String) : ImportStage()
    data class Ready(val entry: ImportedEntry) : ImportStage()
    data class Failed(val message: String) : ImportStage()
}
