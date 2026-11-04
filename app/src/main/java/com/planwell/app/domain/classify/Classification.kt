package com.planwell.app.domain.classify

data class Classification(
    val kind: EntryKind,
    val confidence: Float,
)
