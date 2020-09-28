package com.thewizrd.simpleweather.updates;

import com.google.gson.annotations.SerializedName;

public class UpdateInfo {
    @SerializedName("version")
    private int versionCode;
    @SerializedName("updatePriority")
    private int updatePriority;

    public UpdateInfo() {
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public int getUpdatePriority() {
        return updatePriority;
    }

    public void setUpdatePriority(int updatePriority) {
        this.updatePriority = updatePriority;
    }
}
