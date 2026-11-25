package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.QrDecodedAction

/**
 * Pure Kotlin QR / Barcode payload classifier producing typed safe actions
 * for URL, Wi-Fi, vCard, Email, Phone, and plain text.
 */
object QrPayloadParser {
    fun parse(rawValue: String): QrDecodedAction {
        val trimmed = rawValue.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> {
                QrDecodedAction.Url(rawValue = rawValue, url = trimmed)
            }
            trimmed.startsWith("WIFI:", ignoreCase = true) -> parseWifi(rawValue, trimmed)
            trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> parseVCard(rawValue, trimmed)
            trimmed.startsWith("mailto:", ignoreCase = true) -> parseMailto(rawValue, trimmed)
            trimmed.startsWith("tel:", ignoreCase = true) -> {
                QrDecodedAction.Phone(rawValue = rawValue, number = trimmed.substringAfter(':').trim())
            }
            else -> QrDecodedAction.PlainText(rawValue = rawValue)
        }
    }

    private fun parseWifi(raw: String, trimmed: String): QrDecodedAction.Wifi {
        val body = trimmed.removePrefix("WIFI:").removePrefix("wifi:")
        var ssid = ""
        var password = ""
        var security = "WPA"
        for (segment in body.split(';')) {
            when {
                segment.startsWith("S:", ignoreCase = true) -> ssid = segment.drop(2)
                segment.startsWith("P:", ignoreCase = true) -> password = segment.drop(2)
                segment.startsWith("T:", ignoreCase = true) -> security = segment.drop(2).ifBlank { "nopass" }
            }
        }
        return QrDecodedAction.Wifi(
            rawValue = raw,
            ssid = ssid.ifBlank { "Wi-Fi Network" },
            password = password,
            securityType = security,
        )
    }

    private fun parseVCard(raw: String, trimmed: String): QrDecodedAction.VCard {
        var name = ""
        var phone: String? = null
        var email: String? = null
        var org: String? = null
        for (line in trimmed.lines()) {
            val upper = line.uppercase()
            when {
                upper.startsWith("FN:") -> name = line.substringAfter(':').trim()
                upper.startsWith("N:") && name.isBlank() -> name = line.substringAfter(':').replace(';', ' ').trim()
                upper.startsWith("TEL") -> phone = line.substringAfter(':').trim()
                upper.startsWith("EMAIL") -> email = line.substringAfter(':').trim()
                upper.startsWith("ORG:") -> org = line.substringAfter(':').trim()
            }
        }
        return QrDecodedAction.VCard(
            rawValue = raw,
            fullName = name.ifBlank { "Contact" },
            phone = phone,
            email = email,
            org = org,
        )
    }

    private fun parseMailto(raw: String, trimmed: String): QrDecodedAction.Email {
        val withoutScheme = trimmed.substringAfter(':')
        val address = withoutScheme.substringBefore('?').trim()
        val query = withoutScheme.substringAfter('?', "")
        val subject = query.split('&')
            .firstOrNull { it.startsWith("subject=", ignoreCase = true) }
            ?.substringAfter('=')
        return QrDecodedAction.Email(rawValue = raw, address = address, subject = subject)
    }
}
