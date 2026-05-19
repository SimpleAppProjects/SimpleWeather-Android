package com.thewizrd.simpleweather.adapters

import android.view.Gravity
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as materialRes
import com.google.android.material.button.MaterialButton
import com.thewizrd.simpleweather.R

class ButtonAdapter(
    @StringRes val resId: Int,
    val width: Int = WRAP_CONTENT,
    val gravity: Int = Gravity.CENTER_VERTICAL or Gravity.END,
    @Px val padding: Int = 0,
    val onClickListener: OnClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                addView(
                    MaterialButton(
                        parent.context,
                        null,
                        materialRes.attr.materialButtonOutlinedStyle
                    ).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(this@ButtonAdapter.width, WRAP_CONTENT).apply {
                                gravity = this@ButtonAdapter.gravity
                                updateMargins(
                                    left = padding,
                                    top = padding,
                                    right = padding,
                                    bottom = padding
                                )
                            }
                        setText(resId)
                        setOnClickListener(onClickListener)
                    }
                )
            }
        ) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // no-op
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun getItemViewType(position: Int): Int {
        return resId
    }
}