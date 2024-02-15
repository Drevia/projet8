package com.openclassrooms.tourguide.mapper;

import com.openclassrooms.tourguide.model.NearAttractionResult;
import com.openclassrooms.tourguide.model.NearbyAttraction;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NearAttractionMapper {


    @Mapping(target = "attractionLat", source = "attraction.latitude")
    @Mapping(target = "attractionLong", source = "attraction.longitude")
    @Mapping(target = "rewardPoint", ignore = true)
    @Mapping(target = "distance", ignore = true)
    NearbyAttraction attractionToNearbyAttraction(Attraction attraction, Location userLocation);
}
