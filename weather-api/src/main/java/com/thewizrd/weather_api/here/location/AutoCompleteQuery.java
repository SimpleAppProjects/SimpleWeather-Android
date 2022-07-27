package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class AutoCompleteQuery {

    @Json(name = "suggestions")
    private List<SuggestionsItem> suggestions;

    public void setSuggestions(List<SuggestionsItem> suggestions) {
        this.suggestions = suggestions;
    }

    public List<SuggestionsItem> getSuggestions() {
        return suggestions;
    }
}