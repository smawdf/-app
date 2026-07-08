package com.myorderapp.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountIdentifierTest {

    @Test
    fun `phone number is converted to internal auth email`() {
        assertEquals(
            "phone-13800138000@accounts.gaotangxiaoshi.app",
            AccountIdentifier.normalizeForAuth("13800138000")
        )
    }

    @Test
    fun `real email is kept for auth and password reset`() {
        assertEquals(
            "user@example.com",
            AccountIdentifier.normalizeForAuth(" user@example.com ")
        )
        assertTrue(AccountIdentifier.isRealEmail("user@example.com"))
    }

    @Test
    fun `plain account uses stable internal auth email`() {
        val first = AccountIdentifier.normalizeForAuth("tangtang")
        val second = AccountIdentifier.normalizeForAuth("tangtang")

        assertEquals(first, second)
        assertTrue(first.endsWith("@accounts.gaotangxiaoshi.app"))
        assertFalse(AccountIdentifier.isRealEmail("13800138000"))
    }
}
