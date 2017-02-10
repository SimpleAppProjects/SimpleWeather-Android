package com.thewizrd.simpleweather;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.weather.weatherunderground.AutoCompleteQuery;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Setup extends AppCompatActivity {

    private ImageView mainLogo;
    private SearchView searchView;
    private Spinner apiSpinner;
    private EditText keyEntry;
    private ImageButton locationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mainLogo = (ImageView) findViewById(R.id.main_logo);
        searchView = (SearchView) findViewById(R.id.search_view);
        apiSpinner = (Spinner) findViewById(R.id.api_spinner);
        keyEntry = (EditText) findViewById(R.id.key_entry);
        locationButton = (ImageButton) findViewById(R.id.ibtn_get_location);

        init();
    }

    private void init() {
        // Expand searchbox on start
        searchView.setIconifiedByDefault(false);
        searchView.onActionViewExpanded();

        /* Event Listeners */
        apiSpinner.setOnItemSelectedListener(spinner_ItemSelected);
        searchView.setOnQueryTextListener(searchView_OnQueryTextListener);
        locationButton.setOnClickListener(locationButton_onClick);

        // Set WUnderground as default API
        apiSpinner.setSelection(0);

        // Load API key
        if (Settings.getAPIKEY() != null)
        {
            keyEntry.setText(Settings.getAPIKEY());
        }
    }

    private AdapterView.OnItemSelectedListener spinner_ItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position == 0) { // WeatherUnderground
                keyEntry.setVisibility(View.VISIBLE);
            } else {
                keyEntry.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            return;
        }
    };

    private SearchView.OnQueryTextListener searchView_OnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (!newText.equals("") && newText != null)
            {
                List<AC_Location> results = new ArrayList<>();

                try {
                    results = new AutoCompleteQuery().execute(newText).get();
                    if (results.size() > 1)
                        Toast.makeText(App.getAppContext(), "Size = " + results.size(), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(App.getAppContext(), "City = " + results.get(0).name, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    };

    private ImageButton.OnClickListener locationButton_onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
}
