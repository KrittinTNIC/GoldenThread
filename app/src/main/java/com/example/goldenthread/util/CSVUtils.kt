package com.example.goldenthread.util

import android.content.Context
import android.util.Log
import com.example.goldenthread.model.Drama
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "CSVUtils"

fun loadDramasFromCSV(context: Context): List<Drama> {
    val dramaList = mutableListOf<Drama>()
    val inputStream = context.assets.open("GoldenThread Data - Dramas.csv")
    val reader = BufferedReader(InputStreamReader(inputStream))

    // skip header
    reader.readLine()

    reader.forEachLine { line ->
        if (line.isBlank()) return@forEachLine

        val tokens = parseCsvLine(line)

        // Defensive: tokens might be shorter if some rows are malformed
        val dramaId = tokens.getOrNull(0)?.trim().orEmpty()
        val titleEn = tokens.getOrNull(1)?.trim().orEmpty()
        val titleTh = tokens.getOrNull(2)?.trim().orEmpty()
        val releaseYearInt = tokens.getOrNull(3)?.trim()?.toIntOrNull() ?: 0
        val duration = tokens.getOrNull(4)?.trim().orEmpty()
        val summary = tokens.getOrNull(5)?.trim().orEmpty()
        val posterRaw = tokens.getOrNull(6)?.trim().orEmpty()
        val genre = tokens.getOrNull(7)?.trim().orEmpty()

        // Clean poster URL:
        // 1) Split on whitespace, pick the last token that looks like an image URL (.jpg/.png/.jpeg/.webp)
        // 2) If none found, pick the last token that starts with http
        var posterUrl = posterRaw.split(Regex("\\s+"))
            .lastOrNull {
                it.startsWith("http", ignoreCase = true) &&
                        (it.contains(".jpg", ignoreCase = true) ||
                                it.contains(".png", ignoreCase = true) ||
                                it.contains(".jpeg", ignoreCase = true) ||
                                it.contains(".webp", ignoreCase = true))
            }?.trim()?.trim('"') ?: posterRaw.split(Regex("\\s+"))
            .lastOrNull { it.startsWith("http", ignoreCase = true) }?.trim()?.trim('"') ?: ""

        // Final safe-trim in case there are trailing commas/quotes
        posterUrl = posterUrl.trim().trimEnd(',', '"')

        val drama = Drama(
            dramaId = dramaId,
            titleEn = titleEn,
            titleTh = titleTh,
            releaseYear = releaseYearInt,
            duration = duration,
            summary = summary,
            posterUrl = posterUrl,
            genre = genre
        )

        Log.d(TAG, "Parsed drama $dramaId posterUrl='$posterUrl'") // debug: check in Logcat
        dramaList.add(drama)
    }

    reader.close()
    return dramaList
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                // handle escaped quotes ("")
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i++ // skip the escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                result.add(cur.toString())
                cur.setLength(0)
            }
            else -> cur.append(c)
        }
        i++
    }
    // add last field
    result.add(cur.toString())
    return result
}
