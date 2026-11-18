package com.novabank.novapay.domain.usecase.branch;

import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.GeoLocation;

import java.util.List;

import javax.inject.Inject;

public final class FindNearestBranchUseCase {

    @Inject
    public FindNearestBranchUseCase() {
    }

    public Branch findNearest(GeoLocation userLocation, List<Branch> branches) {
        if (userLocation == null || branches == null || branches.isEmpty()) {
            return null;
        }
        Branch nearest = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (Branch branch : branches) {
            if (branch == null || branch.getLocation() == null) {
                continue;
            }
            double dist = userLocation.distanceMetersTo(branch.getLocation());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = branch;
            }
        }
        return nearest;
    }
}
