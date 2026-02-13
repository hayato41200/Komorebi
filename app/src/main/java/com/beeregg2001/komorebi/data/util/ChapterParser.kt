package com.beeregg2001.komorebi.data.util

import com.beeregg2001.komorebi.data.model.RecordedChapter
import java.io.File

object ChapterParser {

    private val timeRegex = Regex("(\\d{1,2}):(\\d{2}):(\\d{2})(?:[\\.,](\\d{1,3}))?")
    private const val fallbackChapterTitle = "先頭"

    fun parseAmatsukazeTxt(content: String, durationSeconds: Long? = null): List<RecordedChapter> {
        if (content.isBlank()) {
            return fallbackChapters()
        }

        val parsed = content.lineSequence()
            .mapNotNull { line -> parseChapterLine(line, durationSeconds) }
            .distinctBy { it.positionSeconds }
            .sortedBy { it.positionSeconds }
            .toList()

        return parsed.ifEmpty { fallbackChapters() }
    }

    fun parseFromRecordedFilePath(recordedFilePath: String, durationSeconds: Long? = null): List<RecordedChapter> {
        val sidecarFile = resolveAmatsukazeFile(recordedFilePath) ?: return emptyList()
        val content = runCatching { sidecarFile.readText() }.getOrNull() ?: return emptyList()
        return parseAmatsukazeTxt(content, durationSeconds)
    }

    private fun parseChapterLine(line: String, durationSeconds: Long?): RecordedChapter? {
        val match = timeRegex.find(line) ?: return null
        val h = match.groupValues[1].toLongOrNull() ?: return null
        val m = match.groupValues[2].toLongOrNull() ?: return null
        val s = match.groupValues[3].toLongOrNull() ?: return null
        val millis = match.groupValues[4].toLongOrNull()?.let { if (it > 99) it else it * 10 } ?: 0L

        val rawSeconds = h * 3600 + m * 60 + s + millis / 1000
        val safeSeconds = if (durationSeconds != null) rawSeconds.coerceIn(0L, durationSeconds) else rawSeconds.coerceAtLeast(0L)

        val title = line
            .replace(match.value, "")
            .replace(Regex("^[\\s\\t,:：\\-\\]]+"), "")
            .ifBlank { "チャプター ${safeSeconds}s" }

        return RecordedChapter(title = title, positionSeconds = safeSeconds)
    }

    private fun resolveAmatsukazeFile(recordedFilePath: String): File? {
        if (recordedFilePath.isBlank()) return null
        val videoFile = File(recordedFilePath)
        val baseName = videoFile.nameWithoutExtension
        val dir = videoFile.parentFile ?: return null

        val candidates = listOf(
            File(dir, "$baseName.txt"),
            File(dir, "$baseName.chapters.txt"),
            File(dir, "${videoFile.name}.txt")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    private fun fallbackChapters(): List<RecordedChapter> {
        return listOf(RecordedChapter(title = fallbackChapterTitle, positionSeconds = 0L))
    }
}
