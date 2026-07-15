package com.myorderapp.ui.profile

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PairStateRefreshSourceTest {
    @Test
    fun `profile refreshes pair state before invite actions`() {
        val viewModel = readSource("ui/profile/ProfileViewModel.kt")
        val screen = readSource("ui/profile/ProfileScreen.kt")

        assertTrue(viewModel.contains("profileRepository.loadProfile()\n            refreshPairInfo()"))
        assertTrue(viewModel.contains("if (currentInfo.isPaired)"))
        assertTrue(viewModel.contains("你们已经绑定，无需再次生成邀请码"))
        assertTrue(screen.contains("viewModel.refreshPairState()"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(Paths.get("src/main/java/com/myorderapp").resolve(relativePath), Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)).first(Files::exists)
    )
}
