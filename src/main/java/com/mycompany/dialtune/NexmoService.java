package com.mycompany.dialtune;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class NexmoService {
	
	@Value("${nexmo.api.key}")
    private String nexmoApiKey;

    @Value("${nexmo.api.secret}")
    private String nexmoApiSecret;
    
    @Value("${nexmo.phone.number}")
    private String nexmoNumber;
    
    @Autowired
    RestTemplate restTemplate;
	
	private static final String NEXMO_NUMBER_API_URL = "https://api.nexmo.com/number/lookup/json?api_key={api_key}&api_secret={api_secret}&number={number}";
	private static final String NEXMO_SMS_API_URL = "https://rest.nexmo.com/sms/json?api_key={api_key}&api_secret={api_secret}";
	private static final String THANK_YOU_MESSAGE = "Thanks for voting";
	
	public String getCountryCode(String phoneNubmer) {
		String country = "";
		ResponseEntity<String> response = restTemplate.getForEntity(NEXMO_NUMBER_API_URL, String.class, nexmoApiKey, nexmoApiSecret, phoneNubmer);
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			country = (String)((JSONArray)JsonPath.read(response.getBody(), "$..current_carrier.country")).get(0);
		}
		return country;
	}
	
	public void sendSMS(String phoneNumber) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		
		JSONObject request = new JSONObject();
		request.put("from", nexmoNumber);
		request.put("to", phoneNumber);
		request.put("text", THANK_YOU_MESSAGE);
		
		HttpEntity<String> entity = new HttpEntity<String>(request.toJSONString(), headers);

		restTemplate.exchange(NEXMO_SMS_API_URL, HttpMethod.POST, entity, String.class, nexmoApiKey, nexmoApiSecret);
		
	}
}
