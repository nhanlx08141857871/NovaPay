package com.novabank.novapay.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.novabank.novapay.data.dto.OpenRouteDirectionsRequestDto;
import com.novabank.novapay.data.dto.OpenRouteDirectionsResponseDto;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OpenRouteMapperTest {

    @Test
    public void requestDto_serializesCoordinatesAsLngLat() {
        GeoLocation origin = new GeoLocation(10.7326, 106.6998);
        GeoLocation dest = new GeoLocation(10.7400, 106.7100);
        OpenRouteDirectionsRequestDto dto = OpenRouteDirectionsRequestDto.fromLocations(origin, dest);

        List<List<Double>> coords = dto.getCoordinates();
        assertEquals(2, coords.size());
        assertEquals(106.6998, coords.get(0).get(0), 0.00001);
        assertEquals(10.7326, coords.get(0).get(1), 0.00001);
        assertEquals(106.7100, coords.get(1).get(0), 0.00001);
        assertEquals(10.7400, coords.get(1).get(1), 0.00001);
    }

    @Test
    public void toWalkingRoute_swapsLngLatToLatLngAndMapsSummary() {
        OpenRouteDirectionsResponseDto.GeometryDto geometry =
                new OpenRouteDirectionsResponseDto.GeometryDto();
        geometry.setCoordinates(Arrays.asList(
                Arrays.asList(106.6998, 10.7326),
                Arrays.asList(106.7010, 10.7340)
        ));

        OpenRouteDirectionsResponseDto.SummaryDto summary =
                new OpenRouteDirectionsResponseDto.SummaryDto();
        summary.setDistance(420.5);
        summary.setDuration(315.0);

        OpenRouteDirectionsResponseDto.PropertiesDto props =
                new OpenRouteDirectionsResponseDto.PropertiesDto();
        props.setSummary(summary);

        OpenRouteDirectionsResponseDto.FeatureDto feature =
                new OpenRouteDirectionsResponseDto.FeatureDto();
        feature.setGeometry(geometry);
        feature.setProperties(props);

        OpenRouteDirectionsResponseDto response = new OpenRouteDirectionsResponseDto();
        response.setFeatures(Collections.singletonList(feature));

        WalkingRoute route = OpenRouteMapper.toWalkingRoute(response);
        assertNotNull(route);
        assertFalse(route.isStraightLineFallback());
        assertEquals(2, route.getPoints().size());
        assertEquals(10.7326, route.getPoints().get(0).getLat(), 0.00001);
        assertEquals(106.6998, route.getPoints().get(0).getLng(), 0.00001);
        assertEquals(420.5, route.getDistanceMeters(), 0.01);
        assertEquals(315.0, route.getDurationSeconds(), 0.01);
    }

    @Test
    public void toWalkingRoute_returnsNullOnEmptyFeaturesForFallback() {
        OpenRouteDirectionsResponseDto empty = new OpenRouteDirectionsResponseDto();
        empty.setFeatures(Collections.emptyList());
        assertNull(OpenRouteMapper.toWalkingRoute(empty));
        assertNull(OpenRouteMapper.toWalkingRoute(null));
    }
}
