package es.gob.midni.qrdemo

object C40 {

    private val shift2Set = "!\"#$%&'()*+,-./:;<=>?@[\\]^_"

    fun decode(input: ByteArray): String {
        require(input.size % 2 == 0) { "C40 requiere numero par de bytes." }

        val out = StringBuilder()
        var shift = 0

        for (i in input.indices step 2) {
            val i1 = input[i].toUByte().toInt()
            val i2 = input[i + 1].toUByte().toInt()
            val full = (i1 shl 8) + i2 - 1

            val c1 = full / 1600
            val c2 = (full % 1600) / 40
            val c3 = full % 40

            val triple = intArrayOf(c1, c2, c3)
            for (c in triple) {
                when (shift) {
                    0 -> {
                        when (c) {
                            0 -> shift = 1
                            1 -> shift = 2
                            2 -> shift = 3
                            3 -> out.append(' ')
                            in 4..13 -> out.append((c - 4).digitToChar())
                            in 14..39 -> out.append(('A'.code + (c - 14)).toChar())
                            else -> error("Valor C40 invalido: $c")
                        }
                    }

                    1 -> {
                        out.append(c.toChar())
                        shift = 0
                    }

                    2 -> {
                        if (c < shift2Set.length) {
                            out.append(shift2Set[c])
                        } else {
                            error("Valor C40 Shift2 invalido: $c")
                        }
                        shift = 0
                    }

                    3 -> {
                        out.append((c + 96).toChar())
                        shift = 0
                    }
                }
            }
        }

        return out.toString().trimEnd()
    }
}
