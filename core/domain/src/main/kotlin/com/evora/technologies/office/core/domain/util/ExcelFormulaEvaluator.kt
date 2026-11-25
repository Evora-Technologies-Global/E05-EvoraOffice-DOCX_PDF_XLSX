package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.WorksheetData
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Pure Kotlin spreadsheet formula evaluator supporting SUM, AVERAGE, COUNT, MIN, MAX, IF,
 * cell references (A1), range references (A1:C5), and number formatting.
 */
object ExcelFormulaEvaluator {
    private val cellRefRegex = Regex("^([A-Za-z]+)([1-9][0-9]*)$")

    fun colIndexToName(colIndex: Int): String {
        var idx = colIndex
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.append(('A'.code + (idx % 26)).toChar())
            idx = (idx / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun parseCellAddress(address: String): Pair<Int, Int>? {
        val match = cellRefRegex.matchEntire(address.trim()) ?: return null
        val colLetters = match.groupValues[1].uppercase(Locale.US)
        val rowOneBased = match.groupValues[2].toIntOrNull() ?: return null
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch.code - 'A'.code + 1)
        }
        return (rowOneBased - 1) to (col - 1)
    }

    fun evaluateSheet(sheet: WorksheetData): WorksheetData {
        val visiting = HashSet<String>()
        val cache = HashMap<String, String>()
        val updatedCells = sheet.cells.toMutableMap()

        for ((key, cell) in sheet.cells) {
            val evaluated = evaluateCell(key, sheet, visiting, cache)
            val formatted = formatValue(evaluated, cell.numberFormat)
            updatedCells[key] = cell.copy(evaluatedDisplay = formatted)
        }
        return sheet.copy(cells = updatedCells)
    }

    fun evaluateExpression(
        rawInput: String,
        sheet: WorksheetData,
        visiting: MutableSet<String> = HashSet(),
        cache: MutableMap<String, String> = HashMap(),
    ): String {
        val trimmed = rawInput.trim()
        if (!trimmed.startsWith("=")) return rawInput
        val body = trimmed.drop(1).trim()
        if (body.isEmpty()) return ""

        val upper = body.uppercase(Locale.US)
        return when {
            upper.startsWith("SUM(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(4, body.length - 1))
                val nums = collectNumbers(args, sheet, visiting, cache)
                formatNumeric(nums.sum())
            }
            upper.startsWith("AVERAGE(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(8, body.length - 1))
                val nums = collectNumbers(args, sheet, visiting, cache)
                if (nums.isEmpty()) "0" else formatNumeric(nums.average())
            }
            upper.startsWith("COUNT(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(6, body.length - 1))
                val nums = collectNumbers(args, sheet, visiting, cache)
                nums.size.toString()
            }
            upper.startsWith("MIN(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(4, body.length - 1))
                val nums = collectNumbers(args, sheet, visiting, cache)
                formatNumeric(nums.minOrNull() ?: 0.0)
            }
            upper.startsWith("MAX(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(4, body.length - 1))
                val nums = collectNumbers(args, sheet, visiting, cache)
                formatNumeric(nums.maxOrNull() ?: 0.0)
            }
            upper.startsWith("IF(") && body.endsWith(")") -> {
                val args = splitArgs(body.substring(3, body.length - 1))
                if (args.size < 2) return "#ARG!"
                val cond = evaluateCondition(args[0], sheet, visiting, cache)
                val branch = if (cond) args[1] else args.getOrElse(2) { "FALSE" }
                resolveValueToken(branch, sheet, visiting, cache)
            }
            else -> evaluateSimpleArithmetic(body, sheet, visiting, cache)
        }
    }

    private fun evaluateCell(
        key: String,
        sheet: WorksheetData,
        visiting: MutableSet<String>,
        cache: MutableMap<String, String>,
    ): String {
        cache[key]?.let { return it }
        if (!visiting.add(key)) return "#CYCLE!"
        val cell = sheet.cells[key]
        val result = if (cell == null) {
            ""
        } else {
            evaluateExpression(cell.rawInput, sheet, visiting, cache)
        }
        visiting.remove(key)
        cache[key] = result
        return result
    }

    private fun collectNumbers(
        args: List<String>,
        sheet: WorksheetData,
        visiting: MutableSet<String>,
        cache: MutableMap<String, String>,
    ): List<Double> {
        val out = mutableListOf<Double>()
        for (arg in args) {
            val trimmed = arg.trim()
            if (':' in trimmed) {
                val parts = trimmed.split(':')
                if (parts.size == 2) {
                    val start = parseCellAddress(parts[0])
                    val end = parseCellAddress(parts[1])
                    if (start != null && end != null) {
                        val r0 = minOf(start.first, end.first)
                        val r1 = maxOf(start.first, end.first)
                        val c0 = minOf(start.second, end.second)
                        val c1 = maxOf(start.second, end.second)
                        for (r in r0..r1) {
                            for (c in c0..c1) {
                                evaluateCell("$r,$c", sheet, visiting, cache).toDoubleOrNull()?.let { out.add(it) }
                            }
                        }
                    }
                }
            } else {
                val resolved = resolveValueToken(trimmed, sheet, visiting, cache)
                resolved.toDoubleOrNull()?.let { out.add(it) }
            }
        }
        return out
    }

    private fun evaluateCondition(
        expr: String,
        sheet: WorksheetData,
        visiting: MutableSet<String>,
        cache: MutableMap<String, String>,
    ): Boolean {
        val operators = listOf(">=", "<=", "!=", "<>", "=", ">", "<")
        for (op in operators) {
            val idx = expr.indexOf(op)
            if (idx > 0) {
                val leftRaw = resolveValueToken(expr.substring(0, idx), sheet, visiting, cache)
                val rightRaw = resolveValueToken(expr.substring(idx + op.length), sheet, visiting, cache)
                val lNum = leftRaw.toDoubleOrNull()
                val rNum = rightRaw.toDoubleOrNull()
                return if (lNum != null && rNum != null) {
                    when (op) {
                        ">=" -> lNum >= rNum
                        "<=" -> lNum <= rNum
                        "!=", "<>" -> lNum != rNum
                        "=" -> lNum == rNum
                        ">" -> lNum > rNum
                        "<" -> lNum < rNum
                        else -> false
                    }
                } else {
                    when (op) {
                        "=" -> leftRaw.equals(rightRaw, ignoreCase = true)
                        "!=", "<>" -> !leftRaw.equals(rightRaw, ignoreCase = true)
                        else -> false
                    }
                }
            }
        }
        val num = resolveValueToken(expr, sheet, visiting, cache).toDoubleOrNull()
        return (num ?: 0.0) != 0.0
    }

    private fun evaluateSimpleArithmetic(
        expr: String,
        sheet: WorksheetData,
        visiting: MutableSet<String>,
        cache: MutableMap<String, String>,
    ): String {
        val singleRef = parseCellAddress(expr)
        if (singleRef != null) {
            return evaluateCell("${singleRef.first},${singleRef.second}", sheet, visiting, cache)
        }
        // Basic binary + - * / over operands (cell refs or numbers)
        val ops = listOf('+', '-', '*', '/')
        for (op in ops) {
            val idx = expr.lastIndexOf(op)
            if (idx > 0) {
                val lhs = evaluateSimpleArithmetic(expr.substring(0, idx).trim(), sheet, visiting, cache).toDoubleOrNull()
                val rhs = evaluateSimpleArithmetic(expr.substring(idx + 1).trim(), sheet, visiting, cache).toDoubleOrNull()
                if (lhs != null && rhs != null) {
                    val value = when (op) {
                        '+' -> lhs + rhs
                        '-' -> lhs - rhs
                        '*' -> lhs * rhs
                        '/' -> if (rhs == 0.0) return "#DIV/0!" else lhs / rhs
                        else -> 0.0
                    }
                    return formatNumeric(value)
                }
            }
        }
        return resolveValueToken(expr, sheet, visiting, cache)
    }

    private fun resolveValueToken(
        token: String,
        sheet: WorksheetData,
        visiting: MutableSet<String>,
        cache: MutableMap<String, String>,
    ): String {
        val t = token.trim()
        if (t.length >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length - 1)
        }
        val addr = parseCellAddress(t)
        if (addr != null) {
            return evaluateCell("${addr.first},${addr.second}", sheet, visiting, cache)
        }
        return t
    }

    private fun splitArgs(raw: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var depth = 0
        for (ch in raw) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                !inQuotes && ch == '(' -> {
                    depth++
                    current.append(ch)
                }
                !inQuotes && ch == ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                !inQuotes && depth == 0 && ch == ',' -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
    }

    private fun formatNumeric(value: Double): String {
        val rounded = value.roundToLong()
        return if (kotlin.math.abs(value - rounded.toDouble()) < 1e-9) {
            rounded.toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun formatValue(raw: String, format: CellNumberFormat): String {
        val num = raw.toDoubleOrNull() ?: return raw
        return when (format) {
            CellNumberFormat.GENERAL -> raw
            CellNumberFormat.NUMBER -> String.format(Locale.US, "%,.2f", num)
            CellNumberFormat.CURRENCY -> String.format(Locale.US, "$%,.2f", num)
            CellNumberFormat.ACCOUNTING -> if (num < 0) {
                String.format(Locale.US, "($%,.2f)", kotlin.math.abs(num))
            } else {
                String.format(Locale.US, "$%,.2f", num)
            }
            CellNumberFormat.PERCENTAGE -> String.format(Locale.US, "%.1f%%", num * 100.0)
            CellNumberFormat.FRACTION -> {
                val whole = num.toLong()
                val frac = num - whole
                if (kotlin.math.abs(frac) < 1e-4) whole.toString() else "$whole ${(frac * 100).roundToLong()}/100"
            }
            CellNumberFormat.DATE -> raw
        }
    }
}
