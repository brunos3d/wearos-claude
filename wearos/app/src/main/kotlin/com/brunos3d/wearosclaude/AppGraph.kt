package com.brunos3d.wearosclaude

import android.content.Context
import com.brunos3d.wearosclaude.data.Settings
import com.brunos3d.wearosclaude.data.UsageApi
import com.brunos3d.wearosclaude.data.UsageRepository
import kotlinx.coroutines.runBlocking

/**
 * Tiny hand-rolled object graph.
 *
 * We deliberately avoid pulling in Hilt/Koin — every consumer here only needs
 * the same three singletons, and Tiles/Complications need to access them from
 * `onCreate` where Hilt's lifecycle hooks haven't necessarily run yet.
 */
object AppGraph {
    @Volatile private var settings: Settings? = null
    @Volatile private var api: UsageApi? = null
    @Volatile private var repository: UsageRepository? = null

    fun settings(context: Context): Settings = settings ?: synchronized(this) {
        settings ?: Settings(context.applicationContext).also { settings = it }
    }

    fun api(context: Context): UsageApi = api ?: synchronized(this) {
        api ?: UsageApi(
            baseUrlProvider = { runBlocking { settings(context).snapshot().backendUrl } },
            authTokenProvider = { runBlocking { settings(context).snapshot().authToken } },
        ).also { api = it }
    }

    fun repository(context: Context): UsageRepository = repository ?: synchronized(this) {
        repository ?: UsageRepository(api(context)).also { repository = it }
    }
}
