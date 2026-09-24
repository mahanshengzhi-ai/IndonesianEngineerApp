package com.mahanshengzhi.indonesianengineer.model

data class Vocabulary(
    val id: String,
    val indonesian: String,
    val chinese: String,
    val category: String,
    val scene: String,
    val example: String = "",
    val exampleChinese: String = ""
)

data class SentencePattern(
    val id: String,
    val indonesian: String,
    val chinese: String,
    val scene: String,
    val explanation: String = "",
    val example: String = ""
)

data class SceneLine(
    val speaker: String,
    val indonesian: String,
    val chinese: String
)

data class LearningScene(
    val id: String,
    val title: String,
    val description: String,
    val lines: List<SceneLine>
)

data class LetterPronunciation(
    val letter: String,
    val name: String,
    val hint: String,
    val example: String
)
