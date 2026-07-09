package com.myorderapp.data.remote.supabase

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudErrorLoggerSourceTest {

    @Test
    fun `cloud error logger writes scoped client logs`() {
        val logger = readMainSource("data/remote/supabase/CloudErrorLogger.kt")
        val appModule = readMainSource("di/AppModule.kt")
        val sql = Files.readString(
            listOf(
                Paths.get("table/20_client_error_logs.sql"),
                Paths.get("../table/20_client_error_logs.sql")
            ).firstOrNull { Files.exists(it) } ?: error("client error log SQL not found")
        )

        listOf(
            "client_error_logs",
            "userId = userId",
            "area = area.take(64)",
            "action = action.take(96)",
            "Logging must never break the user flow"
        ).forEach { expected ->
            assertTrue("CloudErrorLogger missing marker: $expected", logger.contains(expected))
        }

        listOf(
            "single { CloudErrorLogger(get()) }",
            "SupabaseStorageUploader(get(), get())",
            "SupabaseProfileRepository(get(), androidContext(), get(), get())",
            "SupabaseOrderRepository(get(), get(), get(), get(), get())"
        ).forEach { expected ->
            assertTrue("DI missing cloud logger wiring: $expected", appModule.contains(expected))
        }

        listOf(
            "create table if not exists public.client_error_logs",
            "user_id uuid references auth.users(id)",
            "with check (user_id = auth.uid())",
            "using (user_id = auth.uid())",
            "idx_client_error_logs_user_created"
        ).forEach { expected ->
            assertTrue("Cloud log SQL missing marker: $expected", sql.contains(expected))
        }
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
