package com.planwell.app.ui.share

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.ui.theme.PlanWellTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareImportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun review_editTitleAndSave() {
        var saved = false
        var draft = EditableImportDraft(
            kind = EntryKind.TASK,
            title = "pay rent",
            notes = "pay rent by the 5th",
            dueOrStartMs = System.currentTimeMillis() + 86_400_000L,
            timeWasExplicit = false,
            phone = null,
            address = null,
            url = null,
            money = null,
            sourceImagePath = null,
            confidence = 0.7f,
        )
        var state: ShareImportState by mutableStateOf(ShareImportState.Review(draft))

        composeRule.setContent {
            PlanWellTheme {
                ShareImportScreen(
                    state = state,
                    onCancel = {},
                    onRetry = {},
                    onSave = { saved = true },
                    onKindChange = { kind ->
                        draft = draft.copy(kind = kind)
                        state = ShareImportState.Review(draft)
                    },
                    onTitleChange = { title ->
                        draft = draft.copy(title = title)
                        state = ShareImportState.Review(draft)
                    },
                    onNotesChange = {},
                    onDueChange = {},
                    onClearPhone = {},
                    onClearAddress = {},
                    onClearUrl = {},
                    onClearMoney = {},
                )
            }
        }

        composeRule.onNodeWithText("Review import").assertIsDisplayed()
        composeRule.onNodeWithText("pay rent").performTextReplacement("Pay rent — Dec")
        composeRule.onNodeWithText("Task").performClick()
        composeRule.onNodeWithText("Save").performClick()
        assertTrue(saved)
        assertEquals("Pay rent — Dec", draft.title)
        assertEquals(EntryKind.TASK, draft.kind)
    }

    @Test
    fun loading_showsStage() {
        composeRule.setContent {
            PlanWellTheme {
                ShareImportScreen(
                    state = ShareImportState.Loading("Reading image…"),
                    onCancel = {},
                    onRetry = {},
                    onSave = {},
                    onKindChange = {},
                    onTitleChange = {},
                    onNotesChange = {},
                    onDueChange = {},
                    onClearPhone = {},
                    onClearAddress = {},
                    onClearUrl = {},
                    onClearMoney = {},
                )
            }
        }
        composeRule.onNodeWithText("Reading image…").assertIsDisplayed()
    }
}
