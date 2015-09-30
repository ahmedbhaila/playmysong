package com.mycompany.dialtune;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
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
	
	public void deletePoll(String poll) {
		// remove any old references to this poll
		// 2. choices list
		// 3. choice count set
		// 4. poll meta data
		// 1. all poll choices
				
		List<String> choiceList = redisTemplate.opsForList().range(poll + ":choices", 0, -1);
		choiceList.forEach(c -> {
			redisTemplate.opsForHash().delete(poll + ":" + c, "count");
			redisTemplate.opsForHash().delete(poll + ":" + c, "choice");
		});
		
		redisTemplate.delete(poll + ":choices");
		redisTemplate.opsForZSet().removeRange(poll + ":count", 0, -1);
		redisTemplate.delete(poll);
		redisTemplate.opsForZSet().removeRange(poll + ":country", 0, -1);
			
	}
	
	public void setupPoll(Poll poll) {
		
		deletePoll(poll.getPollName());
	
		
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
		
		// insert poll in queue
		redisTemplate.opsForList().rightPush("polls", poll.getPollName());
		
		
		// disable this to enable poll chaining
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
	
	public JSONObject expirePoll() {
		JSONObject jsonObject = new JSONObject();
		try{
			String pollName = (String)redisTemplate.opsForValue().get("campaign:current");
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
			poll.setActive(true);
		}
		return poll;
	}
	
	public List<Poll> getPolls() {
		List<String> pollNames = redisTemplate.opsForList().range("polls", 0, -1);
		List<Poll> polls = new LinkedList<Poll>();
		
		// get any current polls
		polls.add(getCurrentPollDetails());
		pollNames.forEach(p -> {
			Poll poll = new Poll();
			poll.setPhoneNumber(phoneNumber);
			poll.setPollName(p);
			poll.setMediaSource((String)redisTemplate.opsForHash().get(p, "media_source"));
			List<String> choices = redisTemplate.opsForList().range(p + ":choices", 0, -1);
			Map<String, String> choiceMap = new HashMap<String, String>();
			choices.forEach(c -> {
				choiceMap.put(c, (String)redisTemplate.opsForHash().get(p + ":" + c, "choice"));
			});
			poll.setPollChoices(choiceMap);
			poll.setActive(false);
			polls.add(poll);
			
		});
		return polls;
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
			
		});
		handler.handleMessage(null, null);
	}
	public String sendVoiceXml(String callerId) {
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
			
			source = source.replaceAll("\\{choice_option1\\}", "1");
			source = source.replaceAll("\\{choice_option2\\}", "2");
			
			source = source.replaceAll("\\{choice_desc1\\}", choiceMap.get("1"));
			source = source.replaceAll("\\{choice_desc2\\}", choiceMap.get("2"));
			
			if(choiceMap.size() == 3) {
				source = source.replaceAll("\\{choice_option3\\}", "3");
				source = source.replaceAll("\\{choice_desc3\\}", choiceMap.get("3"));	
			}
			source = source.replaceAll("\\{caller\\}", "'" + callerId + "'");
		}
		catch(Exception e) {
			
		}
		return source;
		
	}
	
	private String getFileContents(String filename) throws IOException {
		return new String(Files.readAllBytes(Paths.get(filename)));
	}
	
	public void setNextPoll() {
		String currentPoll = redisTemplate.opsForValue().get("campaign:current");
		if(currentPoll != null) {
			deletePoll(currentPoll);
		}
		String nextPoll = redisTemplate.opsForList().leftPop("polls");
		if(nextPoll != null) {
			redisTemplate.opsForValue().set("campaign:current", nextPoll);
		}
	}
}
