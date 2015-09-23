package com.mycompany.dialtune;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class DialATuneApplication {

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
	
    public static void main(String[] args) {
        SpringApplication.run(DialATuneApplication.class, args);
    }
}
