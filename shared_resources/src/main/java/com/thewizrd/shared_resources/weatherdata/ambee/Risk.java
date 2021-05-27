package com.thewizrd.shared_resources.weatherdata.ambee;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Risk {

    @SerializedName("tree_pollen")
    private String treePollen;

    @SerializedName("weed_pollen")
    private String weedPollen;

    @SerializedName("grass_pollen")
    private String grassPollen;

    public void setTreePollen(String treePollen) {
        this.treePollen = treePollen;
    }

    public String getTreePollen() {
        return treePollen;
    }

    public void setWeedPollen(String weedPollen) {
        this.weedPollen = weedPollen;
    }

    public String getWeedPollen() {
        return weedPollen;
    }

    public void setGrassPollen(String grassPollen) {
        this.grassPollen = grassPollen;
    }

    public String getGrassPollen() {
        return grassPollen;
    }
}