package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.mapper.NearAttractionMapper;
import com.openclassrooms.tourguide.model.NearAttractionResult;
import com.openclassrooms.tourguide.model.NearbyAttraction;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	private final NearAttractionMapper nearAttractionMapper;

	private final ExecutorService executorService = Executors.newFixedThreadPool(20);

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, NearAttractionMapper mapper) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		this.nearAttractionMapper = mapper;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		return (user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
				: trackUserLocation(user).join();
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		CompletableFuture<VisitedLocation> futureLocation = CompletableFuture.supplyAsync(() ->
				gpsUtil.getUserLocation(user.getUserId()), executorService);
		/*VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());*/
		user.addToVisitedLocations(futureLocation.join());
		rewardsService.calculateRewards(user);
		return futureLocation;
	}

	public NearAttractionResult getNearByAttractions(VisitedLocation visitedLocation, User user) {
		NearAttractionResult nearAttractionResult = new NearAttractionResult();
		List<NearbyAttraction> nearbyAttractionList = new ArrayList<>();

		for (Attraction attraction : gpsUtil.getAttractions()) {

			NearbyAttraction nearbyAttraction = nearAttractionMapper.attractionToNearbyAttraction(attraction, visitedLocation.location);

			nearbyAttraction.setDistance(rewardsService.getDistance(attraction, visitedLocation.location));
			nearbyAttraction.setRewardPoint(rewardsService.getRewardPoints(attraction, user));
			nearbyAttractionList.add(nearbyAttraction);
		}

		nearbyAttractionList = getFiveClosestAttraction(nearbyAttractionList);

		nearAttractionResult.setUserLong(visitedLocation.location.longitude);
		nearAttractionResult.setUserLat(visitedLocation.location.latitude);
		nearAttractionResult.setNearbyAttractionList(nearbyAttractionList);

		return nearAttractionResult;
	}

	private List<NearbyAttraction> getFiveClosestAttraction(List<NearbyAttraction> attractionsDistance) {
		Collections.sort(attractionsDistance, Comparator.comparingDouble(NearbyAttraction::getDistance));
		return attractionsDistance.subList(0, Math.min(5, attractionsDistance.size()));
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
