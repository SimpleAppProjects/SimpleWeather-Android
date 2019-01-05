package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AutoCompleteQuery {

    @SerializedName("suggestions")
    private List<SuggestionsItem> suggestions;

    public void setSuggestions(List<SuggestionsItem> suggestions) {
        this.suggestions = suggestions;
    }

    public List<SuggestionsItem> getSuggestions() {
        return suggestions;
    }
}