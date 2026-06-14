package com.example.core.config

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = AppKeys.SUPABASE_URL,
            supabaseKey = AppKeys.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
