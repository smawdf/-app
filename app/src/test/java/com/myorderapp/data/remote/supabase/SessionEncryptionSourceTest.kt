package com.myorderapp.data.remote.supabase

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEncryptionSourceTest {
    @Test
    fun sessionSecretsUseAndroidKeystoreAndMigrateLegacyValues() {
        val session = readMainSource("data/remote/supabase/SessionManager.kt")
        val cipher = readMainSource("data/remote/supabase/KeystoreStringCipher.kt")

        assertTrue(session.contains("token_encrypted"))
        assertTrue(session.contains("saved_password_encrypted"))
        assertTrue(session.contains("remove(\"token\")"))
        assertTrue(cipher.contains("AndroidKeyStore"))
        assertTrue(cipher.contains("AES/GCM/NoPadding"))
        assertFalse(session.contains(".putString(\"token\", token)"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
