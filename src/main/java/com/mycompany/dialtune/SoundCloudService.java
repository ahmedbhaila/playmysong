package com.mycompany.dialtune;

import net.minidev.json.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class SoundCloudService {
	private static final String SOUNDCLOUD_SEARCH_URL="http://api.soundcloud.com/tracks?client_id=4283396bcac311205c6de16b64e8714a&q={artist}";
	
	@Autowired
	RestTemplate restTemplate;
	
	
	public String getTrackUrl(String artist) {
		String trackUrl = "";
		ResponseEntity<String> response = restTemplate.getForEntity(SOUNDCLOUD_SEARCH_URL, String.class, artist);
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			trackUrl = (String)((JSONArray)JsonPath.read(response.getBody(), "$..permalink_url")).get(0);
		}
		return trackUrl;
	}
}
