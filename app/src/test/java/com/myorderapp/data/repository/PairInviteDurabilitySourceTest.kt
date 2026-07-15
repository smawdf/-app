package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairInviteDurabilitySourceTest {
    @Test
    fun `pair invite is stored independently until accepted`() {
        val repository = readSource("data/repository/SupabaseProfileRepository.kt")
        val sql = Files.readString(projectPath("table/26_pair_invites.sql"))

        assertTrue(sql.contains("create table if not exists public.pair_invites"))
        assertTrue(sql.contains("expires_at > now()"))
        assertTrue(sql.contains("for update"))
        assertTrue(sql.contains("set pair_id = normalized_code"))
        val generateBody = functionBody(repository, "generatePairCode")
        assertFalse(generateBody.contains("pairId = result.pairCode"))
        assertFalse(generateBody.contains("session.setPairId(result.pairCode)"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(Paths.get("src/main/java/com/myorderapp").resolve(relativePath), Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)).first(Files::exists)
    )
    private fun projectPath(relativePath: String) = listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath)).first(Files::exists)
    private fun functionBody(source: String, name: String): String {
        val start = source.indexOf("fun $name")
        val bodyStart = source.indexOf('{', start)
        var depth = 0
        for (index in bodyStart until source.length) {
            if (source[index] == '{') depth++
            if (source[index] == '}' && --depth == 0) return source.substring(bodyStart + 1, index)
        }
        error("function body not found")
    }
}
