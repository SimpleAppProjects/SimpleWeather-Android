package com.thewizrd.simpleweather.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.thewizrd.common.helpers.ObservableArrayList
import com.thewizrd.common.helpers.OnListChangedListener
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.LocationPanel
import com.thewizrd.simpleweather.controls.LocationPanelUiModel
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapter
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.*

class FavoritesPanelAdapter : LocationPanelAdapter(), ItemTouchHelperAdapter,
    LifecycleEventObserver {
    private val mDataset =
        ObservableArrayList<LocationPanelUiModel>(
            settingsManager.getMaxLocations()
        )
    private val mSelectedItems =
        ObservableArrayList<LocationPanelUiModel>(
            settingsManager.getMaxLocations()
        )

    private var headerViewHolder: HeaderViewHolder? = null

    private var mParentRecyclerView: RecyclerView? = null
    private var mSnackMgr: SnackbarManager? = null
    private var isInEditMode = false

    // Event listeners
    private var onLongClickListener: ListAdapterOnClickInterface<LocationPanelUiModel>? = null
    private var onLongClickToDragListener: ViewHolderLongClickListener? = null
    private var onListChangedCallback: OnListChangedListener<LocationPanelUiModel>? = null
    private var onSelectionChangedCallback: OnListChangedListener<LocationPanelUiModel>? = null

    private var scope = CoroutineScope(Job() + Dispatchers.Main.immediate)

    fun setOnLongClickListener(onLongClickListener: ListAdapterOnClickInterface<LocationPanelUiModel>?) {
        this.onLongClickListener = onLongClickListener
    }

    fun setOnLongClickToDragListener(onLongClickToDragListener: ViewHolderLongClickListener?) {
        this.onLongClickToDragListener = onLongClickToDragListener
    }

    fun setOnListChangedCallback(onListChangedCallback: OnListChangedListener<LocationPanelUiModel>?) {
        if (this.onListChangedCallback != null) {
            mDataset.removeOnListChangedCallback(this.onListChangedCallback)
        }
        this.onListChangedCallback = onListChangedCallback
        if (onListChangedCallback != null) mDataset.addOnListChangedCallback(onListChangedCallback)
    }

    fun setOnSelectionChangedCallback(onSelectionChangedCallback: OnListChangedListener<LocationPanelUiModel>?) {
        if (this.onSelectionChangedCallback != null) {
            mSelectedItems.removeOnListChangedCallback(this.onSelectionChangedCallback)
        }
        this.onSelectionChangedCallback = onSelectionChangedCallback
        if (onSelectionChangedCallback != null) mSelectedItems.addOnListChangedCallback(
            onSelectionChangedCallback
        )
    }

    // Create copy of list
    fun getDataset(): List<LocationPanelUiModel> {
        return mDataset.toList()
    }

    fun getHeaderViewHolder(): RecyclerView.ViewHolder? {
        return headerViewHolder
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class LocationPanelViewHolder internal constructor(private val locPanel: LocationPanel) :
        RecyclerView.ViewHolder(locPanel) {
        init {
            locPanel.showLoading(true)
        }

        fun bind(model: LocationPanelUiModel) {
            if (!model.isEditMode) {
                model.isChecked = false
            }
            locPanel.bindModel(model)

            locPanel.setOnClickListener { v ->
                onClickListener?.onClick(v, model)

                if (itemViewType == LocationPanelItemType.SEARCH_PANEL && model.isEditMode) {
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

            locPanel.setOnLongClickListener { v ->
                if (model.locationType == LocationType.SEARCH.value) {
                    onLongClickToDragListener?.onLongClick(this@LocationPanelViewHolder)

                    if (!model.isEditMode && !model.isChecked && !mSelectedItems.contains(model)) {
                        mSelectedItems.add(model)
                    }
                }
                onLongClickListener?.onClick(v, model)
                true
            }
        }
    }

    fun clearSelection() {
        mSelectedItems.clear()
    }

    val selectedItems: List<LocationPanelUiModel>
        get() = mSelectedItems

    fun setInEditMode(value: Boolean) {
        isInEditMode = value
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mParentRecyclerView = recyclerView
        mSnackMgr = SnackbarManager(recyclerView)
        refreshScope()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mSnackMgr?.dismissAll()
        mSnackMgr = null
        mParentRecyclerView = null
        scope.cancel()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event >= Lifecycle.Event.ON_PAUSE) {
            scope.cancel()

            mSnackMgr?.dismissAll()
            mSnackMgr = null
        } else if (event >= Lifecycle.Event.ON_START) {
            mParentRecyclerView?.let {
                mSnackMgr = SnackbarManager(it)
            }
            refreshScope()
        }
    }

    private fun refreshScope() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context: Context = parent.context
        return when (viewType) {
            LocationPanelItemType.HEADER_FAV -> {
                HeaderViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.locations_header, parent, false)
                ).also {
                    headerViewHolder = it
                }
            }
            else -> {
                // create a new view
                LocationPanelViewHolder(LocationPanel(context))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderSetterInterface -> {
                holder.setHeader()
                holder.setHeaderTextColor()
            }
            else -> {
                val vHolder = holder as LocationPanelViewHolder
                val panelView = getPanelUiModel(position)

                if (panelView != null) {
                    vHolder.bind(panelView)
                }
            }
        }

        super.onBindViewHolder(holder, position)
    }

    @Synchronized
    override fun getItemViewType(position: Int): Int {
        if (itemCount > 0 && position == 0)
            return LocationPanelItemType.HEADER_FAV

        return LocationPanelItemType.SEARCH_PANEL
    }

    @Synchronized
    override fun getViewPosition(item: LocationPanelUiModel?): Int {
        var position = mDataset.indexOf(item)

        if (itemCount > 0 && position >= 0)
            position++

        return position
    }

    @Synchronized
    fun getDataPosition(position: Int): Int {
        var dataPosition = position

        if (itemCount > 0) {
            dataPosition--
        }

        return dataPosition
    }

    @Synchronized
    override fun getItemCount(): Int {
        var size = mDataset.size

        if (size > 0) size++ // For Header

        return size
    }

    @Synchronized
    fun getFavoritesCount(): Int {
        return mDataset.size
    }

    fun getFirstFavPanel(): LocationPanelUiModel? {
        if (itemCount > 0) {
            return getPanelUiModel(0)
        }
        return null
    }

    fun add(item: LocationPanelUiModel) {
        mDataset.add(item)
        notifyItemInserted(getViewPosition(item))
    }

    fun add(index: Int, item: LocationPanelUiModel) {
        if (index > mDataset.size) {
            mDataset.add(item)
        } else {
            mDataset.add(index, item)
        }

        notifyItemInserted(getViewPosition(item))
    }

    fun addAll(items: Collection<LocationPanelUiModel>) {
        val currentSize = itemCount

        mDataset.addAll(items)
        notifyItemRangeInserted(currentSize, currentSize + items.size)
    }

    fun replaceAll(items: List<LocationPanelUiModel>) {
        scope.launch {
            val oldList = mDataset.toList()

            val diffCallback = LocationPanelDiffCallback(oldList, items)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            mDataset.clear()
            mDataset.addAll(items)

            diffResult.dispatchUpdatesTo(HeaderOffsetListUpdateCallback(oldList))
        }
    }

    inner class HeaderOffsetListUpdateCallback(
        private val oldList: List<LocationPanelUiModel>
    ) : ListUpdateCallback {
        private val hadHeader = oldList.isNotEmpty()

        override fun onInserted(position: Int, count: Int) {
            if (hadHeader) {
                notifyItemRangeInserted(position + 1, count) // offset position by header
            } else {
                notifyItemRangeInserted(0, count + 1) // header added too
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            if (hadHeader && oldList.size == count) {
                notifyItemRangeRemoved(0, count + 1) // header removed too
            } else if (hadHeader) {
                notifyItemRangeRemoved(position + 1, count) // offset position by header
            } else {
                notifyItemRangeRemoved(position, count)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (hadHeader) {
                notifyItemMoved(fromPosition + 1, toPosition + 1) // offset position by header
            } else {
                notifyItemMoved(fromPosition, toPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (hadHeader) {
                notifyItemRangeChanged(position + 1, count, payload) // offset position by header
            } else {
                notifyItemRangeChanged(position, count, payload)
            }
        }
    }

    fun remove(panel: LocationPanelUiModel): Boolean {
        val viewPosition = getViewPosition(panel)

        val removed = mDataset.remove(panel)

        if (!removed) return false

        notifyItemRemoved(viewPosition)
        return true
    }

    fun removeAll() {
        val size = itemCount
        mDataset.clear()
        notifyItemRangeRemoved(0, size)
    }

    @Synchronized
    override fun getPanelUiModel(position: Int): LocationPanelUiModel? {
        if (position >= itemCount || position < 0 || mDataset.isEmpty())
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

        val panel = getPanelUiModel(position) ?: return null

        return panel.locationData
    }

    private fun removeLocation(panel: LocationPanelUiModel) {
        // Remove panel
        scope.launch {
            // Remove location from list
            val query = panel.locationData!!.query
            settingsManager.deleteLocation(query)

            // Notify location removed
            localBroadcastManager.sendBroadcast(
                Intent(CommonActions.ACTION_WEATHER_LOCATIONREMOVED)
                    .putExtra(Constants.WIDGETKEY_LOCATIONQUERY, query)
            )

            remove(panel)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        mDataset.move(getDataPosition(fromPosition), getDataPosition(toPosition))
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        AnalyticsLogger.logEvent("LocationPanelAdapter: onItemDismiss")

        val dismissedPanel: LocationPanelUiModel = getPanelUiModel(position) ?: return

        dismissedPanel.isChecked = false
        mSelectedItems.remove(dismissedPanel)

        scope.launch {
            if (mParentRecyclerView != null) {
                launch(Dispatchers.Main.immediate) {
                    PanelDeleteHandler(dismissedPanel).deletePanels()
                }
            } else {
                removeLocation(dismissedPanel)
            }
        }.invokeOnCompletion { cause ->
            if (cause == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutCreatorWorker.requestUpdateShortcuts(appLib.context)
                }
            }
        }
    }

    class HeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        HeaderSetterInterface {
        private var header = itemView.findViewById<TextView>(R.id.header)

        override fun setHeader() {
            header.setText(R.string.label_favoritelocations)
        }

        override fun setHeaderTextColor() {
            header.setTextColor(header.context.getAttrColor(android.R.attr.textColorPrimary))
        }
    }

    fun removeSelectedItems() {
        scope.launch(Dispatchers.Main.immediate) {
            PanelDeleteHandler(mSelectedItems).deletePanels()
        }
    }

    private inner class PanelDeleteHandler {
        // For undo
        private val panelPairs: MutableList<Pair<Int, LocationPanelUiModel>>

        constructor(panel: LocationPanelUiModel?) {
            panelPairs = MutableList(1) {
                Pair<Int, LocationPanelUiModel>(mDataset.indexOf(panel), panel)
            }
        }

        constructor(panels: List<LocationPanelUiModel>) {
            panelPairs = MutableList(panels.size) {
                val panel = panels[it]
                Pair<Int, LocationPanelUiModel>(mDataset.indexOf(panel), panel)
            }
        }

        private val context: Context
            get() = mParentRecyclerView?.context ?: appLib.context

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
                    val holder = mParentRecyclerView?.findViewHolderForAdapterPosition(
                        getViewPosition(panelPair.second)
                    )

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

            scope.launch(Dispatchers.Main.immediate) {
                for (panelPair in panelPairs) {
                    panelPair.second.isEditMode = false
                    panelPair.second.isChecked = false
                    mSelectedItems.remove(panelPair.second)
                    remove(panelPair.second)
                }

                // If only a single favorite location is left, revert the deletion
                if (getFavoritesCount() <= 0) {
                    undoAction()

                    if (isActive) {
                        mSnackMgr?.show(
                            com.thewizrd.simpleweather.snackbar.Snackbar.make(
                                context,
                                R.string.message_needfavorite,
                                com.thewizrd.simpleweather.snackbar.Snackbar.Duration.SHORT
                            ), null
                        )
                    }

                    return@launch
                }

                showUndoSnackbar()
            }
        }

        private fun showUndoSnackbar() {
            if (mSnackMgr != null) {
                // Make SnackBar
                val snackbar = com.thewizrd.simpleweather.snackbar.Snackbar.make(
                    context,
                    R.string.message_locationremoved,
                    com.thewizrd.simpleweather.snackbar.Snackbar.Duration.SHORT
                )
                snackbar.setAction(R.string.undo) { undoAction() }

                val callback = object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)

                        if (event != DISMISS_EVENT_ACTION) {
                            scope.launch(Dispatchers.IO) {
                                for (panelPair in panelPairs) {
                                    if (panelPair.second == null)
                                        return@launch

                                    val key: String = panelPair.second.locationData!!.query
                                    settingsManager.deleteLocation(key)

                                    // Notify location removed
                                    localBroadcastManager.sendBroadcast(
                                        Intent(CommonActions.ACTION_WEATHER_LOCATIONREMOVED)
                                            .putExtra(Constants.WIDGETKEY_LOCATIONQUERY, key)
                                    )
                                }
                            }
                        }
                    }
                }

                scope.launch(Dispatchers.Main.immediate) {
                    mSnackMgr?.show(snackbar, callback)
                }
            }
        }
    }
}