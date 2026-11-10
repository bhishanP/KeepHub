package com.keephub.core.data.daily

data class LeveledWord(
    val term: String,
    val pos: String,          // e.g. "adjective", "noun", "verb"
    val ipa: String,          // e.g. "/ˈdɪlɪdʒənt/"
    val definition: String,
    val example: String,
    val mnemonic: String,     // High-retention memory anchor / tip
    val level: VocabularyLevel
)
