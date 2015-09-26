package com.mycompany.dialtune;

import net.minidev.json.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class NexmoService {
	
	@Value("${nexmo.api.key}")
    private String nexmoApiKey;

    @Value("${nexmo.api.secret}")
    private String nexmoApiSecret;
    
    @Autowired
    RestTemplate restTemplate;
	
	private static final String NEXMO_NUMBER_API_URL = "https://api.nexmo.com/number/lookup/json?api_key={api_key}&api_secret={api_secret}&number={number}";
	
	
	public String getCountryCode(String phoneNubmer) {
		String country = "";
		ResponseEntity<String> response = restTemplate.getForEntity(NEXMO_NUMBER_API_URL, String.class, nexmoApiKey, nexmoApiSecret, phoneNubmer);
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			country = (String)((JSONArray)JsonPath.read(response.getBody(), "$..current_carrier.country")).get(0);
		}
		return country;
	}
}
