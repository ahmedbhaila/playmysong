package com.mycompany.dialtune;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Poll implements Serializable {
	
	@JsonProperty("name")
	String pollName;
	
	@JsonProperty("choices")
	Map<String, String> pollChoices;
	
	@JsonProperty("media_source")
	String mediaSource;
	
	public String getMediaSource() {
		return mediaSource;
	}
	public void setMediaSource(String mediaSource) {
		this.mediaSource = mediaSource;
	}
	public String getPollName() {
		return pollName;
	}
	public void setPollName(String pollName) {
		this.pollName = pollName;
	}
	public Map<String, String> getPollChoices() {
		return pollChoices;
	}
	public void setPollChoices(Map<String, String> pollChoices) {
		this.pollChoices = pollChoices;
	}
	
	
}
