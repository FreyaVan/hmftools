package com.hartwig.hmftools.common.purple.region;

import com.hartwig.hmftools.common.genome.region.GenomeRegion;
import com.hartwig.hmftools.common.purple.segment.SegmentSupport;

public interface ObservedRegion extends GenomeRegion {

    boolean ratioSupport();

    SegmentSupport support();

    int bafCount();

    double observedBAF();

    int depthWindowCount();

    double observedTumorRatio();

    double observedNormalRatio();

    double unnormalisedObservedNormalRatio();

    GermlineStatus status();

    boolean svCluster();

    double gcContent();

    long minStart();

    long maxStart();
}
