package com.myorderapp.data.remote.supabase

import com.myorderapp.ApiConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = ApiConfig.SUPABASE_URL,
            supabaseKey = ApiConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
        }
    }
}
