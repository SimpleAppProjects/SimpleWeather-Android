package com.thewizrd.simpleweather.main

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.launch
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.ObjectsCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.platform.MaterialFade
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.thewizrd.common.helpers.*
import com.thewizrd.common.utils.ActivityUtils.setLightStatusBar
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchResult
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.activities.LocationSearch
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter.ViewHolderLongClickListener
import com.thewizrd.simpleweather.controls.LocationPanelUiModel
import com.thewizrd.simpleweather.databinding.FragmentLocationsBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.helpers.*
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.viewmodels.LocationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LocationsFragment : ToolbarFragment() {
    companion object {
        private const val TAG = "LocationsFragment"
    }

    private var mEditMode = false
    private var mDataChanged = false
    private var mHomeChanged = false

    // Views
    private lateinit var binding: FragmentLocationsBinding
    private lateinit var mAdapter: LocationPanelAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var mITHCallback: ItemTouchHelperCallback

    private val locationsViewModel by viewModels<LocationsViewModel>()
    private lateinit var locationSearchLauncher: ActivityResultLauncher<Void?>

    // GPS Location
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    private val mMainHandler = Handler(Looper.getMainLooper())

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override val titleResId: Int
        get() = R.string.label_nav_locations

    override fun createSnackManager(activity: Activity): SnackbarManager {
        return SnackbarManager(rootView).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }

    // For LocationPanels
    private val onRecyclerClickListener =
        object : ListAdapterOnClickInterface<LocationPanelUiModel> {
            override fun onClick(view: View, item: LocationPanelUiModel) {
                AnalyticsLogger.logEvent("LocationsFragment: recycler click")
                val navController = binding.root.findNavController()

                if (view.isEnabled && view.tag is LocationData) {
                    runWithView {
                        val locData = view.tag as LocationData

                        val isHome = ObjectsCompat.equals(locData, settingsManager.getHomeData())

                        val args =
                            LocationsFragmentDirections.actionLocationsFragmentToWeatherNowFragment()
                                .setData(withContext(Dispatchers.Default) {
                                    JSONParser.serializer(locData)
                                })
                                .setBackground(item.imageData?.imageURI)
                                .setHome(isHome)

                        navController.safeNavigate(args)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()

        // Create your fragment here
        AnalyticsLogger.logEvent("LocationsFragment: onCreate")

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    locationsViewModel.refreshLocations()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    settingsManager.setFollowGPS(false)

                    locationsViewModel.refreshLocations()

                    context?.let {
                        showSnackbar(
                            Snackbar.make(
                                it,
                                R.string.error_location_denied,
                                Snackbar.Duration.SHORT
                            )
                        )
                    }
                }
            }
        )

        locationSearchLauncher = registerForActivityResult(LocationSearch()) { result ->
            when (result) {
                is LocationSearchResult.AlreadyExists,
                is LocationSearchResult.Success -> {
                    lifecycleScope.launch {
                        result.data?.takeIf { it.isValid }?.let {
                            settingsManager.addLocation(it)
                        }
                        locationsViewModel.refreshLocations()
                    }
                }
                is LocationSearchResult.Failed,
                null -> {
                    // no-op
                }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(mEditMode) {
            override fun handleOnBackPressed() {
                if (mEditMode) {
                    toggleEditMode()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override val scrollTargetViewId: Int
        get() = binding.recyclerView.id

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        // Inflate the layout for this fragment
        binding = FragmentLocationsBinding.inflate(inflater, root, true)
        binding.viewModel = locationsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Request focus away from RecyclerView
        root.isFocusableInTouchMode = true
        root.requestFocus()

        /*
         * Capture touch events on RecyclerView
         * Expand or collapse FAB (MaterialButton) based on scroll direction
         * Collapse FAB if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
         * Expand FAB if we're scrolling to the top (items at the top are already visible)
         */
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var scrollState = RecyclerView.SCROLL_STATE_IDLE

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                scrollState = newState
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (dy < 0) {
                        binding.fab.extend()
                    } else {
                        binding.fab.shrink()
                    }
                }
            }
        })

        toolbar.setOnMenuItemClickListener(menuItemClickListener)

        // FAB
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(sysBarInsets.left, sysBarInsets.top, sysBarInsets.right, 0)
                )
                .build()
        }
        binding.fab.setOnClickListener {
            locationSearchLauncher.launch(
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    it,
                    Constants.SHARED_ELEMENT
                )
            )
        }
        ViewCompat.setTransitionName(binding.fab, Constants.SHARED_ELEMENT)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        if (requireContext().isLargeTablet()) {
            // use a linear layout manager
            val gridLayoutManager =
                object : GridLayoutManager(requireContext(), 2, VERTICAL, false) {
                    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                        return RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
            gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (mAdapter.getItemViewType(position)) {
                        LocationPanelAdapter.ItemType.HEADER_FAV, LocationPanelAdapter.ItemType.HEADER_GPS -> gridLayoutManager.spanCount
                        else -> 1
                    }
                }
            }
            mLayoutManager = gridLayoutManager
            binding.recyclerView.addItemDecoration(object : ItemDecoration() {})
        } else {
            // use a linear layout manager
            mLayoutManager = object : LinearLayoutManager(requireContext()) {
                override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                    return RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }
        binding.recyclerView.layoutManager = mLayoutManager

        // Setup RecyclerView
        LocationPanelAdapter(object : ViewHolderLongClickListener {
            override fun onLongClick(holder: RecyclerView.ViewHolder) {
                mItemTouchHelper.startDrag(holder)
            }
        }).apply {
            setOnClickListener(onRecyclerClickListener)
            setOnLongClickListener(onRecyclerLongClickListener)
            setOnListChangedCallback(onListChangedListener)
            setOnSelectionChangedCallback(onSelectionChangedListener)

            binding.recyclerView.adapter = this
            mAdapter = this
        }
        mITHCallback = ItemTouchHelperCallback(requireContext(), mAdapter)
        mItemTouchHelper = ItemTouchHelper(mITHCallback)
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView)
        mITHCallback.addItemTouchHelperCallbackListener(object : ItemTouchCallbackListener {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) {
                mDataChanged = true
                if (mEditMode) {
                    toggleEditMode()
                } else {
                    val dataSet = mAdapter.getDataset()
                    for (view in dataSet) {
                        if (view.locationType != LocationType.GPS.value) {
                            updateFavoritesPosition(view)
                        }
                    }

                    if (!mAdapter.hasGPSHeader() && mAdapter.hasSearchHeader()) {
                        val firstFavPosition = mAdapter.getViewPosition(mAdapter.getFirstFavPanel())

                        if (viewHolder.bindingAdapterPosition == firstFavPosition || target.bindingAdapterPosition == firstFavPosition) {
                            mMainHandler.removeCallbacks(sendUpdateRunner)
                            mMainHandler.postDelayed(sendUpdateRunner, 2500)
                        }
                    }
                }
            }

            override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {}

            private val sendUpdateRunner = Runnable {
                // Home has changed send notice
                Timber.tag(TAG).d("Home changed; sending update")
                localBroadcastManager.sendBroadcast(
                    Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false)
                )
                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
            }
        })
        if (!requireContext().isLargeTablet()) {
            val swipeDecor = SwipeToDeleteOffSetItemDecoration(
                binding.recyclerView.context, 2f,
                OffsetMargin.TOP or OffsetMargin.BOTTOM
            )
            mITHCallback.addItemTouchHelperCallbackListener(swipeDecor)
            binding.recyclerView.addItemDecoration(swipeDecor)
        } else {
            binding.recyclerView.addItemDecoration(
                LocationPanelOffsetDecoration(
                    binding.recyclerView.context,
                    2f
                )
            )
        }
        binding.recyclerView.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
        }

        // Enable touch actions
        mITHCallback.isItemViewSwipeEnabled = false

        // Create options menu
        createOptionsMenu()

        // Add Adapter as Lifecycle observer
        this.lifecycle.addObserver(mAdapter)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustPanelContainer()

        viewLifecycleOwner.lifecycleScope.launch {
            locationsViewModel.locations.collectLatest {
                mAdapter.replaceAll(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationsViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationsViewModel.weatherUpdatedFlow.collect {
                if (it != null) {
                    it.updateBackground()

                    binding.recyclerView.post {
                        mAdapter.notifyItemChanged(
                            mAdapter.getViewPosition(it)
                        )
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationsViewModel.refreshLocations()
            }
        }
    }

    private fun adjustPanelContainer() {
        if (requireContext().isLargeTablet()) {
            binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                    val ctx = binding.recyclerView.context

                    val isLandscape = ctx.getOrientation() == Configuration.ORIENTATION_LANDSCAPE
                    val viewWidth = binding.recyclerView.measuredWidth
                    val minColumns = if (isLandscape) 2 else 1

                    // Minimum width for ea. card
                    val minWidth =
                        ctx.resources.getDimensionPixelSize(R.dimen.location_panel_minwidth)
                    // Available columns based on min card width
                    val availColumns =
                        if ((viewWidth / minWidth) <= 1) minColumns else (viewWidth / minWidth)

                    (binding.recyclerView.layoutManager as? GridLayoutManager)?.let {
                        it.spanCount = availColumns
                    }

                    return true
                }
            })
        }
    }

    override fun onDestroyView() {
        this.lifecycle.removeObserver(mAdapter)
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustPanelContainer()
        mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount, LocationPanelAdapter.Payload.IMAGE_UPDATE)
    }

    private fun createOptionsMenu() {
        // Inflate the menu; this adds items to the action bar if it is present.
        val menu = toolbar.menu
        menu.clear()
        toolbar.inflateMenu(R.menu.locations)

        val editMenuBtn = menu?.findItem(R.id.action_editmode)
        editMenuBtn?.isVisible = !mEditMode && mAdapter.getFavoritesCount() > 1
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent AppCompatActivity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_editmode -> {
                toggleEditMode()
                true
            }
            R.id.action_delete -> {
                mAdapter.removeSelectedItems()
                true
            }
            R.id.action_done -> {
                toggleEditMode()
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("LocationsFragment: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("LocationsFragment: onPause")

        // End edit mode
        if (mEditMode) {
            toggleEditMode()
        }

        super.onPause()
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.stringId, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.String -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.message, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.WeatherError -> {
                onWeatherError(error.exception)
            }
        }

        locationsViewModel.setErrorMessageShown(error)
    }

    private fun onWeatherError(wEx: WeatherException) {
        when (wEx.errorStatus) {
            ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                val snackbar =
                    Snackbar.make(rootView.context, wEx.message, Snackbar.Duration.LONG)
                snackbar.setAction(R.string.action_retry) {
                    locationsViewModel.refreshLocations()
                }
                showSnackbar(snackbar)
            }
            ErrorStatus.QUERYNOTFOUND -> {
                showSnackbar(
                    Snackbar.make(rootView.context, wEx.message, Snackbar.Duration.LONG)
                )
            }
            else -> {
                showSnackbar(
                    Snackbar.make(rootView.context, wEx.message, Snackbar.Duration.LONG)
                )
            }
        }
    }

    private val onListChangedListener = object : OnListChangedListener<LocationPanelUiModel>() {
        override fun onChanged(
            sender: ArrayList<LocationPanelUiModel>,
            e: ListChangedArgs<LocationPanelUiModel>
        ) {
            runWithView {
                val dataMoved =
                    e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE
                val onlyHomeIsLeft = mAdapter.getFavoritesCount() <= 1

                // Flag that data has changed
                if (mEditMode && dataMoved)
                    mDataChanged = true

                if (mEditMode && (e.newStartingIndex == 0 || e.oldStartingIndex == 0))
                    mHomeChanged = true

                // Hide FAB; Don't allow adding more locations
                if (mAdapter.getDataCount() >= settingsManager.getMaxLocations()) {
                    binding.fab.hide()
                } else {
                    binding.fab.show()
                }

                // Cancel edit Mode
                if (mEditMode && onlyHomeIsLeft) toggleEditMode()

                // Disable EditMode if only single location
                val editMenuBtn = toolbar.menu?.findItem(R.id.action_editmode)
                editMenuBtn?.isVisible = if (mEditMode) false else !onlyHomeIsLeft
            }
        }
    }
    private val onSelectionChangedListener =
        object : OnListChangedListener<LocationPanelUiModel>() {
            override fun onChanged(
                sender: ArrayList<LocationPanelUiModel>,
                args: ListChangedArgs<LocationPanelUiModel>
            ) {
                runWithView {
                    if (mEditMode) {
                        toolbar.title = if (sender.isNotEmpty()) sender.size.toString() else ""

                        val deleteBtnItem = toolbar.menu.findItem(R.id.action_delete)
                        deleteBtnItem?.isVisible = sender.isNotEmpty()
                    }
                }
            }
        }
    private val onRecyclerLongClickListener =
        object : ListAdapterOnClickInterface<LocationPanelUiModel> {
            override fun onClick(view: View, item: LocationPanelUiModel) {
                val position = mAdapter.getViewPosition(item)

                if (mAdapter.getItemViewType(position) == LocationPanelAdapter.ItemType.SEARCH_PANEL) {
                    if (!mEditMode && mAdapter.getFavoritesCount() > 1) {
                        toggleEditMode()

                        item.isChecked = true
                        mAdapter.notifyItemChanged(position)
                    }
                }
            }
        }

    private fun toggleEditMode() {
        // Toggle EditMode
        mEditMode = !mEditMode
        onBackPressedCallback.isEnabled = mEditMode
        mAdapter.setInEditMode(mEditMode)

        // Set Drag & Swipe ability
        mITHCallback.isItemViewSwipeEnabled = mEditMode

        if (mEditMode) {
            // Unregister events
            mAdapter.setOnClickListener(null)
            mAdapter.setOnLongClickListener(null)
        } else {
            // Register events
            mAdapter.setOnClickListener(onRecyclerClickListener)
            mAdapter.setOnLongClickListener(onRecyclerLongClickListener)
            mAdapter.clearSelection()
        }

        updateToolbarForEditMode(mEditMode)

        for (view in mAdapter.getDataset()) {
            view.isEditMode = mEditMode
            if (!mEditMode) view.isChecked = false
            binding.recyclerView.post {
                if (isViewAlive) {
                    mAdapter.notifyItemChanged(mAdapter.getViewPosition(view))
                }
            }
            if (view.locationType != LocationType.GPS.value && !mEditMode && (mDataChanged || mHomeChanged)) {
                updateFavoritesPosition(view)
            }
        }

        if (!mEditMode && mHomeChanged) {
            Timber.tag(TAG).d("Home changed; sending update")
            localBroadcastManager.sendBroadcast(
                Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                    .putExtra(CommonActions.EXTRA_FORCEUPDATE, false)
            )
            localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
        }

        mDataChanged = false
        mHomeChanged = false
    }

    private fun updateToolbarForEditMode(inEditMode: Boolean) {
        TransitionManager.beginDelayedTransition(appBarLayout, MaterialFade().apply {
            duration = 175
            secondaryAnimatorProvider = null
        })

        if (inEditMode) {
            val navIcon =
                ContextCompat.getDrawable(toolbar.context, R.drawable.ic_close_white_24dp)!!
                    .mutate()
            DrawableCompat.setTint(
                navIcon,
                toolbar.context.getAttrColor(R.attr.colorOnPrimary)
            )
            toolbar.navigationIcon = navIcon
            toolbar.setNavigationOnClickListener {
                toggleEditMode()
            }
            toolbar.title = if (mAdapter.selectedItems.isNotEmpty()) {
                mAdapter.selectedItems.size.toString()
            } else {
                ""
            }
            toolbar.setTitleTextAppearance(
                toolbar.context,
                R.style.TextAppearance_OpenSans_ActionModeTitleOnPrimary
            )
        } else {
            toolbar.navigationIcon = null
            toolbar.setNavigationOnClickListener(null)
            toolbar.setTitle(titleResId)
            toolbar.setTitleTextAppearance(
                toolbar.context,
                toolbar.context.getAttrResourceId(R.attr.textAppearanceHeadline6)
            )
            (activity as? WindowColorManager)?.updateWindowColors()
        }

        toolbar.menu.forEach {
            when (it.itemId) {
                R.id.action_editmode -> {
                    it.isVisible = if (inEditMode) {
                        false
                    } else {
                        mAdapter.getFavoritesCount() > 1
                    }
                }
                R.id.action_delete -> {
                    it.isVisible = if (inEditMode) {
                        mAdapter.selectedItems.isNotEmpty()
                    } else {
                        false
                    }
                    MenuItemCompat.setIconTintList(
                        it,
                        ColorStateList.valueOf(toolbar.context.getAttrColor(R.attr.colorOnPrimary))
                    )
                }
                R.id.action_done -> {
                    it.isVisible = inEditMode
                    MenuItemCompat.setIconTintList(
                        it,
                        ColorStateList.valueOf(toolbar.context.getAttrColor(R.attr.colorOnPrimary))
                    )
                }
                else -> it.isVisible = !inEditMode
            }
        }

        runAppBarAnimation(
            if (inEditMode) {
                appBarLayout.context.getAttrColor(R.attr.colorPrimary)
            } else {
                appBarLayout.context.getAttrColor(R.attr.colorSurface)
            }
        )
    }

    private fun updateFavoritesPosition(view: LocationPanelUiModel) {
        val query = view.locationData!!.query
        var dataPosition = mAdapter.getDataset().indexOf(view)
        val pos = if (mAdapter.hasGPSHeader()) --dataPosition else dataPosition
        appLib.appScope.launch(Dispatchers.Default) {
            settingsManager.moveLocation(query, pos)
        }
    }

    override fun updateWindowColors() {
        super.updateWindowColors()

        if (mEditMode) {
            activity?.let {
                val statusBarColor = it.getAttrColor(R.attr.colorPrimary)

                if (appBarLayout.background is MaterialShapeDrawable) {
                    val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
                    materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
                } else {
                    appBarLayout.setBackgroundColor(statusBarColor)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.window?.setLightStatusBar(
                        ColorsUtils.isSuperLight(statusBarColor)
                    )
                }
            }
        }
    }

    private var mAppBarAnimator: ValueAnimator? = null
    private fun runAppBarAnimation(@ColorInt colorTo: Int) {
        val colorFrom = if (appBarLayout.background is MaterialShapeDrawable) {
            val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor?.defaultColor
        } else {
            (appBarLayout.background as? ColorDrawable)?.color
        } ?: appBarLayout.context.getAttrColor(R.attr.colorSurface)
        if (colorFrom != colorTo) {
            if (mAppBarAnimator?.isRunning == true) {
                mAppBarAnimator?.cancel()
            }
            mAppBarAnimator = ValueAnimator.ofObject(ArgbEvaluatorCompat(), colorFrom, colorTo)
            mAppBarAnimator!!.addUpdateListener {
                val statusBarColor = it.animatedValue as Int
                if (appBarLayout.background is MaterialShapeDrawable) {
                    val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
                    materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
                } else {
                    appBarLayout.setBackgroundColor(statusBarColor)
                }
            }
            mAppBarAnimator!!.doOnEnd {
                updateWindowColors()
            }
            mAppBarAnimator!!.duration = 195
            mAppBarAnimator!!.startDelay = 0
            mAppBarAnimator!!.start()
        }
    }
}