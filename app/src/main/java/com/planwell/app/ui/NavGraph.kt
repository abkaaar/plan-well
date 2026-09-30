package com.planwell.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.planwell.app.ui.category.CategoryScreen
import com.planwell.app.ui.home.HomeScreen
import com.planwell.app.ui.search.SearchScreen
import com.planwell.app.ui.task.AddEditTaskScreen
import com.planwell.app.ui.task.TaskDetailScreen
import com.planwell.app.ui.task.TrashScreen

@Composable
fun PlanWellNavGraph(
    deepLinkTaskId: Long? = null,
    onDeepLinkHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
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
                onAddTask = { navController.navigate(Routes.ADD_TASK) },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenCategories = { navController.navigate(Routes.CATEGORY) },
            )
        }
        composable(Routes.ADD_TASK) {
            AddEditTaskScreen(
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
    }
}
