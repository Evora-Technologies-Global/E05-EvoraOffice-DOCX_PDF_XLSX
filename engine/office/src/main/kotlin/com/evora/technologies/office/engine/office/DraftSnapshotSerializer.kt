package com.evora.technologies.office.engine.office

import com.evora.technologies.office.core.model.CellHorizontalAlign
import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.CellVerticalAlign
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideElement
import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.model.SlideStrokeStyle
import com.evora.technologies.office.core.model.TextAlignOption
import com.evora.technologies.office.core.model.TrackChangeItem
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.model.WorksheetData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Deterministic serializer for atomic 15s / onStop DraftRecovery checkpoints.
 */
object DraftSnapshotSerializer {
    fun encodeWord(snapshot: WordDocumentSnapshot): ByteArray {
        val root = JSONObject().apply {
            put("title", snapshot.title)
            put("trackChangesEnabled", snapshot.trackChangesEnabled)
            put("pageMarginDp", snapshot.pageMarginDp)
            put("isLandscape", snapshot.isLandscape)
            put("inkNoteSummary", snapshot.inkNoteSummary)
            put(
                "paragraphs",
                JSONArray().apply {
                    snapshot.paragraphs.forEach { p ->
                        put(
                            JSONObject().apply {
                                put("id", p.id)
                                put("text", p.text)
                                put("bold", p.bold)
                                put("italic", p.italic)
                                put("underline", p.underline)
                                put("strikethrough", p.strikethrough)
                                put("fontSizeSp", p.fontSizeSp)
                                put("textColorArgb", p.textColorArgb)
                                if (p.highlightColorArgb != null) put("highlightColorArgb", p.highlightColorArgb)
                                put("alignment", p.alignment.name)
                                put("isBullet", p.isBullet)
                                put("isNumbered", p.isNumbered)
                                put("indentLevel", p.indentLevel)
                            },
                        )
                    }
                },
            )
            put(
                "trackChanges",
                JSONArray().apply {
                    snapshot.trackChanges.forEach { tc ->
                        put(
                            JSONObject().apply {
                                put("id", tc.id)
                                put("kind", tc.kind.name)
                                put("paragraphIndex", tc.paragraphIndex)
                                put("text", tc.text)
                                put("author", tc.author)
                                put("timestampEpochMs", tc.timestampEpochMs)
                            },
                        )
                    }
                },
            )
        }
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeWord(bytes: ByteArray): WordDocumentSnapshot {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        val pArr = root.optJSONArray("paragraphs") ?: JSONArray()
        val paragraphs = (0 until pArr.length()).map { i ->
            val o = pArr.getJSONObject(i)
            WordParagraph(
                id = o.optString("id", "p_$i"),
                text = o.optString("text", ""),
                bold = o.optBoolean("bold", false),
                italic = o.optBoolean("italic", false),
                underline = o.optBoolean("underline", false),
                strikethrough = o.optBoolean("strikethrough", false),
                fontSizeSp = o.optInt("fontSizeSp", 14),
                textColorArgb = o.optLong("textColorArgb", 0xFF0F172AL),
                highlightColorArgb = if (o.has("highlightColorArgb")) o.optLong("highlightColorArgb") else null,
                alignment = runCatching { TextAlignOption.valueOf(o.optString("alignment")) }.getOrDefault(TextAlignOption.LEFT),
                isBullet = o.optBoolean("isBullet", false),
                isNumbered = o.optBoolean("isNumbered", false),
                indentLevel = o.optInt("indentLevel", 0),
            )
        }
        val tcArr = root.optJSONArray("trackChanges") ?: JSONArray()
        val trackChanges = (0 until tcArr.length()).map { i ->
            val o = tcArr.getJSONObject(i)
            TrackChangeItem(
                id = o.optString("id", "tc_$i"),
                kind = runCatching { TrackChangeKind.valueOf(o.optString("kind")) }.getOrDefault(TrackChangeKind.COMMENT),
                paragraphIndex = o.optInt("paragraphIndex", 0),
                text = o.optString("text", ""),
                author = o.optString("author", "Evora User"),
                timestampEpochMs = o.optLong("timestampEpochMs", 0L),
            )
        }
        return WordDocumentSnapshot(
            title = root.optString("title", "Document.docx"),
            paragraphs = paragraphs,
            trackChangesEnabled = root.optBoolean("trackChangesEnabled", false),
            trackChanges = trackChanges,
            pageMarginDp = root.optInt("pageMarginDp", 24),
            isLandscape = root.optBoolean("isLandscape", false),
            inkNoteSummary = root.optString("inkNoteSummary", ""),
        )
    }

    fun encodeWorkbook(snapshot: WorkbookSnapshot): ByteArray {
        val root = JSONObject().apply {
            put("title", snapshot.title)
            put("activeSheetIndex", snapshot.activeSheetIndex)
            put(
                "sheets",
                JSONArray().apply {
                    snapshot.sheets.forEach { s ->
                        put(
                            JSONObject().apply {
                                put("name", s.name)
                                put("rowCount", s.rowCount)
                                put("colCount", s.colCount)
                                put("frozenRows", s.frozenRows)
                                put("frozenCols", s.frozenCols)
                                put(
                                    "cells",
                                    JSONArray().apply {
                                        s.cells.values.forEach { c ->
                                            put(
                                                JSONObject().apply {
                                                    put("row", c.row)
                                                    put("col", c.col)
                                                    put("rawInput", c.rawInput)
                                                    put("evaluatedDisplay", c.evaluatedDisplay)
                                                    put("bold", c.bold)
                                                    if (c.fillColorArgb != null) put("fillColorArgb", c.fillColorArgb)
                                                    put("textColorArgb", c.textColorArgb)
                                                    put("hAlign", c.hAlign.name)
                                                    put("vAlign", c.vAlign.name)
                                                    put("numberFormat", c.numberFormat.name)
                                                    put("mergedSpanRows", c.mergedSpanRows)
                                                    put("mergedSpanCols", c.mergedSpanCols)
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeWorkbook(bytes: ByteArray): WorkbookSnapshot {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        val sArr = root.optJSONArray("sheets") ?: JSONArray()
        val sheets = (0 until sArr.length()).map { i ->
            val so = sArr.getJSONObject(i)
            val cArr = so.optJSONArray("cells") ?: JSONArray()
            val cellMap = LinkedHashMap<String, ExcelCell>()
            for (ci in 0 until cArr.length()) {
                val co = cArr.getJSONObject(ci)
                val r = co.optInt("row", 0)
                val c = co.optInt("col", 0)
                cellMap["$r,$c"] = ExcelCell(
                    row = r,
                    col = c,
                    rawInput = co.optString("rawInput", ""),
                    evaluatedDisplay = co.optString("evaluatedDisplay", ""),
                    bold = co.optBoolean("bold", false),
                    fillColorArgb = if (co.has("fillColorArgb")) co.optLong("fillColorArgb") else null,
                    textColorArgb = co.optLong("textColorArgb", 0xFF0F172AL),
                    hAlign = runCatching { CellHorizontalAlign.valueOf(co.optString("hAlign")) }.getOrDefault(CellHorizontalAlign.LEFT),
                    vAlign = runCatching { CellVerticalAlign.valueOf(co.optString("vAlign")) }.getOrDefault(CellVerticalAlign.MIDDLE),
                    numberFormat = runCatching { CellNumberFormat.valueOf(co.optString("numberFormat")) }.getOrDefault(CellNumberFormat.GENERAL),
                    mergedSpanRows = co.optInt("mergedSpanRows", 1),
                    mergedSpanCols = co.optInt("mergedSpanCols", 1),
                )
            }
            WorksheetData(
                name = so.optString("name", "Sheet${i + 1}"),
                rowCount = so.optInt("rowCount", 20),
                colCount = so.optInt("colCount", 8),
                frozenRows = so.optInt("frozenRows", 1),
                frozenCols = so.optInt("frozenCols", 0),
                cells = cellMap,
            )
        }
        return WorkbookSnapshot(
            title = root.optString("title", "Workbook.xlsx"),
            sheets = sheets,
            activeSheetIndex = root.optInt("activeSheetIndex", 0),
        )
    }

    fun encodeSlideDeck(snapshot: SlideDeckSnapshot): ByteArray {
        val root = JSONObject().apply {
            put("title", snapshot.title)
            put(
                "slides",
                JSONArray().apply {
                    snapshot.slides.forEach { slide ->
                        put(
                            JSONObject().apply {
                                put("id", slide.id)
                                put("presenterNotes", slide.presenterNotes)
                                put("backgroundColorArgb", slide.backgroundColorArgb)
                                put(
                                    "elements",
                                    JSONArray().apply {
                                        slide.elements.forEach { el ->
                                            put(
                                                JSONObject().apply {
                                                    put("id", el.id)
                                                    put("kind", el.kind)
                                                    put("text", el.text)
                                                    put("xFraction", el.xFraction.toDouble())
                                                    put("yFraction", el.yFraction.toDouble())
                                                    put("widthFraction", el.widthFraction.toDouble())
                                                    put("heightFraction", el.heightFraction.toDouble())
                                                    put("fillColorArgb", el.fillColorArgb)
                                                    put("strokeColorArgb", el.strokeColorArgb)
                                                    put("strokeWidthDp", el.strokeWidthDp.toDouble())
                                                    put("strokeStyle", el.strokeStyle.name)
                                                    put("zOrder", el.zOrder)
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeSlideDeck(bytes: ByteArray): SlideDeckSnapshot {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        val sArr = root.optJSONArray("slides") ?: JSONArray()
        val slides = (0 until sArr.length()).map { i ->
            val so = sArr.getJSONObject(i)
            val eArr = so.optJSONArray("elements") ?: JSONArray()
            val elements = (0 until eArr.length()).map { ei ->
                val eo = eArr.getJSONObject(ei)
                SlideElement(
                    id = eo.optString("id", "el_${i}_$ei"),
                    kind = eo.optString("kind", "TEXT"),
                    text = eo.optString("text", ""),
                    xFraction = eo.optDouble("xFraction", 0.1).toFloat(),
                    yFraction = eo.optDouble("yFraction", 0.2).toFloat(),
                    widthFraction = eo.optDouble("widthFraction", 0.8).toFloat(),
                    heightFraction = eo.optDouble("heightFraction", 0.2).toFloat(),
                    fillColorArgb = eo.optLong("fillColorArgb", 0xFFE6F1FFL),
                    strokeColorArgb = eo.optLong("strokeColorArgb", 0xFF186FD4L),
                    strokeWidthDp = eo.optDouble("strokeWidthDp", 2.0).toFloat(),
                    strokeStyle = runCatching { SlideStrokeStyle.valueOf(eo.optString("strokeStyle")) }.getOrDefault(SlideStrokeStyle.SOLID),
                    zOrder = eo.optInt("zOrder", ei),
                )
            }
            SlidePageData(
                id = so.optString("id", "slide_$i"),
                elements = elements,
                presenterNotes = so.optString("presenterNotes", ""),
                backgroundColorArgb = so.optLong("backgroundColorArgb", 0xFFFFFFFFL),
            )
        }
        return SlideDeckSnapshot(
            title = root.optString("title", "Presentation.pptx"),
            slides = slides,
        )
    }
}
