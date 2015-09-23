package com.mycompany.dialtune;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@Controller
@RequestMapping("/dialatune")
public class DialATuneController {
	
	@Autowired
	SpotifyService spotifyService;
	
	@Autowired
	SoundCloudService soundCloudService;
	
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
	
	@RequestMapping("/nexmo/callback")
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	// msisdn=19150000001&to=12108054321
	//&messageId=000000FFFB0356D1&text=This+is+an+inbound+message
	//&type=text&message-timestamp=2012-08-19+20%3A38%3A23
	public String handleNexmoCallback(@RequestParam Map<String,String> allRequestParams, ModelMap model) {
	/* handleNexmoCallback(@RequestParam(required = false, value="msisdn") String mIsdn, @RequestParam(required = false, value="to") String to, @RequestParam(required = false, value="messageId") String messageId,
			@RequestParam(required = false, value="text") String text, @RequestParam(required = false, value="type") String type, @RequestParam(required = false, value="message-timestamp") String timestamp,
			@RequestParam(required = false, value="keyword") String keyword) {
			*/
		allRequestParams.forEach((k, v) -> 
	    System.out.println(k + "=" + v));
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
