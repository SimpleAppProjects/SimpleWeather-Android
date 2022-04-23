package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thewizrd.common.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.AppItemLayoutBinding
import com.thewizrd.simpleweather.databinding.DialogAppchooserBinding
import kotlinx.coroutines.*
import java.util.*

class AppChoiceDialogBuilder(private val context: Context) {
    private lateinit var mAdapter: AppsListAdapter
    private var onItemSelectedListener: OnAppSelectedListener? = null
    private lateinit var binding: DialogAppchooserBinding
    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)

    interface OnAppSelectedListener {
        fun onItemSelected(key: String?)
    }

    private fun createView(): View {
        binding = DialogAppchooserBinding.inflate(LayoutInflater.from(context))

        // Force a minimum height
        binding.recyclerView.minimumHeight = context.resources.displayMetrics.heightPixels

        // Setup RecyclerView
        mAdapter = AppsListAdapter(object : DiffUtil.ItemCallback<AppsViewModel>() {
            override fun areItemsTheSame(oldItem: AppsViewModel, newItem: AppsViewModel): Boolean {
                return ObjectsCompat.equals(oldItem.packageName, newItem.packageName) &&
                        ObjectsCompat.equals(oldItem.activityName, newItem.activityName)
            }

            override fun areContentsTheSame(oldItem: AppsViewModel, newItem: AppsViewModel): Boolean {
                return ObjectsCompat.equals(oldItem, newItem)
            }
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = mAdapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        mAdapter.submitList(emptyList<AppsViewModel>())

        return binding.root
    }

    fun setOnItemSelectedListener(onItemSelectedListener: OnAppSelectedListener?): AppChoiceDialogBuilder {
        this.onItemSelectedListener = onItemSelectedListener
        return this
    }

    fun show() {
        val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.abc_activitychooserview_choose_application)
                .setCancelable(true)
                .setView(createView())
                .setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }
                .create()

        dialog.setOnShowListener {
            binding.progressBar.visibility = View.VISIBLE
            updateAppsList()
        }

        dialog.setOnDismissListener {
            scope.cancel()
        }

        mAdapter.setOnClickListener(object : ListAdapterOnClickInterface<AppsViewModel> {
            override fun onClick(view: View, item: AppsViewModel) {
                onItemSelectedListener?.onItemSelected(item.key)
                dialog.dismiss()
            }
        })

        mAdapter.registerAdapterDataObserver(object : SimpleRecyclerViewAdapterObserver() {
            override fun onChanged() {
                super.onChanged()
                binding.progressBar.visibility = View.GONE
                mAdapter.unregisterAdapterDataObserver(this)
            }
        })

        dialog.show()
    }

    private fun updateAppsList() {
        scope.launch(Dispatchers.Default) {
            val infos = context.packageManager.getInstalledApplications(0)

            // Sort result
            Collections.sort(infos, ApplicationInfo.DisplayNameComparator(context.packageManager))

            val appsList: MutableList<AppsViewModel> = ArrayList(infos.size)

            val defaultApp = AppsViewModel().apply {
                appLabel = context.getString(R.string.summary_default)
            }
            appsList.add(defaultApp)

            for (info in infos) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(info.packageName)
                        ?: continue

                val activityCmpName = launchIntent.component ?: continue

                val label = context.packageManager.getApplicationLabel(info).toString()

                var drawable: Drawable? = null

                try {
                    drawable = context.packageManager.getActivityIcon(activityCmpName)
                } catch (e: PackageManager.NameNotFoundException) {
                }

                val app = AppsViewModel().apply {
                    this.appLabel = label
                    this.packageName = info.packageName
                    this.activityName = activityCmpName.className
                    this.drawable = drawable
                }

                appsList.add(app)
            }

            mAdapter.submitList(appsList)
        }
    }

    class AppsViewModel {
        var drawable: Drawable? = null
        var appLabel: String? = null
        var packageName: String? = null
        var activityName: String? = null
        val key: String?
            get() = if (packageName != null && activityName != null) {
                String.format(Locale.ROOT, "%s/%s", packageName, activityName)
            } else null

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as AppsViewModel
            if (if (drawable != null) drawable != that.drawable else that.drawable != null) return false
            if (if (appLabel != null) appLabel != that.appLabel else that.appLabel != null) return false
            if (if (packageName != null) packageName != that.packageName else that.packageName != null) return false
            return if (activityName != null) activityName == that.activityName else that.activityName == null
        }

        override fun hashCode(): Int {
            var result = if (drawable != null) drawable.hashCode() else 0
            result = 31 * result + if (appLabel != null) appLabel.hashCode() else 0
            result = 31 * result + if (packageName != null) packageName.hashCode() else 0
            result = 31 * result + if (activityName != null) activityName.hashCode() else 0
            return result
        }
    }

    private class AppsListAdapter : ListAdapter<AppsViewModel, AppsListAdapter.ViewHolder> {
        private var onClickListener: ListAdapterOnClickInterface<AppsViewModel>? = null

        constructor(diffCallback: DiffUtil.ItemCallback<AppsViewModel>) : super(diffCallback)
        protected constructor(config: AsyncDifferConfig<AppsViewModel>) : super(config)

        fun setOnClickListener(onClickListener: ListAdapterOnClickInterface<AppsViewModel>?) {
            this.onClickListener = onClickListener
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding: AppItemLayoutBinding = DataBindingUtil.bind(itemView)!!

            init {
                itemView.isClickable = true
            }

            fun bindModel(model: AppsViewModel) {
                binding.appViewModel = model
                itemView.setOnClickListener { v ->
                    onClickListener?.onClick(v, model)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = AppItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindModel(getItem(position))
        }
    }
}