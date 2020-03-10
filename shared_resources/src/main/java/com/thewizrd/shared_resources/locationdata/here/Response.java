package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Response {

    @SerializedName("metaInfo")
    private MetaInfo metaInfo;

    @SerializedName("view")
    private List<ViewItem> view;

    public void setMetaInfo(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setView(List<ViewItem> view) {
        this.view = view;
    }

    public List<ViewItem> getView() {
        return view;
    }
}