package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import com.example.core.config.WidgetOption
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec

/**
 * Wraps an existing widget so it offers extra [styles]. Everything is delegated to [base]; only the
 * option list gains a "Style" choice and [render] dispatches on it. With no style chosen (every widget
 * placed before styles existed) or "classic" chosen, the original renderer runs untouched.
 */
class StyledDefinition(
    val base: WidgetDefinition,
    val styles: List<WidgetStyle>
) : WidgetDefinition by base {

    val styleOption = WidgetOption.Choice(
        key = StyleCatalog.KEY,
        label = "Style",
        options = listOf(StyleCatalog.CLASSIC to "Classic") + styles.map { it.id to it.label },
        default = StyleCatalog.CLASSIC
    )

    override val options: List<WidgetOption> = listOf(styleOption) + base.options

    override fun render(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val chosen = StyleCatalog.selectedStyle(spec.options.string(StyleCatalog.KEY, StyleCatalog.CLASSIC), styles)
        if (chosen == null) base.render(context, spec, canvas) else chosen.render(context, spec, canvas)
    }
}
