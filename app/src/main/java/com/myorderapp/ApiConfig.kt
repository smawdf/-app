package com.myorderapp

object ApiConfig {
    val TIAN_API_KEY = BuildConfig.TIAN_API_KEY

    val SPOONACULAR_API_KEY = try {
        val field = BuildConfig::class.java.getDeclaredField("SPOONACULAR_API_KEY")
        field.isAccessible = true
        field.get(null) as? String ?: ""
    } catch (_: Exception) {
        "c4d077f93cbd4545ae6337c03b1c925a"
    }

    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
}
