package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApi
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.HackerNewsApi
import com.example.data.api.TranslationResult
import com.example.data.api.RetrofitClient
import com.example.data.db.StoryDao
import com.example.data.db.StoryEntity
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StoryRepository(
    private val hackerNewsApi: HackerNewsApi,
    private val geminiApi: GeminiApi,
    private val storyDao: StoryDao
) {
    val topStories: Flow<List<StoryEntity>> = storyDao.getTopStories()

    suspend fun fetchAndRefreshStories() = withContext(Dispatchers.IO) {
        // 1. Fetch top story IDs from Hacker News
        val ids = hackerNewsApi.getTopStories()
        if (ids.isEmpty()) return@withContext

        // 2. Fetch details for first 15 stories in parallel, to find exactly 10 valid news items
        val storiesList = ids.take(15).map { id ->
            async {
                try {
                    hackerNewsApi.getItem(id)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull().filter { !it.title.isNullOrBlank() }.take(10)

        if (storiesList.isEmpty()) return@withContext

        // 3. Request Gemini to translate the titles in block
        val prompt = """
            You are an experienced technology blog translator and editor. Translate these Hacker News article titles into natural, idiomatic, high-quality Chinese (Simplified), and also provide a concise 1-sentence Chinese context/explanation of what each article is likely about based on its title and domain.

            Here are the articles to translate and summarize:
            ${storiesList.mapIndexed { index, s -> "${index + 1}. Title: ${s.title}\nURL: ${s.url ?: "N/A"}" }.joinToString("\n")}

            Respond STRICTLY with a JSON array of objects. Do not wrap in markdown or include any text other than the JSON representation itself. The array must contain exactly ${storiesList.size} objects, in the same matching order as provided. Each object must have these keys:
            - "translatedTitle": (string) The professional Chinese translation of the title. Keep it clear, engaging, and in standard Chinese terminology.
            - "synopsis": (string) A concise 1-sentence Chinese explanation/context of what this article is about (max 60 characters).

            Example format:
            [
              {
                "translatedTitle": "为什么我们用Go重写了数据库驱动",
                "synopsis": "本文将探讨由于性能瓶颈，开发团队选择用Go语言重新实现数据库驱动，并带来10倍性能提升的过程。"
              }
            ]
        """.trimIndent()

        var translationResults: List<TranslationResult>? = null
        try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    )
                )
                val response = geminiApi.generateContent(key, request)
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val jsonString = rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val listType = Types.newParameterizedType(List::class.java, TranslationResult::class.java)
                    val adapter = RetrofitClient.getMoshi().adapter<List<TranslationResult>>(listType)
                    translationResults = adapter.fromJson(jsonString)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Map HnItem with TranslationResult (or fallback) to StoryEntity list
        val entities = storiesList.mapIndexed { index, item ->
            val tr = translationResults?.getOrNull(index)
            StoryEntity(
                id = item.id,
                title = item.title ?: "",
                translatedTitle = tr?.translatedTitle ?: (item.title ?: ""),
                synopsis = tr?.synopsis ?: "译文暂时落后，可点击链接查看英文原帖观点。",
                by = item.by ?: "unknown",
                score = item.score ?: 0,
                time = item.time ?: 0L,
                url = item.url,
                fetchedAt = System.currentTimeMillis()
            )
        }

        // 5. Save to Room database
        storyDao.refreshStories(entities)
    }
}
