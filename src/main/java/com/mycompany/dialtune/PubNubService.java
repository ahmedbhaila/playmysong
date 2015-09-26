package com.mycompany.dialtune;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;

public class PubNubService {

	@Autowired
	Pubnub pubNub;
	
	@Value("${pubnub.channel.name}")
	String votingChannel;

	public void publishMessage(JSONObject message) {

		Callback callback = new Callback() {
			public void successCallback(String channel, Object response) {
				System.out.println(response.toString());
			}

			public void errorCallback(String channel, Object error) {
				System.out.println(error.toString());
			}
		};

		pubNub.publish(votingChannel, message, callback);
	}
	public void publishMessage(JSONObject message, String votingChannel) {

		Callback callback = new Callback() {
			public void successCallback(String channel, Object response) {
				System.out.println(response.toString());
			}

			public void errorCallback(String channel, Object error) {
				System.out.println(error.toString());
			}
		};

		pubNub.publish(votingChannel, message, callback);
	}
}
