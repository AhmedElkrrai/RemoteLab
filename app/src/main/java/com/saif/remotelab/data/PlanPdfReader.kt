package com.saif.remotelab.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Extracts the text layer of a PDF. Scanned (image-only) PDFs have none and come back blank. */
class PlanPdfReader(private val context: Context) {

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("The file could not be opened")
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                // Position sorting keeps the cells of one table row on one line.
                PDFTextStripper().apply { sortByPosition = true }.getText(document)
            }
        }
    }
}
