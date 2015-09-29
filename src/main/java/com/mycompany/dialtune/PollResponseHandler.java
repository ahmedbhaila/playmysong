package com.mycompany.dialtune;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class PollResponseHandler {

	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	PubNubService pubNubService;
	
	@Autowired
	NexmoService nexmoService;
	
	@Autowired
	GoogleGeoService geoService;

	public void handleMessage(String from, String response) {
		
		JSONObject data = new JSONObject();
		
		// find out current campaign key
		String currentCampaign = redisTemplate.opsForValue().get(
				"campaign:current");

		// responses are numerical choices, increment number for the choice
		/*redisTemplate.opsForHash().increment(currentCampaign + ":" + response,
				"count", 1);*/
		if(response != null) {
			redisTemplate.opsForZSet().incrementScore(currentCampaign + ":count", response, 1);
		}

		List<String> choices = redisTemplate.opsForList().range(
				currentCampaign + ":choices", 0, -1);

		Double totalVotes = 0.0;
		Map<String, Double> indiVotes = new HashMap<String, Double>();
		for (String choice : choices) {
			Double voteCount = redisTemplate.opsForZSet().score(currentCampaign + ":count", choice);
			voteCount = voteCount == null ? 0 : voteCount;
			/*Double voteCount = Double.parseDouble((String) redisTemplate
					.opsForHash().get(currentCampaign + ":" + choice, "count"));*/
			totalVotes += voteCount;
			indiVotes.put(
					(String) redisTemplate.opsForHash().get(
							currentCampaign + ":" + choice, "choice"),
					voteCount);
		}
		// do pie-chart calculations here and create json object to represent
		// for charting
		
		JSONArray mainArray = new JSONArray();
		final Double fTotalVotes = totalVotes;
		if(fTotalVotes != 0.0) {
			try {

				indiVotes.forEach((k, v) -> {
					try{
						JSONArray val = new JSONArray();
						val.put(k);
						val.put(v / fTotalVotes * 100);

						mainArray.put(val);
					}
					catch(Exception e) {
						e.printStackTrace();
					}

				});
				data.put("columns", mainArray);
			} catch (JSONException e) {
				e.printStackTrace();
			}	
			pubNubService.publishMessage(data);
			data = new JSONObject();
			try{
				data.put("total_votes", totalVotes);
				pubNubService.publishMessage(data, "total_votes");
			}
			catch(Exception ex) {
				
			}
			if(from != null) {
				handleLocationData(currentCampaign, from);
			}
		}
		
	}
	
	private void handleLocationData(String currentCampaign, String from) {
		// get country ISO code for this number
		String country = nexmoService.getCountryCode(from);
		
		// check for lat,lng in datastore
		String lat = (String)redisTemplate.opsForHash().get(country, "lat");
		String lng = (String)redisTemplate.opsForHash().get(country, "lng");
	
		String[] latLng = null;
		if(lat == null) {
			// get geo location center for this country
			latLng = geoService.getLatLng(country);
			
			// store latlng in datasource
			redisTemplate.opsForHash().put(country, "lat", latLng[0]);
			redisTemplate.opsForHash().put(country, "lng", latLng[1]);
			
			lat = latLng[0];
			lng = latLng[1];
		}
		
		// increment score for this location against the current campaign
		redisTemplate.opsForZSet().incrementScore(currentCampaign + ":country", country, 1);
		
		JSONObject jsonObject = new JSONObject();
		try{
			jsonObject.put("lat", lat);
			jsonObject.put("lng", lng);
			jsonObject.put("count", (Double)redisTemplate.opsForZSet().score(currentCampaign + ":country", country));
		}
		catch(Exception e) {
			
		}
		pubNubService.publishMessage(jsonObject, "map");
		
	}
	
	
	
}
