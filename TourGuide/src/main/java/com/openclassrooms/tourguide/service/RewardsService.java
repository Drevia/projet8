package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
	public static final int FIXED_THREAD_POOLS_SIZE = 20;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	private final ExecutorService executorService = Executors.newFixedThreadPool(FIXED_THREAD_POOLS_SIZE);
	private final Logger logger = LoggerFactory.getLogger(RewardsService.class);

	private List<Attraction> attractionList;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}


	public CompletableFuture<Void> calculateRewards(User user) {

		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = getAttractions();

		List<VisitedLocation> userLocationsCopy = new ArrayList<>(userLocations);

		long start = System.currentTimeMillis();

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		//TODO: refacto les boucle for
		for (VisitedLocation visitedLocation : userLocationsCopy) {
			for (Attraction attraction : attractions) {
				if (shouldAddReward(user, attraction) && nearAttraction(visitedLocation, attraction)) {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
							addReward(user, visitedLocation, attraction), executorService);
					futures.add(future);
				}
			}
		}

		//Attendre la complétion de tout les future
		CompletableFuture<Void>[] futureArray = futures.toArray(new CompletableFuture[0]);

		long end = System.currentTimeMillis();
		logger.debug("temps total de l'operation: {} ms", end - start);

		return CompletableFuture.allOf(futureArray);
	}

	private List<Attraction> getAttractions() {
		if (attractionList == null){
			attractionList = gpsUtil.getAttractions();
			return attractionList;
		} else {
			return attractionList;
		}
	}

	//doublon avec User.addUserReward
	private boolean shouldAddReward(User user, Attraction attraction) {
		return user.getUserRewards().stream()
				.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName));
	}

	//Bouger le getReward après etre sur qu'on peut ajouter le UserReward
	private void addReward(User user, VisitedLocation visitedLocation, Attraction attraction) {
		int rewardPoints = getRewardPoints(attraction, user);
		user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
	}


	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		CompletableFuture<Integer> futureRewardPoints = CompletableFuture.supplyAsync(() ->
		rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId()));

		return futureRewardPoints.join();
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
		return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}
