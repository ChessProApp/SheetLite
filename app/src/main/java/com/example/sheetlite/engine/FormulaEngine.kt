package com.example.sheetlite.engine

import java.util.Locale

object FormulaEngine {

    private val cellRefRegex = Regex("""^([A-Z]+)(\d+)$""")
    private val rangeRegex = Regex("""^([A-Z]+\d+):([A-Z]+\d+)$""")
    private val tokenRegex = Regex("""[A-Z]+\d+|\d+(?:\.\d+)?|[+\-*/()]""")

    fun colIndexToName(col: Int): String {
        var index = col
        val sb = StringBuilder()
        while (index >= 0) {
            sb.append(('A'.code + (index % 26)).toChar())
            index = (index / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun cellCoordsToName(row: Int, col: Int): String = "${colIndexToName(col)}${row + 1}"

    fun parseCellRef(ref: String): Pair<Int, Int>? {
        val match = cellRefRegex.matchEntire(ref.trim().uppercase(Locale.US)) ?: return null
        val colStr = match.groupValues[1]
        val rowNum = match.groupValues[2].toIntOrNull() ?: return null
        if (rowNum < 1) return null

        var col = 0
        for (ch in colStr) {
            col = col * 26 + (ch.code - 'A'.code + 1)
        }
        return Pair(rowNum - 1, col - 1)
    }

    fun evaluate(
        row: Int,
        col: Int,
        rawMap: Map<Pair<Int, Int>, String>,
        visited: Set<Pair<Int, Int>> = emptySet()
    ): String {
        val key = Pair(row, col)
        if (key in visited) return "#CYCLE!"
        val raw = rawMap[key]?.trim() ?: return ""
        if (!raw.startsWith("=")) return raw

        val expr = raw.substring(1).trim().uppercase(Locale.US)
        if (expr.isEmpty()) return ""

        val nextVisited = visited + key
        return try {
            val result = when {
                expr.startsWith("SUM(") && expr.endsWith(")") -> {
                    evalAggregate(expr.removePrefix("SUM(").removeSuffix(")"), rawMap, nextVisited).sum()
                }
                expr.startsWith("AVG(") && expr.endsWith(")") -> {
                    val values = evalAggregate(expr.removePrefix("AVG(").removeSuffix(")"), rawMap, nextVisited)
                    if (values.isEmpty()) 0.0 else values.average()
                }
                expr.startsWith("MIN(") && expr.endsWith(")") -> {
                    evalAggregate(expr.removePrefix("MIN(").removeSuffix(")"), rawMap, nextVisited).minOrNull() ?: 0.0
                }
                expr.startsWith("MAX(") && expr.endsWith(")") -> {
                    evalAggregate(expr.removePrefix("MAX(").removeSuffix(")"), rawMap, nextVisited).maxOrNull() ?: 0.0
                }
                else -> evalArithmetic(expr, rawMap, nextVisited)
            }
            formatNumber(result)
        } catch (e: CycleException) {
            "#CYCLE!"
        } catch (e: Exception) {
            "#ERR!"
        }
    }

    private class CycleException : RuntimeException()

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "#DIV/0!"
        return if (value % 1.0 == 0.0 && value in -1e15..1e15) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun getNumericValue(
        row: Int,
        col: Int,
        rawMap: Map<Pair<Int, Int>, String>,
        visited: Set<Pair<Int, Int>>
    ): Double {
        val evaluated = evaluate(row, col, rawMap, visited)
        if (evaluated == "#CYCLE!") throw CycleException()
        if (evaluated.startsWith("#")) throw IllegalArgumentException(evaluated)
        return evaluated.toDoubleOrNull() ?: 0.0
    }

    private fun evalAggregate(
        inner: String,
        rawMap: Map<Pair<Int, Int>, String>,
        visited: Set<Pair<Int, Int>>
    ): List<Double> {
        val values = mutableListOf<Double>()
        val parts = inner.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            val rangeMatch = rangeRegex.matchEntire(trimmed)
            if (rangeMatch != null) {
                val start = parseCellRef(rangeMatch.groupValues[1]) ?: throw IllegalArgumentException()
                val end = parseCellRef(rangeMatch.groupValues[2]) ?: throw IllegalArgumentException()
                val rMin = minOf(start.first, end.first)
                val rMax = maxOf(start.first, end.first)
                val cMin = minOf(start.second, end.second)
                val cMax = maxOf(start.second, end.second)
                for (r in rMin..rMax) {
                    for (c in cMin..cMax) {
                        val strVal = evaluate(r, c, rawMap, visited)
                        if (strVal == "#CYCLE!") throw CycleException()
                        strVal.toDoubleOrNull()?.let { values.add(it) }
                    }
                }
            } else {
                val coords = parseCellRef(trimmed)
                if (coords != null) {
                    val strVal = evaluate(coords.first, coords.second, rawMap, visited)
                    if (strVal == "#CYCLE!") throw CycleException()
                    strVal.toDoubleOrNull()?.let { values.add(it) }
                } else {
                    trimmed.toDoubleOrNull()?.let { values.add(it) }
                }
            }
        }
        return values
    }

    private fun evalArithmetic(
        expr: String,
        rawMap: Map<Pair<Int, Int>, String>,
        visited: Set<Pair<Int, Int>>
    ): Double {
        val clean = expr.replace(" ", "")
        val tokens = tokenRegex.findAll(clean).map { it.value }.toList()
        if (tokens.isEmpty() || tokens.joinToString("") != clean) {
            throw IllegalArgumentException("Invalid expression")
        }

        var pos = 0

        fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos++]
                val right = parseTerm()
                left = if (op == "+") left + right else left - right
            }
            return left
        }

        fun parseTerm(): Double {
            var left = parseFactor()
            while (pos < tokens.size && (tokens[pos] == "*" || tokens[pos] == "/")) {
                val op = tokens[pos++]
                val right = parseFactor()
                if (op == "*") {
                    left *= right
                } else {
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    left /= right
                }
            }
            return left
        }

        fun parseFactor(): Double {
            if (pos >= tokens.size) throw IllegalArgumentException("Unexpected end")
            val token = tokens[pos]
            if (token == "+") {
                pos++
                return parseFactor()
            }
            if (token == "-") {
                pos++
                return -parseFactor()
            }
            if (token == "(") {
                pos++
                val value = parseExpression()
                if (pos >= tokens.size || tokens[pos] != ")") {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                pos++
                return value
            }
            pos++
            val coords = parseCellRef(token)
            return if (coords != null) {
                getNumericValue(coords.first, coords.second, rawMap, visited)
            } else {
                token.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid token: $token")
            }
        }

        val result = parseExpression()
        if (pos != tokens.size) throw IllegalArgumentException("Unexpected trailing tokens")
        return result
    }
}
