package com.mycompany.dialtune;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class PollCampaignHandler {

	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	PubNubService pubNubService;
	
	public void setupPoll(Poll poll) {
		
		// remove any old references to this poll
		// 2. choices list
		// 3. choice count set
		// 4. poll meta data
		// 1. all poll choices
		
		List<String> choiceList = redisTemplate.opsForList().range(poll.getPollName() + ":choices", 0, -1);
		choiceList.forEach(c -> {
			redisTemplate.opsForHash().delete(poll.getPollName() + ":" + c, "count");
			redisTemplate.opsForHash().delete(poll.getPollName() + ":" + c, "choice");
		});
		redisTemplate.delete(poll.getPollName() + ":choices");
		redisTemplate.opsForZSet().removeRange(poll.getPollName() + ":count", 0, -1);
		redisTemplate.delete(poll.getPollName());
		redisTemplate.opsForZSet().removeRange(poll.getPollName() + ":country", 0, -1);
	
		
		Map<String, String> choices = poll.getPollChoices();
		// set hash value counters for each choice to 0
		// also setup list of choices
		choices.forEach((k,v) -> {
			redisTemplate.opsForHash().put(poll.getPollName() + ":" + k, "count", "0");
			redisTemplate.opsForHash().put(poll.getPollName() + ":" + k, "choice", v);
			redisTemplate.opsForList().leftPush(poll.getPollName() + ":choices", k);
		});
		
		
		// set media source
		redisTemplate.opsForHash().put(poll.getPollName(), "media_source", poll.getMediaSource());
		activatePoll(poll.getPollName());
	}
	
	public void activatePoll(String pollName) {
		redisTemplate.opsForValue().set("campaign:current", pollName);
	}
	
	public String getWinner(String pollName) {
		String choiceValue = "";
		try {
			String choice = redisTemplate.opsForZSet().reverseRange(pollName + ":count", 0, 1).iterator().next().toString();
			choiceValue = (String)redisTemplate.opsForHash().get(pollName + ":" + choice, "choice");
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("artist", choiceValue);
			pubNubService.publishMessage(jsonObject, "winner");
		}
		catch(Exception ex) {
			
		}
		return choiceValue;
	}
	
	public JSONObject expirePoll(String pollName) {
		JSONObject jsonObject = new JSONObject();
		try{
			
			jsonObject.put("artist", getWinner(pollName));
			jsonObject.put("media_source", redisTemplate.opsForHash().get(pollName, "media_source"));
		}
		catch(Exception e) {
			
		}
		return jsonObject;
	}
	
	public Poll getCurrentPollDetails() {
		Poll poll = new Poll();
		
		String currentPoll = (String)redisTemplate.opsForValue().get("campaign:current");
		if(currentPoll != null) {
			
			poll.setPollName(currentPoll);
			poll.setMediaSource((String)redisTemplate.opsForHash().get(currentPoll, "media_source"));
			List<String> choices = redisTemplate.opsForList().range(currentPoll + ":choices", 0, -1);
			Map<String, String> choiceMap = new HashMap<String, String>();
			choices.forEach(c -> {
				choiceMap.put(c, (String)redisTemplate.opsForHash().get(currentPoll + ":" + c, "choice"));
			});
			poll.setPollChoices(choiceMap);
		}
		return poll;
		
		
	}
}
