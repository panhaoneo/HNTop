package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.db.StoryEntity
import com.example.ui.StoryCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun story_card_screenshot() {
    val mockStory = StoryEntity(
      id = 12345,
      title = "Show HN: Interactive Space Physics Simulation in WebGL",
      translatedTitle = "Show HN: 基于 WebGL 的交互式空间物理模拟接口",
      synopsis = "一个使用 WebGL 渲染的高性能、交互式太空引力和轨道物理学实时模拟器网页系统。",
      by = "space_explorer",
      score = 256,
      time = System.currentTimeMillis() / 1000 - 3600,
      url = "https://example.com/physics"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        StoryCard(
          index = 1,
          story = mockStory,
          onItemClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
