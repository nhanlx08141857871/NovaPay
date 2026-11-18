package com.novabank.novapay.data.dto;

import java.util.List;

public final class OpenRouteDirectionsResponseDto {

    private List<FeatureDto> features;

    public List<FeatureDto> getFeatures() {
        return features;
    }

    public void setFeatures(List<FeatureDto> features) {
        this.features = features;
    }

    public static final class FeatureDto {
        private GeometryDto geometry;
        private PropertiesDto properties;

        public GeometryDto getGeometry() {
            return geometry;
        }

        public void setGeometry(GeometryDto geometry) {
            this.geometry = geometry;
        }

        public PropertiesDto getProperties() {
            return properties;
        }

        public void setProperties(PropertiesDto properties) {
            this.properties = properties;
        }
    }

    public static final class GeometryDto {
        private List<List<Double>> coordinates;

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<List<Double>> coordinates) {
            this.coordinates = coordinates;
        }
    }

    public static final class PropertiesDto {
        private SummaryDto summary;

        public SummaryDto getSummary() {
            return summary;
        }

        public void setSummary(SummaryDto summary) {
            this.summary = summary;
        }
    }

    public static final class SummaryDto {
        private double distance;
        private double duration;

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }
    }
}
