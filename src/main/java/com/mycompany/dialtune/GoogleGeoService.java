package com.mycompany.dialtune;

import net.minidev.json.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class GoogleGeoService {
	
	@Value("${google.api.key}")
	String apiKey;
	
	@Autowired
	RestTemplate restTemplate;
	private static final String GEOCODE_API_URL = "https://maps.googleapis.com/maps/api/geocode/json?address={country_code}&key={api_key}&region={country_code}";
	
	public String[] getLatLng(String isoCountryCode) {
		String[] latLng = new String[2];
		ResponseEntity<String> response = restTemplate.getForEntity(GEOCODE_API_URL, String.class, isoCountryCode, apiKey, isoCountryCode);
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			latLng[0] = (String)((JSONArray)JsonPath.read(response.getBody(), "$..location.lat")).get(0).toString();
			latLng[1] = (String)((JSONArray)JsonPath.read(response.getBody(), "$..location.lng")).get(0).toString();
		}
		return latLng;
	}
}
