package com.mahanshengzhi.indonesianengineer.data

import android.content.Context
import com.mahanshengzhi.indonesianengineer.model.LearningScene
import com.mahanshengzhi.indonesianengineer.model.LetterPronunciation
import com.mahanshengzhi.indonesianengineer.model.SceneLine
import com.mahanshengzhi.indonesianengineer.model.SentencePattern
import com.mahanshengzhi.indonesianengineer.model.Vocabulary

class LearningRepository(private val context: Context) {
    fun letters(): List<LetterPronunciation> =
        AssetTsvReader.read(context, "letters.tsv").map {
            LetterPronunciation(
                it["letter"].orEmpty(),
                it["name"].orEmpty(),
                it["hint"].orEmpty(),
                it["example"].orEmpty()
            )
        }

    fun sentences(): List<SentencePattern> =
        AssetTsvReader.read(context, "sentences.tsv").map {
            SentencePattern(
                it["id"].orEmpty(),
                it["indonesian"].orEmpty(),
                it["chinese"].orEmpty(),
                it["scene"].orEmpty(),
                it["explanation"].orEmpty(),
                it["example"].orEmpty()
            )
        }

    fun scenes(): List<LearningScene> {
        val rows = AssetTsvReader.read(context, "scenes.tsv")
        return rows.groupBy { it["scene_id"].orEmpty() }.values.map { group ->
            val first = group.first()
            LearningScene(
                id = first["scene_id"].orEmpty(),
                title = first["title"].orEmpty(),
                description = first["description"].orEmpty(),
                lines = group.sortedBy { it["turn"]?.toIntOrNull() ?: Int.MAX_VALUE }.map {
                    SceneLine(
                        speaker = it["speaker"].orEmpty(),
                        indonesian = it["indonesian"].orEmpty(),
                        chinese = it["chinese"].orEmpty()
                    )
                }
            )
        }
    }

    fun vocabulary(): Sequence<Vocabulary> = sequence {
        for (index in 1..11) {
            val file = "words_%02d.tsv".format(index)
            val rows = runCatching { AssetTsvReader.read(context, file) }.getOrDefault(emptyList())
            for (row in rows) {
                yield(
                    Vocabulary(
                        row["id"].orEmpty(),
                        row["indonesian"].orEmpty(),
                        row["chinese"].orEmpty(),
                        row["category"].orEmpty(),
                        row["scene"].orEmpty(),
                        row["example"].orEmpty(),
                        row["example_chinese"].orEmpty()
                    )
                )
            }
        }
    }
}
