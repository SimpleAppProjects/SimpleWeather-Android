package com.thewizrd.weather_api.here.location;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
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