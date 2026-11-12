package com.example.data.builder

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.widgets.base.WidgetRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CustomWidgetRepository {
    private val loadMutex = Mutex()
    @Volatile private var registryLoaded = false
    fun observe(context: Context): Flow<List<CustomWidget>> =
        AppDatabase.getInstance(context).customWidgetDao().observeAll()

    suspend fun all(context: Context): List<CustomWidget> =
        AppDatabase.getInstance(context).customWidgetDao().getAll()

    suspend fun save(context: Context, widget: CustomWidget) {
        val name = widget.name.trim()
        require(name.isNotBlank()) { "Widget name must not be blank" }
        require(name.length <= 40) { "Widget name must be 40 characters or fewer" }
        require(widget.id.matches(Regex("[A-Za-z0-9-]{8,64}"))) { "Widget id is invalid" }
        require(widget.supportedSizes.isNotEmpty()) { "Choose at least one widget size." }
        val layout = BuilderLayout.parse(widget.layoutJson) ?: throw IllegalArgumentException("Widget layout is invalid")
        AppDatabase.getInstance(context).customWidgetDao().upsert(
            widget.copy(
                name = name,
                layoutJson = layout.toJson(),
                sizesCsv = com.example.core.bitmap.SizeBucket.entries.filter { it in widget.supportedSizes }.joinToString(",") { it.name },
                updatedAt = System.currentTimeMillis()
            )
        )
        loadIntoRegistry(context, force = true)
    }

    suspend fun delete(context: Context, widget: CustomWidget) {
        AppDatabase.getInstance(context).customWidgetDao().delete(widget)
        loadIntoRegistry(context, force = true)
    }

    suspend fun loadIntoRegistry(context: Context, force: Boolean = false) {
        if (registryLoaded && !force) return
        loadMutex.withLock {
            if (registryLoaded && !force) return
            WidgetRegistry.replaceCustom(all(context))
            registryLoaded = true
        }
    }
}
