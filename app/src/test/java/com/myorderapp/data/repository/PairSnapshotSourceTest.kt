package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PairSnapshotSourceTest {
    @Test
    fun `pair state profile and notice come from one authoritative rpc`() {
        val repository = readSource("data/repository/SupabaseProfileRepository.kt")
        val sql = Files.readString(projectPath("table/28_pair_snapshot.sql"))

        assertTrue(sql.contains("create or replace function public.current_pair_snapshot()"))
        assertTrue(sql.contains("partner_avatar_url text"))
        assertTrue(sql.contains("and e.read_at is null"))
        assertTrue(repository.contains("rpc(\"current_pair_snapshot\")"))
        assertTrue(repository.contains(".toPairInfo()"))
        assertTrue(repository.contains("partnerAvatarUrl = partnerAvatarUrl.takeIfCloudAvatarUrl()"))
        assertTrue(repository.contains("noticeMessage = noticeMessage"))
        assertTrue(repository.contains("loadPairInfoFallback()"))
        assertTrue(repository.contains("decodeList<RemotePairEvent>()"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(Paths.get("src/main/java/com/myorderapp").resolve(relativePath), Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)).first(Files::exists)
    )
    private fun projectPath(relativePath: String) = listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath)).first(Files::exists)
}
