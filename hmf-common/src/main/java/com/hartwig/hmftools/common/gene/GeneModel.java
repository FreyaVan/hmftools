package com.hartwig.hmftools.common.gene;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.SortedSetMultimap;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.region.hmfslicer.HmfGenomeRegion;
import com.hartwig.hmftools.common.slicing.Slicer;
import com.hartwig.hmftools.common.slicing.SlicerFactory;

import org.jetbrains.annotations.NotNull;

public class GeneModel {

    @NotNull
    private final Collection<HmfGenomeRegion> regions;
    @NotNull
    private final Slicer slicer;
    @NotNull
    private final Map<String, HmfGenomeRegion> transcriptMap;
    @NotNull
    private final Set<String> panel;

    public GeneModel(@NotNull SortedSetMultimap<String, HmfGenomeRegion> regions) {
        this.regions = regions.values();
        this.slicer = SlicerFactory.fromRegions(regions);
        this.transcriptMap = extractTranscriptMap(regions.values());
        this.panel = regions.values().stream().map(TranscriptRegion::gene).collect(Collectors.toSet());
    }

    @NotNull
    public Collection<HmfGenomeRegion> regions() {
        return regions;
    }

    public long numberOfBases() {
        return regions.stream().mapToLong(GenomeRegion::bases).sum();
    }

    public int numberOfRegions() {
        return regions.size();
    }

    @NotNull
    public Slicer slicer() {
        return slicer;
    }

    @NotNull
    public Map<String, HmfGenomeRegion> transcriptMap() {
        return transcriptMap;
    }

    @NotNull
    private static Map<String, HmfGenomeRegion> extractTranscriptMap(final @NotNull Collection<HmfGenomeRegion> regions) {
        final Map<String, HmfGenomeRegion> transcriptMap = Maps.newHashMap();
        for (final HmfGenomeRegion region : regions) {
            transcriptMap.put(region.transcriptID(), region);
        }
        return transcriptMap;
    }

    @NotNull
    public Set<String> panel() {
        return panel;
    }
}
