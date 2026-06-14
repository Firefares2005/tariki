package com.example.core.config

import com.example.BuildConfig

/**
 * Centrally manages all API keys and configuration credentials for the Tariki Application.
 * 
 * Safe Fallback Architecture:
 * - Attempts to read from `BuildConfig` (which retrieves keys injected via .env file or the platform).
 * - If a key is missing or is set to a placeholder (e.g. starts with "YOUR_" or "MY_"), 
 *   it falls back to the hardcoded local string below.
 * 
 * Developers can directly replace the local fallbacks in this file to set their credentials.
 */
object AppKeys {

    // 1. Supabase Url
    val SUPABASE_URL: String = run {
        val buildValue = BuildConfig.SUPABASE_URL
        if (buildValue.isNotBlank() && !buildValue.startsWith("YOUR_") && !buildValue.startsWith("MY_")) {
            buildValue
        } else {
            "https://eqezychhyqdkyhhcpwts.supabase.co"
        }
    }

    // 2. Supabase Anon (or Public) API Key
    // Paste your real Supabase Anon key here
    val SUPABASE_ANON_KEY: String = run {
        val buildValue = BuildConfig.SUPABASE_ANON_KEY
        if (buildValue.isNotBlank() && !buildValue.startsWith("YOUR_") && !buildValue.startsWith("MY_")) {
            buildValue
        } else {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVxZXp5Y2hoeXFka3loaGNwd3RzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODExOTY1NjAsImV4cCI6MjA5Njc3MjU2MH0.M2mrbrifuNKRJt4Gb9Re38i917BuOf5hPCPJSk00QJc"
        }
    }

    // 3. Google Maps API Key
    val GOOGLE_MAPS_API_KEY: String = run {
        val buildValue = BuildConfig.GOOGLE_MAPS_API_KEY
        if (buildValue.isNotBlank() && !buildValue.startsWith("YOUR_") && !buildValue.startsWith("MY_") && buildValue != "MOCK_KEY") {
            buildValue
        } else {
            "AIzaSyBY3U-Nb82z-W3qBE04U39ISyrNOiUn5sg"
        }
    }

    // 4. Gemini AI API Key
    val GEMINI_API_KEY: String = run {
        val buildValue = BuildConfig.GEMINI_API_KEY
        if (buildValue.isNotBlank() && !buildValue.startsWith("YOUR_") && !buildValue.startsWith("MY_")) {
            buildValue
        } else {
            "YOUR_GEMINI_API_KEY"
        }
    }

    // 5. Backblaze B2 (Storage) credentials (if needed)
    val B2_KEY_ID: String = if (BuildConfig.B2_KEY_ID.isNotBlank() && !BuildConfig.B2_KEY_ID.startsWith("YOUR_")) BuildConfig.B2_KEY_ID else "005ac4e31759dc00000000001"
    val B2_APPLICATION_KEY: String = if (BuildConfig.B2_APPLICATION_KEY.isNotBlank() && !BuildConfig.B2_APPLICATION_KEY.startsWith("YOUR_")) BuildConfig.B2_APPLICATION_KEY else "K0050J4RpqrxQTElgJnGLyTG8dNUuIE"
    val B2_BUCKET_ID: String = if (BuildConfig.B2_BUCKET_ID.isNotBlank() && !BuildConfig.B2_BUCKET_ID.startsWith("YOUR_")) BuildConfig.B2_BUCKET_ID else "8adca4ae2311b78599ed0c10"
    val B2_BUCKET_NAME: String = if (BuildConfig.B2_BUCKET_NAME.isNotBlank() && !BuildConfig.B2_BUCKET_NAME.startsWith("YOUR_")) BuildConfig.B2_BUCKET_NAME else "profile-photos12"
    val B2_ENDPOINT: String = if (BuildConfig.B2_ENDPOINT.isNotBlank() && !BuildConfig.B2_ENDPOINT.startsWith("YOUR_")) BuildConfig.B2_ENDPOINT else "https://s3.us-west-004.backblazeb2.com"
}
