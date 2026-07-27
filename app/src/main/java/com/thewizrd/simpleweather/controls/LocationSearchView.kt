package com.thewizrd.simpleweather.controls

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.thewizrd.common.helpers.SimpleGestureListener
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.google.android.material.R
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter

@SuppressLint("ClickableViewAccessibility")
class LocationSearchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CustomSearchView(context, attrs) {
    private val locationAdapter = LocationQueryAdapter()
    private val footerAdapter = LocationQueryFooterAdapter()
    private val adapter = ConcatAdapter(locationAdapter)

    val recyclerView = RecyclerView(context, attrs).apply {
        layoutParams = generateDefaultLayoutParams()
        clipToPadding = false
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        adapter = this@LocationSearchView.adapter
    }

    private val searchProgressBar: LinearProgressIndicator

    private val gestureListener = object : SimpleGestureListener() {
        private var mY = 0
        private var shouldCloseKeyboard = false

        override fun onDown(e: MotionEvent?): Boolean {
            e?.run {
                mY = y.toInt()
            }
            return super.onDown(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            hide()
            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            e2?.run {
                val newY = y.toInt()
                val dY = mY - newY
                mY = newY
                // Set flag to hide the keyboard if we're scrolling down
                // So we can see what's behind the keyboard
                shouldCloseKeyboard = dY > 0
            }

            if (shouldCloseKeyboard) {
                clearFocusAndHideKeyboard()
                shouldCloseKeyboard = false
            }

            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e2?.run {
                val newY = y.toInt()
                val dY = mY - newY
                mY = newY
                // Set flag to hide the keyboard if we're scrolling down
                // So we can see what's behind the keyboard
                shouldCloseKeyboard = dY > 0
            }

            if (shouldCloseKeyboard) {
                clearFocusAndHideKeyboard()
                shouldCloseKeyboard = false
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    var onItemClickListener: ListAdapterOnClickInterface<LocationQuery>? = null
        set(value) {
            field = value
            locationAdapter.setOnClickListener(value)
        }

    init {
        recyclerView.setOnTouchListener { _, event ->
            runCatching {
                gestureDetector.onTouchEvent(event)
            }.getOrElse { false }
        }
        addView(recyclerView)

        // Add progress bar
        searchProgressBar = LinearProgressIndicator(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
            isIndeterminate = false
            trackThickness = context.dpToPx(1f).toInt()
        }
        findViewById<FrameLayout>(R.id.open_search_view_toolbar_container)?.addView(
            searchProgressBar
        )
    }

    fun showLoading(show: Boolean) {
        searchProgressBar.isIndeterminate = show
        recyclerView.isEnabled = !show
    }

    fun submitList(list: List<LocationQuery>) {
        locationAdapter.submitList(list)

        if (list.isNotEmpty()) {
            adapter.addAdapter(footerAdapter)
        } else {
            adapter.removeAdapter(footerAdapter)
        }
    }
}