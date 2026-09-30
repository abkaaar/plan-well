package com.planwell.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.planwell.app.ui.theme.PlanWellTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyStateComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionButtonInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            PlanWellTheme {
                EmptyState(
                    title = "Nothing planned yet",
                    message = "Tap + to add your first task",
                    actionLabel = "Add task",
                    onAction = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("Nothing planned yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add task").performClick()
        assertTrue(clicked)
    }
}
