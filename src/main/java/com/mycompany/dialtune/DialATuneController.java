package com.mycompany.dialtune;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;


@Controller
@RequestMapping("/dialatune")
public class DialATuneController {
	
	@Autowired
	SpotifyService spotifyService;
	
	@Autowired
	SoundCloudService soundCloudService;
	
	@Autowired
	PollCampaignHandler pollHandler;
	
	@Autowired
	PollResponseHandler pollResponseHandler;
	
	@Autowired
	NexmoService nexmoService;
	
	@RequestMapping("/search/spotify/{artist}")
	@ResponseBody
	public String getTopTrackLink(@PathVariable("artist") String artist) {
		return spotifyService.getTopTracksLink(artist);
	}
	
	@RequestMapping("/search/soundcloud/{artist}")
	@ResponseBody
	public String getSoundCloudTrackLink(@PathVariable("artist") String artist) {
		return soundCloudService.getTrackUrl(artist);
	}
	
	@RequestMapping(value = "/poll", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void setupPoll(@RequestBody Poll poll) {
		pollHandler.setupPoll(poll);
	}
	
	@RequestMapping(value = "/poll/current/details")
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public Poll getCurrentPollDetails() {
		return pollHandler.getCurrentPollDetails();
	}
	
	@RequestMapping(value = "/poll/current/votes")
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public void getCurrentPollVotes() {
		pollHandler.getCurrentPollVotes();
	}
	
	@RequestMapping(value = "/poll/{poll_name}/activate")
	@ResponseStatus(value = HttpStatus.OK)
	public void activatePoll(@PathVariable("poll_name") String pollName) {
		pollHandler.activatePoll(pollName);
	}
	
	@RequestMapping("/nexmo/callback")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String handleNexmoCallback(@RequestParam Map<String,String> allRequestParams, ModelMap model) throws Exception {
		pollResponseHandler.handleMessage(allRequestParams.get("msisdn"), allRequestParams.get("text"));
		return "true";
	}
	
	@RequestMapping(value="/nexmo/voice", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String handleNexmoVoiceCallback(@RequestParam Map<String,String> allRequestParams, ModelMap model) throws Exception {
		allRequestParams.forEach((k,v) -> System.out.println("Key: " + k + ", value: " + v));
		String dept = allRequestParams.get("department");
		String data[] = dept.split("|");
		pollResponseHandler.handleMessage(data[1], data[0]);
		//pollResponseHandler.handleMessage(allRequestParams.get("msisdn"), allRequestParams.get("text"));
		return "true";
	}
	
	@RequestMapping(value="/nexmo/voice/call", produces="application/xml", headers = "Accept=application/xml")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String sendVoiceXml(@RequestParam("nexmo_caller_id") String callerId) throws Exception {
		// nexmo_caller_id=17732304340&session.callerid=unknown&session.accountid=1&session.sessionid=0bc32933db364647daede958dee16323&session.parentsessionid=2962e8376352f9dee415f36c4eb69d5b&nexmo_call_id=2962e8376352f9dee415f36c4eb69d5b-1&session.virtualplatform=Default&session.calledid=unknown
		//pollResponseHandler.handleMessage(allRequestParams.get("msisdn"), allRequestParams.get("text"));
		return pollHandler.sendVoiceXml(callerId);
	}
	
	@RequestMapping("/poll/{poll_name}/winner")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String determineWinner(@PathVariable("poll_name") String pollName) {
		return pollHandler.getWinner(pollName);
	}
	
	@RequestMapping("/poll/{poll_name}/expire")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String expirePoll(@PathVariable("poll_name") String pollName) {
		return pollHandler.expirePoll(pollName).toString();
	}
	
	@RequestMapping("/nexmo/{phone_number}/country")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public String getCountryCode(@PathVariable("phone_number") String phoneNumber) {
		return nexmoService.getCountryCode(phoneNumber);
	}
	
	@RequestMapping("/nexmo/callback_old")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	// msisdn=19150000001&to=12108054321
	//&messageId=000000FFFB0356D1&text=This+is+an+inbound+message
	//&type=text&message-timestamp=2012-08-19+20%3A38%3A23
	public String handleNexmoCallbackOld(@RequestParam Map<String,String> allRequestParams, ModelMap model) throws Exception {
	/* handleNexmoCallback(@RequestParam(required = false, value="msisdn") String mIsdn, @RequestParam(required = false, value="to") String to, @RequestParam(required = false, value="messageId") String messageId,
			@RequestParam(required = false, value="text") String text, @RequestParam(required = false, value="type") String type, @RequestParam(required = false, value="message-timestamp") String timestamp,
			@RequestParam(required = false, value="keyword") String keyword) {
			*/
		allRequestParams.forEach((k, v) -> 
	    System.out.println(k + "=" + v));
		
		
		Pubnub pubnub = new Pubnub("pub-c-8d096ba1-aa07-435b-badb-5c5d65686626", "sub-c-b7b1860c-6198-11e5-88ff-02ee2ddab7fe");
		
		Callback callback = new Callback() {
			  public void successCallback(String channel, Object response) {
			    System.out.println(response.toString());
			  }
			  public void errorCallback(String channel, Object error) {
			    System.out.println(error.toString());
			  }
			};
			
			
			JSONObject data = new JSONObject();
			
			JSONArray austinArray = new JSONArray();
			austinArray.put("Austin");
			austinArray.put("25");
			
			JSONArray chicagoArray = new JSONArray();
			chicagoArray.put("Chicago");
			chicagoArray.put("75");
			
			JSONArray mainArray = new JSONArray();
			mainArray.put(austinArray);
			mainArray.put(chicagoArray);
			

			try {
			    data.put("columns", mainArray);
			} catch (JSONException e) {
			    e.printStackTrace();
			}

			pubnub.publish("test_channel", data, callback);
			//pubnub.publish("test_channel", "Hello from the PubNub Java SDK!" , callback);
			//pubnub.publish("test_channel", "{'columns': [['Portlandia', 10],['Austin',25],['Chicago',75]]}", callback);
			// "columns: [['Austin',25],['Chicago',75]]"
	   
			
			pubnub.subscribe("test_channel", new Callback() {
				@Override
			      public void connectCallback(String channel, Object message) {
			          System.out.println("SUBSCRIBE : CONNECT on channel:" + channel
			                     + " : " + message.getClass() + " : "
			                     + message.toString());
			      }
			 
			      @Override
			      public void disconnectCallback(String channel, Object message) {
			          System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
			                     + " : " + message.getClass() + " : "
			                     + message.toString());
			      }
			 
			      public void reconnectCallback(String channel, Object message) {
			          System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
			                     + " : " + message.getClass() + " : "
			                     + message.toString());
			      }
			 
			      @Override
			      public void successCallback(String channel, Object message) {
			          System.out.println("SUBSCRIBE : " + channel + " : "
			                     + message.getClass() + " : " + message.toString());
			      }
			 
			      @Override
			      public void errorCallback(String channel, Object error) {
			          System.out.println("SUBSCRIBE : ERROR on channel " + channel
			                     + " : " + error.toString());
			      }
			});
		
		return "true";		
	}
	@RequestMapping(value="/nexmo/callback",method=RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	// msisdn=19150000001&to=12108054321
	//&messageId=000000FFFB0356D1&text=This+is+an+inbound+message
	//&type=text&message-timestamp=2012-08-19+20%3A38%3A23
	public String handleNexmoCallbackPost(@RequestParam Map<String,String> allRequestParams, ModelMap model) {
	/* handleNexmoCallback(@RequestParam(required = false, value="msisdn") String mIsdn, @RequestParam(required = false, value="to") String to, @RequestParam(required = false, value="messageId") String messageId,
			@RequestParam(required = false, value="text") String text, @RequestParam(required = false, value="type") String type, @RequestParam(required = false, value="message-timestamp") String timestamp,
			@RequestParam(required = false, value="keyword") String keyword) {
			*/
		allRequestParams.forEach((k, v) -> 
	    System.out.println(k + "=" + v));
		return "true";		
	}
}
