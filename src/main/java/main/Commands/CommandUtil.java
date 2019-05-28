package main.Commands;

import DAO.DaoImplementation;
import DAO.Entities.ArtistInfo;
import main.APIs.Discogs.DiscogsApi;
import main.APIs.Spotify.Spotify;
import main.Exceptions.DiscogsServiceException;

import java.awt.*;
import java.net.URL;
import java.util.Random;

public class CommandUtil {
	static String getDiscogsUrl(DiscogsApi discogsApi, String artist, DaoImplementation dao, Spotify spotify) {
		String newUrl = null;
		try {
			newUrl = discogsApi.findArtistImage(artist);
			if (!newUrl.isEmpty()) {
				dao.upsertUrl(new ArtistInfo(newUrl, artist));
			} else {
				newUrl = spotify.getArtistUrlImage(artist);
				dao.upsertSpotify(new ArtistInfo(newUrl, artist));
			}
		} catch (DiscogsServiceException ignored) {

		}
		return newUrl;
	}

	static String noImageUrl(String artist) {
		return artist == null || artist.isEmpty() ? "https://lastfm-img2.akamaized.net/i/u/174s/4128a6eb29f94943c9d206c08e625904" : artist;
	}

//	static CompletableFuture<String> getDiscogsUrlAync(DiscogsApi discogsApi, String artist, DaoImplementation dao) {
//		return CompletableFuture.supplyAsync(() -> getDiscogsUrl(discogsApi, artist, dao));
//	}


	public static Color randomColor() {
		Random rand = new Random();
		double r = rand.nextFloat() / 2f + 0.5;
		double g = rand.nextFloat() / 2f + 0.5;
		double b = rand.nextFloat() / 2f + 0.5;
		return new Color((float) r, (float) g, (float) b);
	}

	public static boolean isValidURL(String urlString) {
		try {
			URL url = new URL(urlString);
			url.toURI();
			return true;
		} catch (Exception exception) {
			return false;
		}
	}


}