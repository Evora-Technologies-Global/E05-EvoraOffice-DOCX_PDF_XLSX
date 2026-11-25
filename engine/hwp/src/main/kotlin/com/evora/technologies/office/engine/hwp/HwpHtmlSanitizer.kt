package com.evora.technologies.office.engine.hwp

import java.text.Normalizer

/**
 * Escapes untrusted document text and removes dangerous HTML tags/attributes
 * so HWP/HWPX documents render inside a network-disabled WebView safely (US-023).
 */
object HwpHtmlSanitizer {

    fun escapeHtml(raw: String): String = buildString(raw.length + 16) {
        raw.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    fun stripUnsafeMarkup(rawHtml: String): String = rawHtml
        .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<iframe[^>]*>[\\s\\S]*?</iframe>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<(svg|object|embed)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<(svg|object|embed)[^>]*/>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\son[a-z]+\\s*=\\s*(['\"][\\s\\S]*?\\1|[^\\s>]+)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("(href|src)\\s*=\\s*(['\"])\\s*(https?:|javascript:|data:|vbscript:)[\\s\\S]*?\\2", RegexOption.IGNORE_CASE), "$1=\"#\"")

    fun wrapDocumentHtml(
        title: String,
        bodyHtml: String,
    ): String {
        val safeTitle = escapeHtml(Normalizer.normalize(title, Normalizer.Form.NFC))
        val cleanedBody = stripUnsafeMarkup(bodyHtml)
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline';" />
              <title>$safeTitle</title>
              <style>
                body {
                  font-family: sans-serif;
                  line-height: 1.65;
                  padding: 18px 16px 64px 16px;
                  margin: 0;
                  word-break: break-word;
                }
                h1.doc-title { font-size: 1.15rem; margin-bottom: 14px; border-bottom: 1px solid rgba(127,127,127,0.25); padding-bottom: 8px; }
                p { margin: 0 0 10px 0; font-size: 0.96rem; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 0.9rem; }
                td, th { border: 1px solid rgba(127,127,127,0.35); padding: 6px 8px; text-align: left; vertical-align: top; }
              </style>
            </head>
            <body>
              <h1 class="doc-title">$safeTitle</h1>
              $cleanedBody
            </body>
            </html>
        """.trimIndent()
    }
}
