package com.mahanshengzhi.indonesianengineer.data

import android.content.Context

object AssetTsvReader {
    fun read(context: Context, path: String): List<Map<String, String>> {
        val lines = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readLines() }
        if (lines.isEmpty()) return emptyList()
        val headers = lines.first().split('	')
        return lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val cells = line.split('	')
            headers.mapIndexedNotNull { index, key ->
                cells.getOrNull(index)?.let { key to it }
            }.toMap()
        }
    }
}
