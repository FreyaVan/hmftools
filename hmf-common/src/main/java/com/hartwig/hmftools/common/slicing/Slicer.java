package com.hartwig.hmftools.common.slicing;

import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.SortedSetMultimap;
import com.hartwig.hmftools.common.variant.SomaticVariant;

import com.hartwig.hmftools.common.variant.Variant;
import org.jetbrains.annotations.NotNull;

public class Slicer {

    @NotNull
    private final SortedSetMultimap<String, GenomeRegion> regions;

    Slicer(@NotNull final SortedSetMultimap<String, GenomeRegion> regions) {
        this.regions = regions;
    }

    public boolean includes(@NotNull Variant variant) {
        final Collection<GenomeRegion> regionsForChrom = regions.get(variant.chromosome());
        if (regionsForChrom == null) {
            return false;
        } else {
            for (final GenomeRegion region : regionsForChrom) {
                if (variant.position() >= region.start() && variant.position() <= region.end()) {
                    return true;
                } else if (region.start() > variant.position()) {
                    return false;
                }
            }
        }

        return false;
    }

    @NotNull
    public Collection<GenomeRegion> regions() {
        return regions.values();
    }

    public int numberOfRegions() {
        return regions.size();
    }

    @VisibleForTesting
    public long numberOfBases() {
        long bases = 0;
        for (final GenomeRegion region : regions.values()) {
            bases += region.bases();
        }
        return bases;
    }
}
