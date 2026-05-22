package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DecisionRepository(private val decisionDao: DecisionDao) {

    val allDecisions: Flow<List<DecisionEntity>> = decisionDao.getAllDecisions()

    suspend fun insert(decision: DecisionEntity): Long = withContext(Dispatchers.IO) {
        decisionDao.insertDecision(decision)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        decisionDao.deleteDecisionById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        decisionDao.clearAll()
    }

    /**
     * Call Gemini to analyze the decision based on user input.
     * Extracts Options if left blank by user, assigns ratings/criteria and pros/cons.
     */
    suspend fun analyzeNewDecision(
        question: String,
        explicitOptionA: String,
        explicitOptionB: String
    ): DecisionEntity = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured in the Secrets panel.")
        }

        val formattedOptionPrompt = if (explicitOptionA.isNotEmpty() && explicitOptionB.isNotEmpty()) {
            "Explicitly evaluate Option A as '$explicitOptionA' and Option B as '$explicitOptionB'."
        } else {
            "You must deduce the two options to compare from the query and label them as Option A and Option B respectively."
        }

        val userPrompt = """
            Make a decision comparison for: "$question"
            $formattedOptionPrompt
            Analyze, assign ratings for exact 4 to 6 criteria, list comprehensive Pros and Cons for both, and output the mathematical total winner.
        """.trimIndent()

        val systemInstruction = """
            You are DecidAI, an elite decision assistant. Your goal is to analyze the user's decision topic or dilemma and help them make an choice by creating a robust comparison table, detailed pro/con lists, and an overall verdict.
            Assign scores from 1 to 10 for each criterion for Option A and Option B, and calculate the sum totals.
            The winningOption field MUST be the exact name of the option with the higher total score, or specify "Draw" if equal.

            You MUST respond ONLY with a valid, parsable JSON object exactly matching the following JSON schema:
            {
              "title": "Cleaned up title or title question of the decision",
              "optionA": "Name of Option A (use the user's explicit Option A name, or intelligent concise name if not supplied)",
              "optionB": "Name of Option B (use the user's explicit Option B name, or intelligent concise name if not supplied)",
              "criteria": [
                 {
                    "name": "Criterion Name (e.g. 'Upfront Cost', 'Security', 'Flexibility')",
                    "optionAScore": 8,
                    "optionBScore": 5,
                    "rationale": "Brief explanation of why these scores were assigned"
                 }
              ],
              "optionA_pros": ["Pro 1", "Pro 2", "Pro 3"],
              "optionA_cons": ["Con 1", "Con 2", "Con 3"],
              "optionB_pros": ["Pro 1", "Pro 2", "Pro 3"],
              "optionB_cons": ["Con 1", "Con 2", "Con 3"],
              "verdict": "Clear, practical 3-sentence summary recommendation.",
              "winningOption": "The EXACT string of whichever option had the higher total score, or 'Draw' if equal."
            }

            Do NOT add any introductory or ending text, do NOT include markdown code-block backticks, just return raw JSON content.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Received an empty or invalid response from Gemini.")

        Log.d("DecidAI_Repo", "Raw Gemini Response: $rawText")

        val cleanedJson = cleanJson(rawText)
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(DecisionDetailResponse::class.java)

        val detailResponse = adapter.fromJson(cleanedJson)
            ?: throw IllegalStateException("Could not parse AI response JSON.")

        // Make sure winningOption is populated correctly based on totals
        val sumA = detailResponse.criteria.sumOf { it.optionAScore }
        val sumB = detailResponse.criteria.sumOf { it.optionBScore }
        val finalWinnerName = when {
            sumA > sumB -> detailResponse.optionA
            sumB > sumA -> detailResponse.optionB
            else -> "Draw"
        }

        // Adjust winningOption to match actual scores to prevent mistakes by AI
        val correctedResponse = detailResponse.copy(winningOption = finalWinnerName)

        val entity = DecisionEntity.fromResponse(correctedResponse)
        // Store in local DB automatically
        val insertedId = decisionDao.insertDecision(entity)
        entity.copy(id = insertedId)
    }

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```")
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
        }
        return cleaned.trim()
    }
}
