package com.myorderapp.data.remote.supabase

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseNullProfileSourceTest {
    @Test
    fun `legacy null profile fields are coerced and cleaned`() {
        val provider = Files.readString(mainPath("data/remote/supabase/SupabaseClientProvider.kt"))
        val sql = Files.readString(projectPath("table/32_profile_null_session_cleanup.sql"))

        assertTrue(provider.contains("coerceInputValues = true"))
        assertTrue(provider.contains("ignoreUnknownKeys = true"))
        assertTrue(sql.contains("where session_id is null"))
        assertTrue(sql.contains("alter column session_id set not null"))
        assertTrue(sql.contains("where session_updated_at is null"))
        assertTrue(sql.contains("alter column session_updated_at set not null"))
    }

    private fun mainPath(relativePath: String) = listOf(
        Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
        Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
    ).first(Files::exists)

    private fun projectPath(relativePath: String) = listOf(
        Paths.get(relativePath),
        Paths.get("..").resolve(relativePath)
    ).first(Files::exists)
}
