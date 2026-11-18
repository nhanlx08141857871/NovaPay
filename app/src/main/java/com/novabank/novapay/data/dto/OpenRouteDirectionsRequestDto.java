package com.novabank.novapay.data.dto;

import com.novabank.novapay.domain.model.branch.GeoLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class OpenRouteDirectionsRequestDto {

    private final List<List<Double>> coordinates;

    public OpenRouteDirectionsRequestDto(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public static OpenRouteDirectionsRequestDto fromLocations(
            GeoLocation origin,
            GeoLocation destination
    ) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        return new OpenRouteDirectionsRequestDto(Arrays.asList(
                Arrays.asList(origin.getLng(), origin.getLat()),
                Arrays.asList(destination.getLng(), destination.getLat())
        ));
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }
}
