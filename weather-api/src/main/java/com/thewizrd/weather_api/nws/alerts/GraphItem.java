package com.thewizrd.weather_api.nws.alerts;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class GraphItem {

    @Json(name = "expires")
    private String expires;

    //@Json(name = "@type")
    //private String type;

    @Json(name = "description")
    private String description;

    @Json(name = "effective")
    private String effective;

    @Json(name = "senderName")
    private String senderName;

    @Json(name = "affectedZones")
    private List<String> affectedZones;

    //@Json(name = "messageType")
    //private String messageType;

    //@Json(name = "urgency")
    //private String urgency;

    //@Json(name = "areaDesc")
    //private String areaDesc;

    //@Json(name = "@id")
    //private String atId;

    //@Json(name = "id")
    //private String id;

    @Json(name = "event")
    private String event;

    //@Json(name = "headline")
    //private String headline;

    @Json(name = "severity")
    private String severity;

    //@Json(name = "certainty")
    //private String certainty;

    @Json(name = "onset")
    private String onset;

    @Json(name = "sent")
    private String sent;

    //@Json(name = "sender")
    //private String sender;

    @Json(name = "instruction")
    private String instruction;

    //@Json(name = "response")
    //private String response;

    //@Json(name = "geocode")
    //private Geocode geocode;

    //@Json(name = "geometry")
    //private Object geometry;

    //@Json(name = "category")
    //private String category;

    //@Json(name = "parameters")
    //private Parameters parameters;

    //@Json(name = "status")
    //private String status;

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getExpires() {
        return expires;
    }

    /*
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
     */

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

    /*
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
     */

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    /*
    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getHeadline() {
        return headline;
    }
     */

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    /*
    public void setCertainty(String certainty) {
        this.certainty = certainty;
    }

    public String getCertainty() {
        return certainty;
    }
     */

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

    /*
    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }
     */

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }

    /*
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
     */
}