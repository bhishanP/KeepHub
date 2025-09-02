package com.keephub.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.keephub.app.ui.screens.*

object Routes {
    const val WORD_LIST = "word_list"
    const val ADD_WORD = "add_word?prefill={prefill}"
    const val WORD_DETAIL = "word_detail/{id}"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
}

@Composable
fun KeepHubNavHost(startPrefill: String? = null, startToReview: Boolean) {
    val nav = rememberNavController()
    val startDest = Routes.WORD_LIST
    LaunchedEffect(startToReview) {
        if (startToReview) {
            // Wait one frame so the NavHost is ready
            kotlinx.coroutines.yield()
            nav.navigate(Routes.REVIEW)
        }
    }
    NavHost(navController = nav, startDestination = startDest) {

        composable(Routes.WORD_LIST) {
            WordListScreen(
                onAdd = { nav.navigate("add_word?prefill=") },
                onOpen = { id -> nav.navigate("word_detail/$id") },
                navController = nav
            )
        }

        composable(
            route = Routes.ADD_WORD,
            arguments = listOf(navArgument("prefill") { type = NavType.StringType; nullable = true })
        ) { backStack ->
            val prefillArg = backStack.arguments?.getString("prefill") ?: startPrefill
            AddWordScreen(
                initialTerm = prefillArg ?: "",
                onSaved = { id -> nav.navigate("word_detail/$id") { popUpTo(Routes.WORD_LIST) } },
                onBack = { nav.popBackStack() },
                onOpenDuplicate = { id -> nav.navigate("word_detail/$id") }
            )
        }

        composable(
            route = Routes.WORD_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments!!.getLong("id")
            WordDetailScreen(id = id, onBack = { nav.popBackStack() })
        }

        composable(Routes.REVIEW) { ReviewScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
