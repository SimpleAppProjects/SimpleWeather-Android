package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Rootobject {

    @SerializedName("data")
    private Data data;

    //@SerializedName("warnings")
    //private List<WarningsItem> warnings;

    public void setData(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

	/*
	public void setWarnings(List<WarningsItem> warnings){
		this.warnings = warnings;
	}

	public List<WarningsItem> getWarnings(){
		return warnings;
	}
	 */
}