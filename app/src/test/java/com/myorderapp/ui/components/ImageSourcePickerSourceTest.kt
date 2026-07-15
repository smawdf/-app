package com.myorderapp.ui.components

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSourcePickerSourceTest {

    @Test
    fun `all editable images support gallery and camera sources`() {
        val picker = readMainSource("ui/components/ImageSourcePicker.kt")
        val onboarding = readMainSource("ui/onboarding/OnboardingScreen.kt")
        val profile = readMainSource("ui/profile/ProfileScreen.kt")
        val menu = readMainSource("ui/menu/MenuManagementScreen.kt")

        listOf(
            "ActivityResultContracts.OpenDocument()",
            "ActivityResultContracts.TakePicture()",
            "FileProvider.getUriForFile",
            "从相册选择",
            "拍照上传",
            "takePersistableUriPermission"
        ).forEach { expected ->
            assertTrue("图片来源组件缺少能力：$expected", picker.contains(expected))
        }

        assertTrue(onboarding.contains("title = \"选择头像\""))
        assertTrue(profile.contains("title = \"选择头像\""))
        assertTrue(menu.contains("title = \"选择菜品图片\""))
        assertTrue(menu.contains("title = \"选择店铺封面\""))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
