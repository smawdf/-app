package com.myorderapp.ui.auth

import java.security.MessageDigest

object AccountIdentifier {
    private const val INTERNAL_AUTH_DOMAIN = "accounts.gaotangxiaoshi.app"

    fun normalizeForAuth(input: String): String {
        val account = input.trim()
        if (account.contains("@")) return account

        val digitsOnly = account.filter(Char::isDigit)
        val localPart = if (isPhoneLike(account, digitsOnly)) {
            "phone-$digitsOnly"
        } else {
            "user-${stableHash(account)}"
        }
        return "$localPart@$INTERNAL_AUTH_DOMAIN"
    }

    fun isRealEmail(input: String): Boolean {
        val account = input.trim()
        return account.contains("@") && account.substringAfter("@").contains(".")
    }

    private fun isPhoneLike(account: String, digitsOnly: String): Boolean {
        return digitsOnly.length in 6..20 &&
            account.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
    }

    private fun stableHash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.lowercase().toByteArray())
        return bytes.take(10).joinToString("") { "%02x".format(it) }
    }
}
