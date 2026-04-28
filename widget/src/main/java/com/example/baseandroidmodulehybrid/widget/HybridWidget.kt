package com.example.baseandroidmodulehybrid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.baseandroidmodulehybrid.core.model.WidgetDataDao
import com.example.baseandroidmodulehybrid.core.model.WidgetDataEntity
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * HybridWidget — Android Glance AppWidget.
 */
class HybridWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetDao(): WidgetDataDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val dao = entryPoint.widgetDao()

        provideContent {
            val data by dao.observe().collectAsState(initial = null)
            WidgetContent(data)
        }
    }

    @Composable
    private fun WidgetContent(data: WidgetDataEntity?) {
        val title = data?.title ?: "Немає даних"
        val subtitle = data?.subtitle ?: "Синхронізуйте з додатку"

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Text(text = title)
            Text(text = subtitle)
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            HybridWidget().updateAll(context)
        }
    }
}

class HybridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HybridWidget()
}
