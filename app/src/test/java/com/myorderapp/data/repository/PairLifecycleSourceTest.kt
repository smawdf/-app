package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairLifecycleSourceTest {
    @Test
    fun `pair lifecycle syncs both users and emits notifications`() {
        val sql = Files.readString(projectPath("table/27_pair_events_and_atomic_unpair.sql"))
        val profile = readSource("data/repository/SupabaseProfileRepository.kt")
        val home = readSource("ui/couple/CoupleMenuScreen.kt")
        val profileScreen = readSource("ui/profile/ProfileScreen.kt")

        assertTrue(sql.contains("create table if not exists public.pair_events"))
        assertTrue(sql.contains("create or replace function public.unpair_current_pair()"))
        assertTrue(sql.contains("where pair_id = caller_pair"))
        assertTrue(sql.contains("'unpaired'"))
        assertTrue(profile.contains("current_pair_snapshot"))
        assertTrue(profile.contains("rpc(function = \"unpair_current_pair\")"))
        assertFalse(profile.contains("rpc(function = \"unpair_current_pair\").decodeSingle<Boolean>()"))
        assertTrue(home.contains("pairInfo.noticeMessage"))
        assertTrue(profileScreen.contains("确认解除绑定？"))
        assertTrue(profileScreen.contains("对方会收到解绑通知"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(Paths.get("src/main/java/com/myorderapp").resolve(relativePath), Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)).first(Files::exists)
    )
    private fun projectPath(relativePath: String) = listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath)).first(Files::exists)
}

