package com.thewizrd.simpleweather.wearable.complications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class BaseWeatherComplicationService : SuspendingComplicationDataSourceService() {
    companion object {
        private const val TAG = "BaseWeatherComplicationService"
    }

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected abstract val supportedComplicationTypes: Set<ComplicationType>

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)

        // Enqueue work if not already
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)

        // Request complication update
        WeatherComplicationHelper.requestComplicationUpdate(
            this,
            this::class.java,
            complicationInstanceId
        )
    }

    abstract override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData

    abstract override fun getPreviewData(type: ComplicationType): ComplicationData?

    protected fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }
}