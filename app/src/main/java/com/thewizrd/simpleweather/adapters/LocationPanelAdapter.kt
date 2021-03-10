package com.thewizrd.simpleweather.adapters

import android.content.Context
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.ObservableArrayList
import com.thewizrd.shared_resources.helpers.OnListChangedListener
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.shared_resources.weatherdata.LocationType
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.LocationPanel
import com.thewizrd.simpleweather.controls.LocationPanelViewModel
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapterInterface
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import com.google.android.material.snackbar.Snackbar as MaterialSnackbar

class LocationPanelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>,
        ItemTouchHelperAdapterInterface, LifecycleEventObserver {
    object Payload {
        const val IMAGE_UPDATE = 0
    }

    object ItemType {
        @JvmField
        val GPS_PANEL: Int = LocationType.GPS.value

        @JvmField
        val SEARCH_PANEL: Int = LocationType.SEARCH.value
        const val HEADER_GPS = -2
        const val HEADER_FAV = -3
        const val FOOTER_SPACER = -4
    }

    interface HeaderSetterInterface {
        fun setHeader()
        fun setHeaderTextColor()
    }

    interface ViewHolderLongClickListener {
        fun onLongClick(holder: RecyclerView.ViewHolder)
    }

    private val mDataset: ObservableArrayList<LocationPanelViewModel>
    private val mSelectedItems: ObservableArrayList<LocationPanelViewModel>

    private var gpsVH: GPSHeaderViewHolder? = null
    private var favVH: FavHeaderViewHolder? = null
    private var hasGPSPanel = false
    private var hasSearchPanel = false

    private var mParentRecyclerView: RecyclerView? = null
    private var mSnackMgr: SnackbarManager? = null
    private var isFragmentAlive = false
    private var isInEditMode = false

    // Event listeners
    private var onClickListener: RecyclerOnClickListenerInterface? = null
    private var onLongClickListener: RecyclerOnClickListenerInterface? = null
    private val onLongClickToDragListener: ViewHolderLongClickListener?
    private var onListChangedCallback: OnListChangedListener<LocationPanelViewModel>? = null
    private var onSelectionChangedCallback: OnListChangedListener<LocationPanelViewModel>? = null

    fun setOnClickListener(onClickListener: RecyclerOnClickListenerInterface?) {
        this.onClickListener = onClickListener
    }

    fun setOnLongClickListener(onLongClickListener: RecyclerOnClickListenerInterface?) {
        this.onLongClickListener = onLongClickListener
    }

    fun setOnListChangedCallback(onListChangedCallback: OnListChangedListener<LocationPanelViewModel>?) {
        if (this.onListChangedCallback != null) {
            mDataset.removeOnListChangedCallback(this.onListChangedCallback)
        }
        this.onListChangedCallback = onListChangedCallback
        if (onListChangedCallback != null) mDataset.addOnListChangedCallback(onListChangedCallback)
    }

    fun setOnSelectionChangedCallback(onSelectionChangedCallback: OnListChangedListener<LocationPanelViewModel>?) {
        if (this.onSelectionChangedCallback != null) {
            mSelectedItems.removeOnListChangedCallback(this.onSelectionChangedCallback)
        }
        this.onSelectionChangedCallback = onSelectionChangedCallback
        if (onSelectionChangedCallback != null) mSelectedItems.addOnListChangedCallback(onSelectionChangedCallback)
    }

    // Create copy of list
    fun getDataset(): List<LocationPanelViewModel> {
        return ArrayList(mDataset)
    }

    fun getGPSViewHolder(): RecyclerView.ViewHolder? {
        return gpsVH
    }

    fun getFavViewHolder(): RecyclerView.ViewHolder? {
        return favVH
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class LocationPanelViewHolder internal constructor(internal var mLocView: LocationPanel) : RecyclerView.ViewHolder(mLocView) {
        private lateinit var model: LocationPanelViewModel

        init {
            mLocView.showLoading(true)

            mLocView.setOnClickListener { v ->
                onClickListener?.onClick(v, adapterPosition)

                if (itemViewType == ItemType.SEARCH_PANEL && model.isEditMode) {
                    if (model.isChecked) {
                        model.isChecked = false
                        mSelectedItems.remove(model)
                    } else {
                        model.isChecked = true
                        mSelectedItems.add(model)
                    }
                    notifyItemChanged(getViewPosition(model))
                }
            }

            mLocView.setOnLongClickListener { v ->
                if (model.locationType == LocationType.SEARCH.value) {
                    onLongClickToDragListener?.onLongClick(this@LocationPanelViewHolder)

                    if (!model.isEditMode && !model.isChecked && !mSelectedItems.contains(model)) {
                        mSelectedItems.add(model)
                    }
                }
                onLongClickListener?.onClick(v, adapterPosition)
                true
            }
        }

        fun bind(model: LocationPanelViewModel) {
            this.model = model
            if (!model.isEditMode) {
                model.isChecked = false
            }
            mLocView.bindModel(model)
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    constructor(longClickListener: ViewHolderLongClickListener?) : super() {
        mDataset = ObservableArrayList<LocationPanelViewModel>(Settings.getMaxLocations())
        mSelectedItems = ObservableArrayList<LocationPanelViewModel>(Settings.getMaxLocations())
        onLongClickToDragListener = longClickListener
    }

    fun clearSelection() {
        mSelectedItems.clear()
    }

    val selectedItems: List<LocationPanelViewModel>
        get() = Collections.unmodifiableList(mSelectedItems)

    fun setInEditMode(value: Boolean) {
        isInEditMode = value
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mParentRecyclerView = recyclerView
        mSnackMgr = SnackbarManager(recyclerView)
        isFragmentAlive = true
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mSnackMgr?.dismissAll()
        mSnackMgr = null
        mParentRecyclerView = null
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event >= Lifecycle.Event.ON_PAUSE) {
            isFragmentAlive = false

            mSnackMgr?.dismissAll()
            mSnackMgr = null
        } else {
            isFragmentAlive = true
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context: Context = parent.context
        return when (viewType) {
            ItemType.HEADER_GPS -> {
                GPSHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.locations_header, parent, false)).also {
                    gpsVH = it
                }
            }
            ItemType.HEADER_FAV -> {
                FavHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.locations_header, parent, false)).also {
                    favVH = it
                }
            }
            ItemType.FOOTER_SPACER -> {
                val spacer = Space(parent.context)
                val height = context.resources.getDimensionPixelSize(R.dimen.fab_size)
                spacer.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (height * 1.5f).toInt())
                ViewHolder(spacer)
            }
            else -> {
                // create a new view
                val v = LocationPanel(context)
                LocationPanelViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        super.onBindViewHolder(holder, position, payloads)

        val imageUpdateOnly = if (payloads.isNotEmpty()) {
            payloads[0] == Payload.IMAGE_UPDATE
        } else {
            false
        }

        when (holder) {
            is HeaderSetterInterface -> {
                (holder as HeaderSetterInterface).setHeader()
                (holder as HeaderSetterInterface).setHeaderTextColor()
            }
            is ViewHolder -> {
                // No-op
            }
            else -> {
                val vHolder = holder as LocationPanelViewHolder
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                val panelView: LocationPanelViewModel? = getPanelViewModel(position)

                if (!imageUpdateOnly && panelView != null) {
                    vHolder.bind(panelView)
                }

                // Background
                vHolder.mLocView.post {
                    updatePanelBackground(vHolder, panelView, imageUpdateOnly)
                }
            }
        }
    }

    private fun updatePanelBackground(vHolder: LocationPanelViewHolder, panelView: LocationPanelViewModel?, skipCache: Boolean) {
        if (panelView?.imageData != null) {
            vHolder.mLocView.setWeatherBackground(panelView, skipCache)
        } else {
            vHolder.mLocView.clearBackground()
        }
    }

    fun hasGPSHeader(): Boolean {
        return hasGPSPanel
    }

    fun hasSearchHeader(): Boolean {
        return hasSearchPanel
    }

    @Synchronized
    override fun getItemViewType(position: Int): Int {
        if (hasGPSPanel && hasSearchPanel && position == 0)
            return ItemType.HEADER_GPS
        else if (hasGPSPanel && position == 0)
            return ItemType.HEADER_GPS
        else if (hasSearchPanel && position == 0)
            return ItemType.HEADER_FAV
        else if (hasGPSPanel && hasSearchPanel && position == 2)
            return ItemType.HEADER_FAV
        else if (position == itemCount - 1)
            return ItemType.FOOTER_SPACER

        val model: LocationPanelViewModel? = getPanelViewModel(position)
        return model?.locationType ?: 0
    }

    @Synchronized
    fun getViewPosition(item: LocationPanelViewModel?): Int {
        var position = mDataset.indexOf(item)

        if (position == 0)
            position++
        else if (hasSearchPanel && hasGPSPanel && position > 0)
            position += 2
        else if ((hasSearchPanel || hasGPSPanel) && position > 0)
            position++

        return position
    }

    @Synchronized
    fun getDataPosition(position: Int): Int {
        var position = position

        if (position == 1) {
            position--
        } else if (hasSearchPanel && hasGPSPanel && position > 1) {
            position -= 2
        } else if ((hasSearchPanel || hasGPSPanel) && position > 1) {
            position--
        }

        return position
    }

    @Synchronized
    override fun getItemCount(): Int {
        var size = mDataset.size

        if (hasGPSPanel) size++
        if (hasSearchPanel) size++

        size++ // For Footer

        return size
    }

    @Synchronized
    fun getDataCount(): Int {
        return mDataset.size
    }

    @Synchronized
    fun getFavoritesCount(): Int {
        var size = mDataset.size
        if (hasGPSPanel) size--
        return size
    }

    fun getGPSPanel(): LocationPanelViewModel? {
        if (hasGPSPanel) {
            val data = getPanelData(0)
            if (data?.locationType == LocationType.GPS) {
                return getPanelViewModel(0)
            }
        }
        return null
    }

    fun getFirstFavPanel(): LocationPanelViewModel? {
        if (hasGPSPanel && hasSearchPanel)
            return getPanelViewModel(1)
        else if (hasSearchPanel) {
            return getPanelViewModel(0)
        }
        return null
    }

    fun add(item: LocationPanelViewModel) {
        mDataset.add(item)
        notifyItemInserted(getViewPosition(item))

        if (item.locationType == LocationType.GPS.value)
            hasGPSPanel = true
        else if (!hasSearchPanel)
            hasSearchPanel = true
    }

    fun add(index: Int, item: LocationPanelViewModel) {
        if (index > mDataset.size) {
            if (BuildConfig.DEBUG) Log.i("LocationPanelAdapter", "Index OOB")
            mDataset.add(item)
        } else {
            mDataset.add(index, item)
        }

        notifyItemInserted(getViewPosition(item))

        if (!hasGPSPanel && item.locationType == LocationType.GPS.value) {
            hasGPSPanel = true
        } else if (!hasSearchPanel) {
            hasSearchPanel = true
        }
    }

    fun remove(panel: LocationPanelViewModel): Boolean {
        val viewPosition = getViewPosition(panel)

        val removed = mDataset.remove(panel)

        if (!removed) return false

        if (panel.locationType == LocationType.GPS.value)
            hasGPSPanel = false

        notifyItemRemoved(viewPosition)
        return true
    }

    fun removeGPSPanel() {
        getGPSPanel()?.let { remove(it) }
    }

    fun removeAll() {
        val size = itemCount
        mDataset.clear()
        notifyItemRangeRemoved(0, size)

        hasGPSPanel = false
        hasSearchPanel = false
    }

    @Synchronized
    fun getPanelViewModel(position: Int): LocationPanelViewModel? {
        if (position >= itemCount || position < 0 || mDataset.size == 0)
            return null

        val dataPosition = getDataPosition(position)

        return if (dataPosition >= mDataset.size) {
            null
        } else {
            mDataset[dataPosition]
        }
    }

    @Synchronized
    fun getPanelData(position: Int): LocationData? {
        if (position >= itemCount || mDataset.size == 0)
            return null

        val panel: LocationPanelViewModel = getPanelViewModel(position) ?: return null

        return panel.locationData
    }

    private fun removeLocation(panel: LocationPanelViewModel) {
        // Remove location from list
        Settings.deleteLocation(panel.locationData!!.query)

        // Remove panel
        GlobalScope.launch(Dispatchers.Main.immediate) {
            remove(panel)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        mDataset.move(getDataPosition(fromPosition), getDataPosition(toPosition))
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        AnalyticsLogger.logEvent("LocationPanelAdapter: onItemDismiss")

        val dismissedPanel: LocationPanelViewModel = getPanelViewModel(position) ?: return

        dismissedPanel.isChecked = false
        mSelectedItems.remove(dismissedPanel)

        GlobalScope.launch {
            if (mParentRecyclerView != null) {
                launch(Dispatchers.Main.immediate) {
                    PanelDeleteHandler(dismissedPanel).deletePanels()
                }
            } else {
                removeLocation(dismissedPanel)
            }
        }.invokeOnCompletion { cause ->
            if (cause == null) {
                val context: Context = App.instance.appContext
                ShortcutCreatorWorker.requestUpdateShortcuts(context)
            }
        }
    }

    class GPSHeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), HeaderSetterInterface {
        var header: TextView = itemView.findViewById(R.id.header)

        override fun setHeader() {
            header.setText(R.string.label_currentlocation)
        }

        override fun setHeaderTextColor() {
            header.setTextColor(ContextUtils.getColor(header.context, android.R.attr.textColorPrimary))
        }
    }

    class FavHeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), HeaderSetterInterface {
        var header: TextView = itemView.findViewById<TextView>(R.id.header)

        override fun setHeader() {
            header.setText(R.string.label_favoritelocations)
        }

        override fun setHeaderTextColor() {
            header.setTextColor(ContextUtils.getColor(header.context, android.R.attr.textColorPrimary))
        }
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun removeSelectedItems() {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            PanelDeleteHandler(mSelectedItems).deletePanels()
        }
    }

    private inner class PanelDeleteHandler {
        // For undo
        private val panelPairs: MutableList<Pair<Int, LocationPanelViewModel>>

        constructor(panel: LocationPanelViewModel?) {
            panelPairs = ArrayList<Pair<Int, LocationPanelViewModel>>(1)
            panelPairs.add(Pair<Int, LocationPanelViewModel>(mDataset.indexOf(panel), panel))
        }

        constructor(panels: List<LocationPanelViewModel>) {
            panelPairs = ArrayList<Pair<Int, LocationPanelViewModel>>(panels.size)

            for (panel in panels) {
                panelPairs.add(Pair<Int, LocationPanelViewModel>(mDataset.indexOf(panel), panel))
            }
        }

        private fun undoAction() {
            panelPairs.sortWith { o1, o2 -> o1.first.compareTo(o2.first) }

            for (panelPair in panelPairs) {
                if (panelPair.second != null && !mDataset.contains(panelPair.second)) {
                    panelPair.second.isEditMode = isInEditMode

                    if (panelPair.first >= mDataset.size) {
                        add(panelPair.second)
                    } else {
                        add(panelPair.first, panelPair.second)
                    }

                    // End active removal animations if we're undoing the action
                    val holder = mParentRecyclerView?.findViewHolderForAdapterPosition(getViewPosition(panelPair.second))

                    if (mParentRecyclerView?.itemAnimator != null) {
                        if (holder != null) {
                            mParentRecyclerView?.itemAnimator?.endAnimation(holder)
                        } else {
                            mParentRecyclerView?.itemAnimator?.endAnimations()
                        }
                    }
                }
            }
        }

        fun deletePanels() {
            if (panelPairs.isEmpty()) return

            GlobalScope.launch(Dispatchers.Main.immediate) {
                for (panelPair in panelPairs) {
                    panelPair.second.isEditMode = false
                    panelPair.second.isChecked = false
                    mSelectedItems.remove(panelPair.second)
                    remove(panelPair.second)
                }

                // If only a single favorite location is left, revert the deletion
                if (getFavoritesCount() <= 0) {
                    undoAction()

                    if (mParentRecyclerView != null && isFragmentAlive) {
                        if (mSnackMgr == null) {
                            mSnackMgr = SnackbarManager(mParentRecyclerView!!)
                        }
                        mSnackMgr?.show(Snackbar.make(R.string.message_needfavorite, Snackbar.Duration.SHORT), null)
                    }

                    return@launch
                }

                showUndoSnackbar()
            }
        }

        private fun showUndoSnackbar() {
            if (mParentRecyclerView != null && isFragmentAlive) {
                // Make SnackBar
                val snackbar = Snackbar.make(R.string.message_locationremoved, Snackbar.Duration.SHORT)
                snackbar.setAction(R.string.undo) { undoAction() }

                val callback = object : MaterialSnackbar.Callback() {
                    override fun onDismissed(transientBottomBar: MaterialSnackbar, event: Int) {
                        super.onDismissed(transientBottomBar, event)

                        if (event != DISMISS_EVENT_ACTION) {
                            GlobalScope.launch(Dispatchers.IO) {
                                for (panelPair in panelPairs) {
                                    if (panelPair.second == null)
                                        return@launch

                                    val key: String = panelPair.second.locationData!!.query
                                    Settings.deleteLocation(key)
                                }
                            }
                        }
                    }
                }

                if (mSnackMgr == null)
                    mSnackMgr = SnackbarManager(mParentRecyclerView!!)

                GlobalScope.launch(Dispatchers.Main.immediate) {
                    mSnackMgr?.show(snackbar, callback)
                }
            }
        }
    }
}