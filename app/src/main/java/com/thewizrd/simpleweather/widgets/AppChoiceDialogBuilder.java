package com.thewizrd.simpleweather.widgets;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.SimpleRecyclerViewAdapterObserver;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.AppItemLayoutBinding;
import com.thewizrd.simpleweather.databinding.DialogAppchooserBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AppChoiceDialogBuilder {
    private Context context;
    private AppsListAdapter mAdapter;
    private OnAppSelectedListener onItemSelectedListener;
    private DialogAppchooserBinding binding;

    interface OnAppSelectedListener {
        void onItemSelected(@Nullable String key);
    }

    public AppChoiceDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    private View createView() {
        binding = DialogAppchooserBinding.inflate(LayoutInflater.from(context));

        // Force a minimum height
        binding.recyclerView.setMinimumHeight(context.getResources().getDisplayMetrics().heightPixels);

        // Setup RecyclerView
        mAdapter = new AppsListAdapter(new DiffUtil.ItemCallback<AppsViewModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull AppsViewModel oldItem, @NonNull AppsViewModel newItem) {
                return ObjectsCompat.equals(oldItem.packageName, newItem.packageName) &&
                        ObjectsCompat.equals(oldItem.activityName, newItem.activityName);
            }

            @Override
            public boolean areContentsTheSame(@NonNull AppsViewModel oldItem, @NonNull AppsViewModel newItem) {
                return ObjectsCompat.equals(oldItem, newItem);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        mAdapter.submitList(Collections.<AppsViewModel>emptyList());

        return binding.getRoot();
    }

    public AppChoiceDialogBuilder setOnItemSelectedListener(@Nullable OnAppSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
        return this;
    }

    public void show() {
        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.abc_activitychooserview_choose_application)
                .setCancelable(true)
                .setView(createView())
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                binding.progressBar.setVisibility(View.VISIBLE);
                updateAppsList();
            }
        });

        mAdapter.setOnClickListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {
                if (onItemSelectedListener != null) {
                    onItemSelectedListener.onItemSelected(mAdapter.getCurrentList().get(position).getKey());
                }
                dialog.dismiss();
            }
        });

        mAdapter.registerAdapterDataObserver(new SimpleRecyclerViewAdapterObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                binding.progressBar.setVisibility(View.GONE);
                mAdapter.unregisterAdapterDataObserver(this);
            }
        });

        dialog.show();
    }

    private void updateAppsList() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> infos = context.getPackageManager().getInstalledApplications(0);

                // Sort result
                Collections.sort(infos, new ApplicationInfo.DisplayNameComparator(context.getPackageManager()));

                List<AppsViewModel> appsList = new ArrayList<>();

                AppsViewModel defaultApp = new AppsViewModel();
                defaultApp.setAppLabel(context.getString(R.string.summary_default));
                appsList.add(defaultApp);

                for (ApplicationInfo info : infos) {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(info.packageName);
                    if (launchIntent == null) continue;

                    ComponentName activityCmpName = launchIntent.getComponent();

                    String label = context.getPackageManager().getApplicationLabel(info).toString();
                    Drawable drawable = null;

                    try {
                        drawable = context.getPackageManager().getActivityIcon(activityCmpName);
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    AppsViewModel app = new AppsViewModel();
                    app.setAppLabel(label);
                    app.setPackageName(info.packageName);
                    app.setActivityName(activityCmpName.getClassName());
                    app.setDrawable(drawable);

                    appsList.add(app);
                }

                mAdapter.submitList(appsList);
            }
        });
    }

    public static class AppsViewModel {
        private Drawable drawable;
        private String appLabel;
        private String packageName;
        private String activityName;

        public String getKey() {
            if (packageName != null && activityName != null) {
                return String.format(Locale.ROOT, "%s/%s", packageName, activityName);
            }

            return null;
        }

        public Drawable getDrawable() {
            return drawable;
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        public String getAppLabel() {
            return appLabel;
        }

        public void setAppLabel(String appLabel) {
            this.appLabel = appLabel;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getActivityName() {
            return activityName;
        }

        public void setActivityName(String activityName) {
            this.activityName = activityName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AppsViewModel that = (AppsViewModel) o;

            if (drawable != null ? !drawable.equals(that.drawable) : that.drawable != null)
                return false;
            if (appLabel != null ? !appLabel.equals(that.appLabel) : that.appLabel != null)
                return false;
            if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
                return false;
            return activityName != null ? activityName.equals(that.activityName) : that.activityName == null;
        }

        @Override
        public int hashCode() {
            int result = drawable != null ? drawable.hashCode() : 0;
            result = 31 * result + (appLabel != null ? appLabel.hashCode() : 0);
            result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
            result = 31 * result + (activityName != null ? activityName.hashCode() : 0);
            return result;
        }
    }

    private static class AppsListAdapter extends ListAdapter<AppsViewModel, AppsListAdapter.ViewHolder> {
        private RecyclerOnClickListenerInterface onClickListener;

        protected AppsListAdapter(@NonNull DiffUtil.ItemCallback<AppsViewModel> diffCallback) {
            super(diffCallback);
        }

        protected AppsListAdapter(@NonNull AsyncDifferConfig<AppsViewModel> config) {
            super(config);
        }

        public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
            this.onClickListener = onClickListener;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private AppItemLayoutBinding binding;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                binding = DataBindingUtil.bind(itemView);

                itemView.setClickable(true);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onClickListener != null) {
                            onClickListener.onClick(v, getAdapterPosition());
                        }
                    }
                });
            }

            public void bindModel(AppsViewModel model) {
                binding.setAppViewModel(model);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            AppItemLayoutBinding binding = AppItemLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindModel(getItem(position));
        }
    }
}
