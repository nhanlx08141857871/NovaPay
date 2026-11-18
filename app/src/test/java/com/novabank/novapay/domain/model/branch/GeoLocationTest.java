package com.novabank.novapay.domain.model.branch;

import com.novabank.novapay.core.common.Constants;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeoLocationTest {

    @Test
    public void distanceMetersTo_computesHaversineDistanceWithinExpectedRange() {
        GeoLocation tdtuDistrict7 = new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);
        GeoLocation benThanhDistrict1 = new GeoLocation(10.7725, 106.6980);

        double distanceMeters = tdtuDistrict7.distanceMetersTo(benThanhDistrict1);
        // Khoảng cách chim bay từ ĐH Tôn Đức Thắng (Q7) tới Chợ Bến Thành (Q1) ~ 4.4 km
        assertTrue("Expected ~4.4km but got " + distanceMeters, distanceMeters > 4_000.0 && distanceMeters < 5_000.0);
        assertFalse(benThanhDistrict1.isBeyondRadius(tdtuDistrict7, Constants.MAX_VALID_DISTANCE_FROM_HCMC_METERS));
    }

    @Test
    public void isBeyondRadius_detectsDefaultEmulatorCaliforniaLocation() {
        GeoLocation tdtuDistrict7 = new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);
        GeoLocation californiaEmulator = new GeoLocation(37.421998, -122.084000);

        assertTrue(californiaEmulator.isBeyondRadius(tdtuDistrict7, Constants.MAX_VALID_DISTANCE_FROM_HCMC_METERS));
    }
}
