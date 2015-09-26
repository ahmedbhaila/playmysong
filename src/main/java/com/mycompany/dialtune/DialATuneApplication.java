package com.mycompany.dialtune;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import com.pubnub.api.Pubnub;

@SpringBootApplication
public class DialATuneApplication {

	@Value("${pubnub.subscribe.key}")
	String pubNubSubscribeKey;
	
	@Value("${pubnub.publish.key}")
	String pubNubPublishKey;
	
	@Bean
	public Pubnub pubNub() {
		Pubnub pubnub = new Pubnub(pubNubSubscribeKey, pubNubPublishKey);
		return pubnub;
	}
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public SpotifyService spotifyService() {
		return new SpotifyService();
	}
	
	@Bean
	public SoundCloudService soundCloudService() {
		return new SoundCloudService();
	}
	
	@Bean
	public JedisConnectionFactory jedisConnectionFactory() throws Exception {
		URI redisUri = new URI(System.getenv("REDISCLOUD_URL"));
		JedisConnectionFactory jedisFactory = new JedisConnectionFactory();
		jedisFactory.setHostName(redisUri.getHost());
		jedisFactory.setPort(redisUri.getPort());
		jedisFactory.setPassword(redisUri.getUserInfo().split(":", 2)[1]);
		return jedisFactory;
	}

	@Bean
	public StringRedisSerializer stringSerializer() {
		return new StringRedisSerializer();
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate() throws Exception {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(jedisConnectionFactory());
		redisTemplate.setKeySerializer(stringSerializer());
		redisTemplate.setHashKeySerializer(stringSerializer());
		redisTemplate.setHashValueSerializer(stringSerializer());
		redisTemplate.setValueSerializer(stringSerializer());
		return redisTemplate;
	}
	
	@Bean
	public PollCampaignHandler campaignHandler() {
		return new PollCampaignHandler();
	}
	
	@Bean
	public PollResponseHandler responseHandler() {
		return new PollResponseHandler();
	}
	
	@Bean
	public PubNubService pubNubService() {
		return new PubNubService();
	}

	@Bean
	public NexmoService nexmoService() {
		return new NexmoService();
	}
	
	@Bean
	public GoogleGeoService googleGeoService() {
		return new GoogleGeoService();
	}
	
    public static void main(String[] args) {
        SpringApplication.run(DialATuneApplication.class, args);
    }
}
