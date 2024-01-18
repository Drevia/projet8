package com.openclassrooms.tourguide.mapper;

import com.openclassrooms.tourguide.model.NearAttraction;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NearAttractionMapper {

    @Mapping(target = "userLat", source = "userLocation.latitude")
    @Mapping(target = "userLong", source = "userLocation.longitude")
    @Mapping(target = "attractionLat", source = "attraction.latitude")
    @Mapping(target = "attractionLong", source = "attraction.longitude")
    @Mapping(target = "rewardPoint", ignore = true)
    @Mapping(target = "distance", ignore = true)
    NearAttraction attractionToNearAttraction(Attraction attraction, Location userLocation);
}
