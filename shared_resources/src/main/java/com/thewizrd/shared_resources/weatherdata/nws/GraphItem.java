package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GraphItem {

    @SerializedName("expires")
    private String expires;

    @SerializedName("@type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("effective")
    private String effective;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("affectedZones")
    private List<String> affectedZones;

    @SerializedName("messageType")
    private String messageType;

    @SerializedName("urgency")
    private String urgency;

    @SerializedName("areaDesc")
    private String areaDesc;

    @SerializedName("@id")
    private String atId;

    @SerializedName("id")
    private String id;

    @SerializedName("event")
    private String event;

    @SerializedName("headline")
    private String headline;

    @SerializedName("severity")
    private String severity;

    @SerializedName("certainty")
    private String certainty;

    @SerializedName("onset")
    private String onset;

    @SerializedName("sent")
    private String sent;

    @SerializedName("sender")
    private String sender;

    @SerializedName("instruction")
    private String instruction;

    @SerializedName("response")
    private String response;

    @SerializedName("geocode")
    private Geocode geocode;

    @SerializedName("geometry")
    private Object geometry;

    @SerializedName("category")
    private String category;

    @SerializedName("parameters")
    private Parameters parameters;

    @SerializedName("status")
    private String status;

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getExpires() {
        return expires;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setEffective(String effective) {
        this.effective = effective;
    }

    public String getEffective() {
        return effective;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setAffectedZones(List<String> affectedZones) {
        this.affectedZones = affectedZones;
    }

    public List<String> getAffectedZones() {
        return affectedZones;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setAreaDesc(String areaDesc) {
        this.areaDesc = areaDesc;
    }

    public String getAreaDesc() {
        return areaDesc;
    }

    public void setAtId(String atId) {
        this.atId = atId;
    }

    public String getAtId() {
        return atId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getHeadline() {
        return headline;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setCertainty(String certainty) {
        this.certainty = certainty;
    }

    public String getCertainty() {
        return certainty;
    }

    public void setOnset(String onset) {
        this.onset = onset;
    }

    public String getOnset() {
        return onset;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getSent() {
        return sent;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setGeocode(Geocode geocode) {
        this.geocode = geocode;
    }

    public Geocode getGeocode() {
        return geocode;
    }

    public void setGeometry(Object geometry) {
        this.geometry = geometry;
    }

    public Object getGeometry() {
        return geometry;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}