package com.planwell.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.planwell.app.ui.call.AddCallScreen
import com.planwell.app.ui.category.CategoryScreen
import com.planwell.app.ui.home.HomeScreen
import com.planwell.app.ui.opportunity.AddEditOpportunityScreen
import com.planwell.app.ui.opportunity.OpportunityDetailScreen
import com.planwell.app.ui.opportunity.OpportunityExtractionReviewScreen
import com.planwell.app.ui.opportunity.OpportunityInboxScreen
import com.planwell.app.ui.search.SearchScreen
import com.planwell.app.ui.settings.SettingsScreen
import com.planwell.app.ui.task.AddEditTaskScreen
import com.planwell.app.ui.task.TaskDetailScreen
import com.planwell.app.ui.task.TrashScreen

@Composable
fun PlanWellNavGraph(
    deepLinkTaskId: Long? = null,
    onDeepLinkHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deepLinkTaskId) {
        val id = deepLinkTaskId ?: return@LaunchedEffect
        navController.navigate(Routes.taskDetail(id)) {
            launchSingleTop = true
        }
        onDeepLinkHandled()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddTask = { navController.navigate(Routes.addTask()) },
                onScheduleCall = { navController.navigate(Routes.ADD_CALL) },
                onAddEvent = { navController.navigate(Routes.addTask("event")) },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenCategories = { navController.navigate(Routes.CATEGORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenInbox = { navController.navigate(Routes.OPPORTUNITY_INBOX) },
                onAddOpportunityManual = { navController.navigate(Routes.ADD_OPPORTUNITY) },
                onOpenOpportunity = { id ->
                    navController.navigate(Routes.opportunityDetail(id))
                },
                onOpenExtraction = { id ->
                    navController.navigate(Routes.opportunityExtraction(id))
                },
                snackbarHostState = snackbarHostState,
            )
        }
        composable(
            route = Routes.ADD_TASK,
            arguments = listOf(
                navArgument("preset") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                },
            ),
        ) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ADD_CALL) {
            AddCallScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT_TASK,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.editTask(id)) },
                onOpenOpportunity = { id ->
                    navController.navigate(Routes.opportunityDetail(id))
                },
            )
        }
        composable(Routes.TRASH) {
            TrashScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CATEGORY) {
            CategoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OPPORTUNITY_INBOX) {
            OpportunityInboxScreen(
                onBack = { navController.popBackStack() },
                onOpenOpportunity = { id ->
                    navController.navigate(Routes.opportunityDetail(id))
                },
                onAddOpportunityManual = { navController.navigate(Routes.ADD_OPPORTUNITY) },
                onOpenExtraction = { id ->
                    navController.navigate(Routes.opportunityExtraction(id))
                },
            )
        }
        composable(Routes.ADD_OPPORTUNITY) {
            AddEditOpportunityScreen(
                onDone = { id ->
                    if (id != null) {
                        navController.navigate(Routes.opportunityDetail(id)) {
                            popUpTo(Routes.OPPORTUNITY_INBOX)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT_OPPORTUNITY,
            arguments = listOf(navArgument("opportunityId") { type = NavType.LongType }),
        ) {
            AddEditOpportunityScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.OPPORTUNITY_EXTRACTION,
            arguments = listOf(navArgument("opportunityId") { type = NavType.LongType }),
        ) {
            OpportunityExtractionReviewScreen(
                onDone = { opportunityId, openAct ->
                    if (openAct) {
                        navController.navigate(Routes.opportunityDetail(opportunityId)) {
                            popUpTo(Routes.HOME)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.OPPORTUNITY_DETAIL,
            arguments = listOf(navArgument("opportunityId") { type = NavType.LongType }),
        ) {
            OpportunityDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.editOpportunity(id)) },
                onOpenExtractionReview = { id ->
                    navController.navigate(Routes.opportunityExtraction(id))
                },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
            )
        }
    }
}
