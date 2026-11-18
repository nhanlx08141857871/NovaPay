package com.novabank.novapay.domain.usecase.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.BranchType;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class FindNearestBranchUseCaseTest {

    private final FindNearestBranchUseCase useCase = new FindNearestBranchUseCase();

    @Test
    public void findNearest_returnsClosestBranchToTdtu() {
        GeoLocation tdtu = new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);
        Branch far = new Branch(
                "b_far",
                "Chi nhánh Quận 1",
                "Quận 1",
                new GeoLocation(10.7769, 106.7009),
                "0281111111",
                "08:00 - 17:00",
                BranchType.BRANCH
        );
        Branch near = new Branch(
                "b_near",
                "ATM Nguyễn Hữu Thọ",
                "Quận 7",
                new GeoLocation(10.7330, 106.7002),
                "0282222222",
                "24/7",
                BranchType.ATM
        );

        Branch result = useCase.findNearest(tdtu, Arrays.asList(far, near));
        assertNotNull(result);
        assertEquals("b_near", result.getBranchId());
    }

    @Test
    public void findNearest_handlesEmptyOrNullInputs() {
        GeoLocation tdtu = new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);
        assertNull(useCase.findNearest(tdtu, Collections.emptyList()));
        assertNull(useCase.findNearest(null, Collections.emptyList()));
    }

    @Test
    public void walkingRouteStraightLine_computesDistanceAndPositiveDuration() {
        GeoLocation origin = new GeoLocation(10.7326, 106.6998);
        GeoLocation dest = new GeoLocation(10.7380, 106.7020);
        WalkingRoute route = WalkingRoute.straightLine(origin, dest);
        assertTrue(route.isStraightLineFallback());
        assertEquals(2, route.getPoints().size());
        assertTrue(route.getDistanceMeters() > 500.0);
        assertTrue(route.getDurationSeconds() > 300.0);
    }
}
