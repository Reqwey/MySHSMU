package xyz.reqwey.myshsmu.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CurriculumWidgetReceiver : GlanceAppWidgetReceiver() {
	override val glanceAppWidget: GlanceAppWidget = CurriculumWidget()
}
