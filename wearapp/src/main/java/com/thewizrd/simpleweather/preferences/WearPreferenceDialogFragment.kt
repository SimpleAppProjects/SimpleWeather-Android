package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference
import androidx.preference.DialogPreference.TargetFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.SwipeDismissFrameLayout
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.WearChipButton
import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding
import com.thewizrd.simpleweather.databinding.LayoutWearDialogBinding
import com.thewizrd.simpleweather.fragments.WearDialogInterface
import com.thewizrd.simpleweather.fragments.WearDialogParams
import com.thewizrd.simpleweather.helpers.SpacerItemDecoration

abstract class WearPreferenceDialogFragment : DialogFragment(), WearDialogInterface,
    WearDialogInterface.OnClickListener {
    companion object {
        @JvmStatic
        protected val ARG_KEY = "key"

        private const val SAVE_STATE_TITLE = "PreferenceDialogFragment.title"
        private const val SAVE_STATE_POSITIVE_TEXT = "PreferenceDialogFragment.positiveText"
        private const val SAVE_STATE_NEGATIVE_TEXT = "PreferenceDialogFragment.negativeText"
        private const val SAVE_STATE_MESSAGE = "PreferenceDialogFragment.message"
        private const val SAVE_STATE_LAYOUT = "PreferenceDialogFragment.layout"
        private const val SAVE_STATE_ICON = "PreferenceDialogFragment.icon"
    }

    private lateinit var binding: LayoutWearDialogBinding

    private var mPreference: DialogPreference? = null

    private var mDialogTitle: CharSequence? = null
    private var mPositiveButtonText: CharSequence? = null
    private var mNegativeButtonText: CharSequence? = null
    private var mDialogMessage: CharSequence? = null

    private var mDialogIcon: BitmapDrawable? = null

    /** Which button was clicked.  */
    private var mWhichButtonClicked = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawFragment = targetFragment
        check(rawFragment is TargetFragment) {
            "Target fragment must implement TargetFragment" +
                    " interface"
        }

        val fragment = rawFragment as TargetFragment

        val key = requireArguments().getString(ARG_KEY)
        if (savedInstanceState == null) {
            mPreference = fragment.findPreference(key!!)
            mDialogTitle = mPreference!!.dialogTitle
            mPositiveButtonText = mPreference!!.positiveButtonText
            mNegativeButtonText = mPreference!!.negativeButtonText
            mDialogMessage = mPreference!!.dialogMessage

            val icon = mPreference!!.dialogIcon
            mDialogIcon = if (icon == null || icon is BitmapDrawable) {
                icon as? BitmapDrawable
            } else {
                val bitmap = Bitmap.createBitmap(
                    icon.intrinsicWidth,
                    icon.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
                BitmapDrawable(resources, bitmap)
            }
        } else {
            mDialogTitle =
                savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
            mPositiveButtonText =
                savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT)
            mNegativeButtonText =
                savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT)
            mDialogMessage =
                savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
            val bitmap =
                savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_ICON)
            if (bitmap != null) {
                mDialogIcon = BitmapDrawable(resources, bitmap)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val swipeLayoutBinding = ActivitySettingsBinding.inflate(inflater, container, false)
        swipeLayoutBinding.swipeLayout.isSwipeable = true
        swipeLayoutBinding.swipeLayout.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout?) {
                dismiss()
            }
        })

        binding = LayoutWearDialogBinding.inflate(inflater, swipeLayoutBinding.root, true)
        return swipeLayoutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mWhichButtonClicked = WearDialogInterface.BUTTON_NEGATIVE

        val builder = WearDialogParams.Builder(requireContext())
            .setTitle(mDialogTitle)
            .setIcon(mDialogIcon)

        val contentView = onCreateDialogView(view.context)
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(mDialogMessage)
        }

        onPrepareDialogBuilder(builder)

        val params = builder.build()
        createDialogView(params)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle)
        outState.putCharSequence(
            SAVE_STATE_POSITIVE_TEXT,
            mPositiveButtonText
        )
        outState.putCharSequence(
            SAVE_STATE_NEGATIVE_TEXT,
            mNegativeButtonText
        )
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage)
        if (mDialogIcon != null) {
            outState.putParcelable(
                SAVE_STATE_ICON,
                mDialogIcon!!.bitmap
            )
        }
    }

    /**
     * Get the preference that requested this dialog. Available after [.onCreate] has
     * been called on the [PreferenceFragmentCompat] which launched this dialog.
     *
     * @return The [DialogPreference] associated with this dialog
     */
    open fun getPreference(): DialogPreference? {
        if (mPreference == null) {
            val key = requireArguments().getString(ARG_KEY)
            val fragment = targetFragment as TargetFragment?
            mPreference = fragment!!.findPreference(key!!)
        }
        return mPreference
    }

    protected fun showInputMethod(view: View?) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            ?: return
        view?.let {
            imm.showSoftInput(it, InputMethodManager.SHOW_FORCED)
        }
    }

    protected fun hideInputMethod(view: View?) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            ?: return
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * Creates the content view for the dialog (if a custom content view is required).
     * By default, it inflates the dialog layout resource if it is set.
     *
     * @return The content view for the dialog
     * @see DialogPreference.setLayoutResource
     */
    protected open fun onCreateDialogView(context: Context): View? {
        return null
    }

    /**
     * Binds views in the content view of the dialog to data.
     *
     *
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content view of the dialog, if it is custom
     */
    protected open fun onBindDialogView(view: View) {}

    protected open fun onPrepareDialogBuilder(builder: WearDialogParams.Builder) {}

    protected fun createDialogView(params: WearDialogParams) {
        binding.title.text = params.mTitle
        binding.icon.setImageDrawable(params.mIcon)
        binding.message.text = params.mMessage

        binding.buttonPositive.setOnClickListener {
            if (params.mOnPositiveButtonClicked != null) {
                params.mOnPositiveButtonClicked!!.onClick(this, WearDialogInterface.BUTTON_POSITIVE)
            } else {
                mWhichButtonClicked = WearDialogInterface.BUTTON_POSITIVE
                dismiss()
            }
        }
        binding.buttonNegative.setOnClickListener {
            if (params.mOnNegativeButtonClicked != null) {
                params.mOnNegativeButtonClicked!!.onClick(this, WearDialogInterface.BUTTON_NEGATIVE)
            } else {
                mWhichButtonClicked = WearDialogInterface.BUTTON_NEGATIVE
                dismiss()
            }
        }

        binding.buttonPositive.visibility =
            if (params.mShowPositiveButton) View.VISIBLE else View.GONE
        binding.buttonNegative.visibility =
            if (params.mShowNegativeButton) View.VISIBLE else View.GONE
        binding.spacer.visibility =
            if (params.mShowNegativeButton && params.mShowPositiveButton) View.VISIBLE else View.GONE

        binding.content.removeAllViews()
        if (params.mContentViewLayoutResId != 0) {
            LayoutInflater.from(requireContext())
                .inflate(params.mContentViewLayoutResId, binding.content, true)
            binding.content.visibility = View.VISIBLE
        } else if (params.mContentView != null) {
            binding.content.addView(params.mContentView)
            binding.content.visibility = View.VISIBLE
        } else if (params.mItems != null) {
            createListView(params)
            binding.content.visibility = View.VISIBLE
        }
    }

    private fun createListView(params: WearDialogParams) {
        val recyclerView = RecyclerView(requireContext()).apply {
            id = android.R.id.list
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setHasFixedSize(false)
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            layoutManager = LinearLayoutManager(context)
            adapter = DialogListAdapter(params)
        }

        recyclerView.addItemDecoration(
            SpacerItemDecoration(
                verticalSpace = recyclerView.context.dpToPx(4f).toInt()
            )
        )

        binding.content.addView(recyclerView)

        if (params.mCheckedItem > 0) {
            recyclerView.addOnChildAttachStateChangeListener(object :
                RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    recyclerView.removeOnChildAttachStateChangeListener(this)

                    view.viewTreeObserver.addOnPreDrawListener(object :
                        ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            view.viewTreeObserver.removeOnPreDrawListener(this)

                            val height = view.measuredHeight
                            binding.root.scrollBy(0, params.mCheckedItem * height)

                            return true
                        }
                    })
                }

                override fun onChildViewDetachedFromWindow(view: View) {}
            })
        }
    }

    private inner class DialogListAdapter(private val params: WearDialogParams) :
        RecyclerView.Adapter<DialogListAdapter.ViewHolder>() {
        var mCheckedItems = params.mCheckedItems?.toMutableList()

        inner class ViewHolder(val button: WearChipButton) : RecyclerView.ViewHolder(button)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return if (params.mIsMultiChoice) {
                ViewHolder(
                    WearChipButton(
                        parent.context,
                        defStyleAttr = 0,
                        defStyleRes = R.style.Widget_Wear_WearChipButton_Checkable
                    ).apply {
                        layoutParams = RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        updateControlType(WearChipButton.CONTROLTYPE_CHECKBOX)
                    })
            } else if (params.mIsSingleChoice) {
                ViewHolder(
                    WearChipButton(
                        parent.context,
                        defStyleAttr = 0,
                        defStyleRes = R.style.Widget_Wear_WearChipButton_Checkable
                    ).apply {
                        layoutParams = RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        updateControlType(WearChipButton.CONTROLTYPE_RADIO)
                    })
            } else {
                ViewHolder(WearChipButton(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    updateControlType(WearChipButton.CONTROLTYPE_NONE)
                })
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = params.mItems?.get(position)

            holder.button.setPrimaryText(item)

            if (params.mIsMultiChoice) {
                if (params.mCheckedItems != null) {
                    holder.button.isChecked = params.mCheckedItems!![position]
                }
            } else if (params.mIsSingleChoice) {
                holder.button.isChecked = params.mCheckedItem == position
            }

            if (params.mOnClickListener != null) {
                holder.button.setOnClickListener {
                    params.mOnClickListener?.onClick(this@WearPreferenceDialogFragment, position)
                    if (!params.mIsSingleChoice) {
                        dismiss()
                    }
                }
            } else if (params.mOnCheckboxClickListener != null) {
                holder.button.setOnClickListener {
                    if (mCheckedItems != null) {
                        mCheckedItems!![position] = holder.button.isChecked
                    }
                    params.mOnCheckboxClickListener?.onClick(
                        this@WearPreferenceDialogFragment,
                        position,
                        holder.button.isChecked
                    )
                }
            }
        }

        override fun getItemCount(): Int {
            return params.mItems?.size ?: 0
        }
    }

    override fun onClick(dialog: WearDialogInterface?, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
    }

    abstract fun onDialogClosed(positiveResult: Boolean)

    override fun dismiss() {
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        super.dismissAllowingStateLoss()
    }
}