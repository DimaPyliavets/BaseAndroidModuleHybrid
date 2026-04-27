package com.example.baseandroidmodulehybrid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.appwidget.updateAll

/**
 * HybridWidget — Android Glance AppWidget.
 *
 * Відображає дані з Room БД.
 */
class HybridWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val title = "Завантаження..."
        val subtitle = ""

        provideContent {
            WidgetContent(title, subtitle)
        }
    }

    @Composable
    private fun WidgetContent(title: String, subtitle: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Text(text = title)

            if (subtitle.isNotBlank()) {
                Text(text = subtitle)
            }
        }
    }

    companion object {
        /**
         * Викликати для оновлення всіх екземплярів віджета.
         */
        suspend fun updateAll(context: Context) {
            HybridWidget().updateAll(context)
        }
    }
}

class HybridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HybridWidget()
}
