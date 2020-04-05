package com.thewizrd.shared_resources.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.databinding.LocationQueryViewBinding;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;
import java.util.List;

public class LocationQueryAdapter extends RecyclerView.Adapter {
    private List<LocationQueryViewModel> mDataset;
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public List<LocationQueryViewModel> getDataset() {
        return mDataset;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        private LocationQueryViewBinding binding;

        public ViewHolder(LocationQueryViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, getAdapterPosition());
                }
            });
        }

        public void bind(LocationQueryViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationQueryAdapter(List<LocationQueryViewModel> myDataset) {
        mDataset = new ArrayList<>(myDataset);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_header, parent, false));
            default:
                // create a new view
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                LocationQueryViewBinding binding = LocationQueryViewBinding.inflate(inflater);
                binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new ViewHolder(binding);
        }
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderSetterInterface) {
            ((HeaderSetterInterface) holder).setHeader();
        } else {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            ViewHolder vh = (ViewHolder) holder;
            vh.bind(mDataset.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return 1;

        return 0;
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        if (mDataset.size() == 0)
            return 0;

        return mDataset.size() + 1;
    }

    public int getDataCount() {
        return mDataset.size();
    }

    public void setLocations(List<LocationQueryViewModel> myDataset) {
        mDataset.clear();
        mDataset.addAll(myDataset);
        notifyDataSetChanged();
    }

    public interface HeaderSetterInterface {
        void setHeader();
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        FooterViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }

        public void setHeader() {
            String creditPrefix = SimpleLibrary.getInstance().getAppContext().getString(R.string.credit_prefix);
            String locationAPI = WeatherManager.getInstance().getLocationProvider().getLocationAPI();

            if (WeatherAPI.HERE.equals(locationAPI))
                header.setText(String.format("%s HERE Maps", creditPrefix));
            else if (WeatherAPI.LOCATIONIQ.equals(locationAPI))
                header.setText(String.format("%s LocationIQ", creditPrefix));
        }
    }
}
