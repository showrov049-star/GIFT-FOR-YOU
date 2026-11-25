package com.example.data.util

import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin QR Code generator (Byte mode, ECC Level L, Versions 1..6)
 * so every published surprise gets a real, scannable QR matrix rendered in Compose Canvas
 * with zero extra network or external library dependencies.
 */
object QrCodeGenerator {

    data class QrMatrix(val size: Int, val modules: Array<BooleanArray>)

    fun encode(text: String): QrMatrix {
        val dataBytes = text.toByteArray(StandardCharsets.UTF_8)
        val version = selectVersion(dataBytes.size)
        val size = version * 4 + 17
        val totalCodewords = TOTAL_DATA_CW[version]
        val eccCount = ECC_CW_PER_BLOCK[version]

        val bits = MutableList(0) { false }
        fun appendBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add(((value ushr i) and 1) != 0)
            }
        }

        // Mode 0100 (Byte) + 8-bit length
        appendBits(0b0100, 4)
        appendBits(dataBytes.size, 8)
        for (b in dataBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val capacityBits = totalCodewords * 8
        val termBits = minOf(4, capacityBits - bits.size)
        appendBits(0, termBits)
        while (bits.size % 8 != 0) bits.add(false)
        var padByte = 0xEC
        while (bits.size < capacityBits) {
            appendBits(padByte, 8)
            padByte = if (padByte == 0xEC) 0x11 else 0xEC
        }

        val dataCw = IntArray(totalCodewords) { idx ->
            var v = 0
            for (b in 0 until 8) {
                if (bits[idx * 8 + b]) v = v or (1 shl (7 - b))
            }
            v
        }

        val eccCw = reedSolomonRemainder(dataCw, eccCount)
        val allCw = IntArray(dataCw.size + eccCw.size)
        dataCw.copyInto(allCw, 0)
        eccCw.copyInto(allCw, dataCw.size)

        val grid = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunctionModule(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until size && y in 0 until size) {
                grid[y][x] = dark
                isFunction[y][x] = true
            }
        }

        // Finder patterns
        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val dark = dist != 2 && dist != 4
                    setFunctionModule(cx + dx, cy + dy, dark)
                }
            }
        }
        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            setFunctionModule(i, 6, dark)
            setFunctionModule(6, i, dark)
        }

        // Alignment pattern for version >= 2
        if (version >= 2) {
            val pos = ALIGNMENT_POS[version]
            for (ay in pos) {
                for (ax in pos) {
                    if ((ax == 6 && ay == 6) || (ax == 6 && ay == size - 7) || (ax == size - 7 && ay == 6)) continue
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val dark = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1
                            setFunctionModule(ax + dx, ay + dy, dark)
                        }
                    }
                }
            }
        }

        // Reserve format info + dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunctionModule(8, i, false)
                setFunctionModule(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunctionModule(size - 1 - i, 8, false)
            if (i < 7) setFunctionModule(8, size - 1 - i, false)
        }
        setFunctionModule(8, size - 8, true)

        // Place data bits (Mask 0: (x + y) % 2 == 0)
        var bitIdx = 0
        val totalBits = allCw.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val y = if (((right + 1) and 2) == 0) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y][x]) {
                        var dark = false
                        if (bitIdx < totalBits) {
                            dark = ((allCw[bitIdx ushr 3] ushr (7 - (bitIdx and 7))) and 1) != 0
                            bitIdx++
                        }
                        if ((x + y) % 2 == 0) dark = !dark
                        grid[y][x] = dark
                    }
                }
            }
            right -= 2
        }

        // Format info for ECC L (01) + Mask 0 (000) -> 0b01000 -> encoded 0x77C4
        val formatBits = 0x77C4
        for (i in 0..5) setFunctionModule(8, i, ((formatBits ushr i) and 1) != 0)
        setFunctionModule(8, 7, ((formatBits ushr 6) and 1) != 0)
        setFunctionModule(8, 8, ((formatBits ushr 7) and 1) != 0)
        setFunctionModule(7, 8, ((formatBits ushr 8) and 1) != 0)
        for (i in 9..14) setFunctionModule(14 - i, 8, ((formatBits ushr i) and 1) != 0)

        for (i in 0..7) setFunctionModule(size - 1 - i, 8, ((formatBits ushr i) and 1) != 0)
        for (i in 8..14) setFunctionModule(8, size - 15 + i, ((formatBits ushr i) and 1) != 0)

        return QrMatrix(size, grid)
    }

    private fun selectVersion(byteLen: Int): Int {
        for (v in 1..6) {
            if (byteLen + 2 <= TOTAL_DATA_CW[v]) return v
        }
        return 6
    }

    private fun reedSolomonRemainder(data: IntArray, degree: Int): IntArray {
        val gen = IntArray(degree)
        gen[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                gen[j] = gfMultiply(gen[j], root)
                if (j + 1 < degree) gen[j] = gen[j] xor gen[j + 1]
            }
            root = gfMultiply(root, 2)
        }
        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            for (i in 0 until degree) {
                result[i] = result[i] xor gfMultiply(gen[i], factor)
            }
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    // Single-block ECC Level L capacities for Versions 1..6
    private val TOTAL_DATA_CW = intArrayOf(0, 19, 34, 55, 80, 108, 136)
    private val ECC_CW_PER_BLOCK = intArrayOf(0, 7, 10, 15, 20, 26, 36)
    private val ALIGNMENT_POS = arrayOf(
        intArrayOf(),
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34)
    )
}
