package com.mahanshengzhi.indonesianengineer.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

enum class TranslationDirection(
    val label: String,
    val source: String,
    val target: String
) {
    CHINESE_TO_INDONESIAN(
        label = "中文 → 印尼语",
        source = TranslateLanguage.CHINESE,
        target = TranslateLanguage.INDONESIAN
    ),
    INDONESIAN_TO_CHINESE(
        label = "印尼语 → 中文",
        source = TranslateLanguage.INDONESIAN,
        target = TranslateLanguage.CHINESE
    )
}

class TranslationEngine {
    private var translator: Translator? = null
    private var currentDirection: TranslationDirection? = null

    fun prepare(direction: TranslationDirection) {
        if (currentDirection == direction && translator != null) return

        translator?.close()
        translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(direction.source)
                .setTargetLanguage(direction.target)
                .build()
        )
        currentDirection = direction
    }

    fun download(onReady: () -> Unit, onError: (Exception) -> Unit) {
        val current = translator
        if (current == null) {
            onError(IllegalStateException("翻译模型尚未初始化"))
            return
        }

        current.downloadModelIfNeeded(
            DownloadConditions.Builder().build()
        ).addOnSuccessListener {
            onReady()
        }.addOnFailureListener {
            onError(it)
        }
    }

    fun translate(
        text: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val current = translator
        if (current == null) {
            onError(IllegalStateException("翻译模型尚未初始化"))
            return
        }

        current.translate(text)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onError)
    }

    fun close() {
        translator?.close()
        translator = null
        currentDirection = null
    }
}
