package com.mycompany.dialtune;

import net.minidev.json.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class SpotifyService {
	
	private static final String SPOTIFY_ARTIST_SEARCH_URL="https://api.spotify.com/v1/search?q={artist_name}&type=artist";
	private static final String SPOTIFY_ARTISTS_TOP_TRACKS_URL = "https://api.spotify.com/v1/artists/{artist_id}/top-tracks?country=US";
	@Autowired
	RestTemplate restTemplate;
	
	public String getTopTracksLink(String artist) {
		String topTrackLink = "";
		ResponseEntity<String> response = restTemplate.getForEntity(SPOTIFY_ARTIST_SEARCH_URL, String.class, artist);
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			String artistId = (String)((JSONArray)JsonPath.read(response.getBody(), "$..artists.items[0].id")).get(0);
			
			// get top track list
			response = restTemplate.getForEntity(SPOTIFY_ARTISTS_TOP_TRACKS_URL, String.class, artistId);
			if(response.getStatusCode().equals(HttpStatus.OK)) {
				topTrackLink = (String)((JSONArray)JsonPath.read(response.getBody(), "$..tracks[0].album.uri")).get(0);
			}
		}
		return topTrackLink;
	}
}
