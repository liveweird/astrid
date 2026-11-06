package app.astrid.engine

import java.text.Normalizer

internal fun normalizeText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ")
