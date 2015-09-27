package com.mycompany.dialtune;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

public class PollCampaignHandler {

	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	PubNubService pubNubService;
	
	@Autowired
	PollResponseHandler handler;
	
	@Value("${nexmo.phone.number}")
	String phoneNumber;
	
	private static final String VOICE2 = "src/main/resources/static/voice_2.xml";
	private static final String VOICE3 = "src/main/resources/static/voice_3.xml";
	
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
			poll.setPhoneNumber(phoneNumber);
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
	
	public void getCurrentPollVotes() {
		// get current votes for all countries
		String pollName = redisTemplate.opsForValue().get("campaign:current");
		
		Set<String> countries = redisTemplate.opsForZSet().range(pollName + ":country", 0, -1);
		
		countries.forEach( c -> {
			JSONObject jsonObject = new JSONObject();
			try{
				jsonObject.put("lat", redisTemplate.opsForHash().get(c, "lat"));
				jsonObject.put("lng", redisTemplate.opsForHash().get(c, "lng"));
				jsonObject.put("count", (Double)redisTemplate.opsForZSet().score(pollName + ":country", c));
			}
			catch(Exception e) {
				
			}
			pubNubService.publishMessage(jsonObject, "map");
			handler.handleMessage(null, null);
		});
	}
	public String sendVoiceXml() {
		// get current poll campaign
		String pollName = redisTemplate.opsForValue().get("campaign:current");
		
		// get all choices
		
		List<String> choices = redisTemplate.opsForList().range(pollName + ":choices", 0, -1);
		Map<String, String> choiceMap = new HashMap<String, String>();
		choices.forEach(c -> {
			choiceMap.put(c, (String)redisTemplate.opsForHash().get(pollName + ":" + c, "choice"));
		});
		
		String voiceXmlSource = choiceMap.size() == 2 ? VOICE2 : VOICE3;
		String source = null;
		try{
			source = getFileContents(voiceXmlSource);
			if(choiceMap.size() == 2) {
				source = source.replaceAll("/{choice_option1/}", "1");
				source = source.replaceAll("/{choice_option2/}", "2");
				
				source = source.replaceAll("/{choice_desc1/}", choiceMap.get("1"));
				source = source.replaceAll("/{choice_desc2/}", choiceMap.get("2"));
			}
			else if(choiceMap.size() == 3) {
				source = source.replaceAll("\\{choice_option1\\}", "1");
				source = source.replaceAll("\\{choice_option2\\}", "2");
				source = source.replaceAll("\\{choice_option3\\}", "3");
				
				source = source.replaceAll("\\{choice_desc1\\}", choiceMap.get("1"));
				source = source.replaceAll("\\{choice_desc2\\}", choiceMap.get("2"));
				source = source.replaceAll("\\{choice_desc3\\}", choiceMap.get("3"));	
			}
		}
		catch(Exception e) {
			
		}
		return source;
		
	}
	
	private String getFileContents(String filename) throws IOException {
		return new String(Files.readAllBytes(Paths.get(filename)));
	}
}
