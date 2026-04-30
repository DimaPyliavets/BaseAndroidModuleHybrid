package com.example.baseandroidmodulehybrid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.baseandroidmodulehybrid.core.model.WidgetDataDao
import com.example.baseandroidmodulehybrid.core.model.WidgetDataEntity
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

class HybridWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SQUARE,
            HORIZONTAL_RECT,
            LARGE_RECT
        )
    )

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
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }

    @Composable
    private fun WidgetContent(data: WidgetDataEntity?) {
        val context = LocalContext.current
        val title = data?.title ?: context.getString(R.string.widget_no_data)
        val subtitle = data?.subtitle ?: context.getString(R.string.widget_sync_hint)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECT = DpSize(200.dp, 100.dp)
        private val LARGE_RECT = DpSize(200.dp, 200.dp)

        suspend fun updateAll(context: Context) {
            HybridWidget().updateAll(context)
        }
    }
}

class HybridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HybridWidget()
}
