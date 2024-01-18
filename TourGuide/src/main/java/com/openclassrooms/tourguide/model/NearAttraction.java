package com.openclassrooms.tourguide.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class NearAttraction {

    private String attractionName;
    private long attractionLat;
    private long attractionLong;
    private long userLat;
    private long userLong;
    private long distance;
    private Integer rewardPoint;
}
