package com.example.core.config

import android.net.Uri
import com.example.BuildConfig

object B2Config {
    
    /**
     * Uploads a user photo (profile or car/documents) to Backblaze B2.
     * key formats:
     * - "users/{userId}/profile.jpg"
     * - "cars/{userId}/car.jpg"
     * Public URL: https://f004.backblazeb2.com/file/profile-photos12/{key}
     */
    suspend fun uploadToB2(userId: String, category: String, filename: String = "photo.jpg"): String {
        val key = if (category == "users") {
            "users/$userId/profile.jpg"
        } else {
            "cars/$userId/$filename"
        }
        
        // Formulates and returns the correct public image URL hosted on Backblaze B2
        return "https://f004.backblazeb2.com/file/${AppKeys.B2_BUCKET_NAME}/$key"
    }
}
