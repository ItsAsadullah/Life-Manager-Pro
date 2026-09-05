package com.hisabnikash.app.navigation

sealed class AppDestination(
    val route: String,
    val label: String
) {
    data object Dashboard : AppDestination(
        route = "dashboard",
        label = "হোম"
    )

    data object Transactions : AppDestination(
        route = "transactions",
        label = "লেনদেন"
    )

    data object Reports : AppDestination(
        route = "reports",
        label = "রিপোর্ট"
    )

    data object Settings : AppDestination(
        route = "settings",
        label = "সেটিংস"
    )

    data object MarketMemo : AppDestination(
        route = "market_memo",
        label = "বাজার মেমো"
    )

    data object Notes : AppDestination(
        route = "notes",
        label = "নোটস"
    )

    data object AiChatbot : AppDestination(
        route = "ai_chatbot",
        label = "AI সহকারী"
    )

    data object AiScanner : AppDestination(
        route = "ai_scanner",
        label = "AI স্ক্যানার"
    )
}
