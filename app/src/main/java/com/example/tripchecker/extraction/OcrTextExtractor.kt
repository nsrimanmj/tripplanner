package com.example.tripchecker.extraction

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.extraction.model.ExtractedText
import com.example.tripchecker.extraction.model.TextLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.sqrt

/** OCR fallback using PdfRenderer and bundled ML Kit Latin model. */
class OcrTextExtractor : PdfTextExtractor {
    private val mutex = Mutex()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun extract(file: File): ExtractedText? =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            val page0 = ocrPage(renderer, 0)
                            val combined =
                                if (page0.contains("PNR", ignoreCase = true)) {
                                    page0
                                } else {
                                    val page1 =
                                        if (renderer.pageCount > 1) {
                                            ocrPage(renderer, 1)
                                        } else {
                                            ""
                                        }
                                    page0 + "\n" + page1
                                }
                            if (combined.isBlank()) return@withContext null
                            ExtractedText(
                                text = combined,
                                method = ExtractionMethod.OCR,
                                pageCount = minOf(2, renderer.pageCount),
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: OutOfMemoryError) {
                    null
                } catch (_: Exception) {
                    null
                }
            }
        }

    private suspend fun ocrPage(renderer: PdfRenderer, index: Int): String {
        renderer.openPage(index).use { page ->
            val pageW = page.width.toFloat().coerceAtLeast(1f)
            val pageH = page.height.toFloat().coerceAtLeast(1f)
            val scaleByWidth = MAX_RENDER_WIDTH / pageW
            val scaleByPixels = sqrt(MAX_PIXELS / (pageW * pageH))
            val scale = min(scaleByWidth, scaleByPixels).coerceAtMost(4f)
            val width = (pageW * scale).toInt().coerceAtLeast(1)
            val height = (pageH * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                val lines = mutableListOf<TextLine>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox
                        if (box != null) {
                            lines.add(
                                TextLine(
                                    text = line.text,
                                    left = box.left.toFloat(),
                                    top = box.top.toFloat(),
                                    right = box.right.toFloat(),
                                    bottom = box.bottom.toFloat(),
                                ),
                            )
                        }
                    }
                }
                return RowMerger.merge(lines)
            } finally {
                bitmap.recycle()
            }
        }
    }

    private companion object {
        const val MAX_RENDER_WIDTH = 2000f
        const val MAX_PIXELS = 6_000_000f
    }
}
