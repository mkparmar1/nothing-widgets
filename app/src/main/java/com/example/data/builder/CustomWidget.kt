package com.example.data.builder

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.bitmap.SizeBucket
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class BuilderBlockType {
    TIME, DATE, BATTERY, WEATHER, STEPS, NEXT_EVENT, TEXT, PROGRESS, IMAGE, DIVIDER, SPACER
}

enum class BuilderTextColor {
    PRIMARY, MUTED, ACCENT
}

enum class BuilderAlignment {
    START, CENTER, END
}

data class BuilderBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BuilderBlockType,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 8,
    val height: Int = 4,
    val text: String = "",
    val scale: Float = 1f,
    val color: BuilderTextColor = BuilderTextColor.PRIMARY,
    val alignment: BuilderAlignment = BuilderAlignment.START,
    val imageId: String? = null
) {
    fun normalized(): BuilderBlock = copy(
        x = x.coerceIn(0, 15),
        y = y.coerceIn(0, 15),
        width = width.coerceIn(1, 16),
        height = height.coerceIn(1, 16),
        scale = scale.takeIf { it.isFinite() }?.coerceIn(0.5f, 2f) ?: 1f,
        text = text.take(120),
        imageId = imageId?.takeIf { it.matches(IMAGE_ID) }
    )

    companion object {
        private val IMAGE_ID = Regex("[A-Za-z0-9-]{8,64}")
    }
}

data class BuilderLayout(
    val blocks: List<BuilderBlock>,
    val version: Int = VERSION
) {
    fun toJson(): String = JSONObject().put("v", VERSION).put(
        "blocks",
        JSONArray().apply {
            require(blocks.isNotEmpty()) { "Add at least one block." }
            require(blocks.size <= MAX_BLOCKS) { "A widget can contain up to $MAX_BLOCKS blocks." }
            blocks.forEach { raw ->
                val block = raw.normalized()
                put(JSONObject()
                    .put("id", block.id)
                    .put("type", block.type.name)
                    .put("x", block.x)
                    .put("y", block.y)
                    .put("w", block.width)
                    .put("h", block.height)
                    .put("text", block.text)
                    .put("scale", block.scale.toDouble())
                    .put("color", block.color.name)
                    .put("align", block.alignment.name)
                    .put("image", block.imageId))
            }
        }
    ).toString()

    companion object {
        const val VERSION = 1
        const val MAX_BLOCKS = 24

        fun parse(json: String): BuilderLayout? = runCatching {
            require(json.length <= MAX_JSON_LENGTH)
            val root = JSONObject(json)
            require(root.optInt("v") == VERSION)
            val array = root.optJSONArray("blocks") ?: return null
            require(array.length() <= MAX_BLOCKS)
            val blocks = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                BuilderBlock(
                    id = item.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    type = BuilderBlockType.valueOf(item.getString("type")),
                    x = item.optInt("x"),
                    y = item.optInt("y"),
                    width = item.optInt("w", 4),
                    height = item.optInt("h", 3),
                    text = item.optString("text"),
                    scale = item.optDouble("scale", 1.0).toFloat(),
                    color = BuilderTextColor.valueOf(item.optString("color", BuilderTextColor.PRIMARY.name)),
                    alignment = BuilderAlignment.valueOf(item.optString("align", BuilderAlignment.START.name)),
                    imageId = item.optString("image").takeIf { it.isNotBlank() }
                ).normalized()
            }
            require(blocks.isNotEmpty())
            BuilderLayout(blocks)
        }.getOrNull()

        private const val MAX_JSON_LENGTH = 32_768
    }
}

@Entity(tableName = "custom_widgets")
data class CustomWidget(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val layoutJson: String,
    val sizesCsv: String = SizeBucket.entries.joinToString(",") { it.name },
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val widgetTypeId: String get() = "custom.$id"
    val supportedSizes: Set<SizeBucket>
        get() = sizesCsv.split(',').mapNotNull { value -> SizeBucket.entries.firstOrNull { it.name == value } }.toSet()
}
