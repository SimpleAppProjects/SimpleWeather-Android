package com.thewizrd.shared_resources.weatherdata.ambee;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Count {

    @SerializedName("tree_pollen")
    private int treePollen;

    @SerializedName("weed_pollen")
    private int weedPollen;

    @SerializedName("grass_pollen")
    private int grassPollen;

    public void setTreePollen(int treePollen) {
        this.treePollen = treePollen;
    }

    public int getTreePollen() {
        return treePollen;
    }

    public void setWeedPollen(int weedPollen) {
        this.weedPollen = weedPollen;
    }

    public int getWeedPollen() {
        return weedPollen;
    }

    public void setGrassPollen(int grassPollen) {
        this.grassPollen = grassPollen;
    }

    public int getGrassPollen() {
        return grassPollen;
    }
}