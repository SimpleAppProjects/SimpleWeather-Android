package com.thewizrd.simpleweather.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.SwipeDismissFrameLayout
import com.thewizrd.common.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.WearChipButton
import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding
import com.thewizrd.simpleweather.databinding.LayoutWearDialogBinding
import kotlin.math.roundToInt

public class WearDialogFragment private constructor(private val params: WearDialogParams) :
    DialogFragment(), WearDialogInterface {
    companion object {
        fun newInstance(params: WearDialogParams): WearDialogFragment {
            return WearDialogFragment(params).apply {
                setStyle(STYLE_NO_FRAME, R.style.WearDialogFragmentTheme)
            }
        }

        fun show(fragmentManager: FragmentManager, params: WearDialogParams, tag: String? = null) {
            val f = newInstance(params)
            f.show(fragmentManager, tag)
        }
    }

    private lateinit var binding: LayoutWearDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val swipeLayoutBinding = ActivitySettingsBinding.inflate(inflater, container, false)
        swipeLayoutBinding.swipeLayout.isSwipeable = true
        swipeLayoutBinding.swipeLayout.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout?) {
                dismissAllowingStateLoss()
            }
        })

        binding = LayoutWearDialogBinding.inflate(inflater, swipeLayoutBinding.root, true)

        binding.scrollView.setOnGenericMotionListener(View.OnGenericMotionListener { v, event ->
            if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
                // Don't forget the negation here
                val delta = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL) *
                        ViewConfigurationCompat.getScaledVerticalScrollFactor(
                            ViewConfiguration.get(v.context), v.context
                        )

                // Swap these axes if you want to do horizontal scrolling instead
                v.scrollBy(0, delta.roundToInt())

                return@OnGenericMotionListener true
            }
            false
        })
        binding.scrollView.requestFocus()

        return swipeLayoutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createDialogView(params)
    }

    protected fun createDialogView(params: WearDialogParams) {
        binding.title.text = params.mTitle
        binding.icon.setImageDrawable(params.mIcon)
        binding.message.text = params.mMessage

        binding.icon.visibility = if (params.mIcon != null) View.VISIBLE else View.GONE
        binding.message.visibility = if (params.mMessage != null) View.VISIBLE else View.GONE

        binding.buttonPositive.setOnClickListener {
            if (params.mOnPositiveButtonClicked != null) {
                params.mOnPositiveButtonClicked!!.onClick(this, WearDialogInterface.BUTTON_POSITIVE)
            } else {
                dismiss()
            }
        }
        binding.buttonNegative.setOnClickListener {
            if (params.mOnNegativeButtonClicked != null) {
                params.mOnNegativeButtonClicked!!.onClick(this, WearDialogInterface.BUTTON_NEGATIVE)
            } else {
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
            layoutInflater.inflate(params.mContentViewLayoutResId, binding.content, true)
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
                    params.mOnClickListener?.onClick(this@WearDialogFragment, position)
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
                        this@WearDialogFragment,
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

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun dismiss() {
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss()
    }
}