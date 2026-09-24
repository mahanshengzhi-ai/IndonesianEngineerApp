package com.mahanshengzhi.indonesianengineer.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationEngine {
    private var translator: Translator? = null

    fun prepareChineseToIndonesian() = prepare(TranslateLanguage.CHINESE, TranslateLanguage.INDONESIAN)
    fun prepareIndonesianToChinese() = prepare(TranslateLanguage.INDONESIAN, TranslateLanguage.CHINESE)

    private fun prepare(source: String, target: String) {
        translator?.close()
        translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
        )
    }

    fun download(onReady: () -> Unit, onError: (Exception) -> Unit) {
        val current = translator ?: return
        current.downloadModelIfNeeded(
            DownloadConditions.Builder().requireWifi().build()
        ).addOnSuccessListener { onReady() }
         .addOnFailureListener { onError(it) }
    }

    fun translate(text: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val current = translator ?: return
        current.translate(text).addOnSuccessListener(onSuccess).addOnFailureListener(onError)
    }

    fun close() {
        translator?.close()
        translator = null
    }
}
