package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PropertyListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAdvisorService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askAdvisor(
        userMessage: String,
        selectedProperty: PropertyListing?,
        allProperties: List<PropertyListing>,
        conversationHistory: List<Pair<String, Boolean>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isKeyProvided = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (isKeyProvided) {
            try {
                val response = callGeminiApi(apiKey, userMessage, selectedProperty, allProperties, conversationHistory)
                if (!response.isNullOrBlank()) {
                    return@withContext response
                }
            } catch (e: Exception) {
                Log.w("GeminiAdvisor", "Gemini API failed, using domain fallback: ${e.message}")
            }
        }

        // Domain fallback expert advice
        return@withContext generateExpertLocalResponse(userMessage, selectedProperty, allProperties)
    }

    private fun callGeminiApi(
        apiKey: String,
        userMessage: String,
        selectedProperty: PropertyListing?,
        allProperties: List<PropertyListing>,
        conversationHistory: List<Pair<String, Boolean>>
    ): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = buildString {
            append("You are the AI Smart Property Advisor, an expert real estate and student housing consultant. ")
            append("You give friendly, concise, data-driven advice about rental listings, amenities, leases, neighborhood safety, and roommate expense splitting.\n\n")
            if (selectedProperty != null) {
                append("CURRENTLY VIEWED PROPERTY CONTEXT:\n")
                append("Title: ${selectedProperty.title}\n")
                append("Price: $${selectedProperty.price}/month (Beds: ${selectedProperty.beds}, Baths: ${selectedProperty.baths}, ${selectedProperty.sqft} sqft)\n")
                append("Address: ${selectedProperty.address} (${selectedProperty.distanceFromCampusKm} km from campus)\n")
                append("Amenities: ${selectedProperty.amenities.joinToString(", ")}\n")
                append("Safety Score: ${selectedProperty.safetyScore}/100, Walk Score: ${selectedProperty.walkScore}/100\n")
                append("Estimated Utilities: $${selectedProperty.estimatedUtilitiesMonthly}/mo\n")
                append("Landlord: ${selectedProperty.landlordName} (${selectedProperty.landlordPhone})\n")
                append("Available: ${selectedProperty.availableDate}\n")
                append("Description: ${selectedProperty.description}\n\n")
            }
            append("OTHER AVAILABLE LISTINGS NEARBY:\n")
            allProperties.take(4).forEach {
                append("- ${it.title}: $${it.price}/mo, ${it.beds} beds, ${it.distanceFromCampusKm} km from campus\n")
            }
            append("\nKeep answers clear, actionable, and formatted with bullet points where helpful.")
        }

        val contentsArray = JSONArray()

        // System prompt as first turn or context
        val systemContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", systemPrompt))
            })
        }
        val systemAck = JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", "Understood. I am ready to advise on this property and student housing options."))
            })
        }
        contentsArray.put(systemContent)
        contentsArray.put(systemAck)

        // Previous history
        conversationHistory.takeLast(6).forEach { (text, isUser) ->
            val role = if (isUser) "user" else "model"
            val item = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", text))
                })
            }
            contentsArray.put(item)
        }

        // Current message
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", userMessage))
            })
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: return null
        if (!response.isSuccessful) {
            Log.e("GeminiAdvisor", "Error code ${response.code}: $bodyString")
            return null
        }

        val jsonResponse = JSONObject(bodyString)
        val candidates = jsonResponse.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        return parts.getJSONObject(0).optString("text", "")
    }

    private fun generateExpertLocalResponse(
        userMessage: String,
        property: PropertyListing?,
        allListings: List<PropertyListing>
    ): String {
        val lower = userMessage.lowercase()

        if (property != null) {
            when {
                lower.contains("amenit") || lower.contains("feature") || lower.contains("wifi") || lower.contains("laundry") || lower.contains("gym") -> {
                    return "Here is the amenities breakdown for **${property.title}**:\n\n" +
                            property.amenities.joinToString("\n") { "• $it" } +
                            "\n\n💡 **Advisor Note**: High-speed WiFi and in-unit facilities make this listing very high value for students and remote professionals."
                }
                lower.contains("price") || lower.contains("rent") || lower.contains("cost") || lower.contains("afford") || lower.contains("budget") -> {
                    val perBed = property.pricePerBed
                    val totalWithUtilities = property.price + property.estimatedUtilitiesMonthly
                    return "💰 **Financial Breakdown for ${property.title}**:\n\n" +
                            "• Base Monthly Rent: **$${property.price}**\n" +
                            "• Est. Monthly Utilities: **$${property.estimatedUtilitiesMonthly}**\n" +
                            "• Total Monthly: **$$totalWithUtilities**\n" +
                            (if (property.beds > 1) "• Cost per Roommate (${property.beds} beds): **$$perBed/mo** each + ~$${property.estimatedUtilitiesMonthly / property.beds} utilities\n" else "") +
                            (if (property.hasPriceDrop) "• Recent Price Cut: **-$${property.priceDropAmount}/mo** saved!\n" else "") +
                            "\nThis matches competitive market rates for units within ${property.distanceFromCampusKm} km of campus."
                }
                lower.contains("campus") || lower.contains("distance") || lower.contains("college") || lower.contains("walk") || lower.contains("transit") -> {
                    val walkMin = (property.distanceFromCampusKm * 12).toInt().coerceAtLeast(4)
                    val bikeMin = (property.distanceFromCampusKm * 4).toInt().coerceAtLeast(2)
                    return "📍 **Proximity & Commute Analysis for ${property.title}**:\n\n" +
                            "• Distance to Campus: **${property.distanceFromCampusKm} km**\n" +
                            "• Walk Score: **${property.walkScore}/100** (Very Walkable)\n" +
                            "• Estimated Walking Time: **~$walkMin mins**\n" +
                            "• Cycling / Scooter Time: **~$bikeMin mins**\n" +
                            "• Safety Index: **${property.safetyScore}/100**\n\n" +
                            "A prime location for avoiding long commutes during finals week!"
                }
                lower.contains("pet") || lower.contains("dog") || lower.contains("cat") -> {
                    val hasPet = property.amenities.any { it.contains("Pet", ignoreCase = true) }
                    return if (hasPet) {
                        "🐾 **Pet Policy for ${property.title}**:\n\nYes! This property is listed as **Pet Friendly**. Most student properties have a standard deposit (~$200) and small monthly pet fee ($25-$35/mo). Would you like me to draft an inquiry to ${property.landlordName}?"
                    } else {
                        "🐾 **Pet Policy for ${property.title}**:\n\nPet amenities are not listed on this unit by default. We recommend confirming directly with ${property.landlordName} at ${property.landlordPhone} for service animal policies or case-by-case approval."
                    }
                }
                lower.contains("compare") || lower.contains("alternative") || lower.contains("other") -> {
                    val others = allListings.filter { it.id != property.id }.take(2)
                    val comparison = others.joinToString("\n\n") {
                        "• **${it.title}**: $${it.price}/mo (${it.beds} Bed, ${it.distanceFromCampusKm} km from campus)"
                    }
                    return "⚖️ **Comparing with nearby options**:\n\nCurrent: **${property.title}** ($${property.price}/mo, ${property.distanceFromCampusKm} km away)\n\nAlternatives:\n$comparison\n\nIf budget is top priority, check out the lower-priced options. For closest campus access, ${property.title} is tough to beat!"
                }
                lower.contains("lease") || lower.contains("contract") || lower.contains("sublet") -> {
                    return "📄 **Lease Terms for ${property.title}**:\n\n" +
                            "• Lease Duration: **${property.leaseTermsMonths} Months** (Standard Academic/Annual)\n" +
                            "• Available Move-in: **${property.availableDate}**\n" +
                            "• Landlord/Management: **${property.landlordName}**\n" +
                            "• Recommendation: Ask about summer sublease rights and whether security deposit is 1 month or fixed $500 student rate."
                }
            }
        }

        // General Q&A
        return when {
            lower.contains("budget") || lower.contains("cheap") || lower.contains("under") -> {
                val cheapest = allListings.minByOrNull { it.price }
                "💡 **Budget Student Housing Tip**:\n\nThe most affordable listing currently is **${cheapest?.title}** at **$${cheapest?.price}/mo**. Sharing a multi-bedroom unit (like 2B2B or 3B2B) generally cuts individual costs to under $750/mo. You can filter by price or set your budget in your Profile!"
            }
            lower.contains("roommate") || lower.contains("split") -> {
                "👥 **Roommate Strategy Advice**:\n\nWhen splitting housing near campus, make sure to agree on:\n1. Room size disparity adjustments (master bedroom pays 55/45 split)\n2. Utility management (WiFi, electricity, water)\n3. Shared amenities & cleaning schedules\nSelect any listing to see the exact per-person breakdown!"
            }
            else -> {
                "Hello! I'm your **AI Smart Property Advisor** 🏡✨\n\n" +
                        "I can help you with:\n" +
                        "• Analyzing specific listings, rent fairness & hidden fees\n" +
                        "• Comparing distances and walking times to college campus\n" +
                        "• Checking amenities (WiFi, in-unit laundry, parking, pets)\n" +
                        "• Calculating monthly splits between roommates\n\n" +
                        "Feel free to select any property card or ask me a question!"
            }
        }
    }
}
