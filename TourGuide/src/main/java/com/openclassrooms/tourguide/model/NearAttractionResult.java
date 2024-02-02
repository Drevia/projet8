package com.openclassrooms.tourguide.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NearAttractionResult {

    private List<NearbyAttraction> nearbyAttractionList;
    private double userLat;
    private double userLong;

}
