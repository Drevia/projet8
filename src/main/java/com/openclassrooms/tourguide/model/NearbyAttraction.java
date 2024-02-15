package com.openclassrooms.tourguide.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyAttraction {

    private String attractionName;
    private double attractionLat;
    private double attractionLong;
    private double distance;
    private Integer rewardPoint;
}
