package com.thewizrd.shared_resources.controls;

public class ProviderEntry extends ComboBoxItem {
    private String mainURL;
    private String apiRegisterURL;

    public ProviderEntry(String display, String value, String mainURL, String apiRegisterURL) {
        super(display, value);

        this.mainURL = mainURL;
        this.apiRegisterURL = apiRegisterURL;
    }

    public String getMainURL() {
        return mainURL;
    }

    public void setMainURL(String mainURL) {
        this.mainURL = mainURL;
    }

    public String getApiRegisterURL() {
        return apiRegisterURL;
    }

    public void setApiRegisterURL(String apiRegisterURL) {
        this.apiRegisterURL = apiRegisterURL;
    }
}
