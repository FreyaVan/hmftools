package com.hartwig.hmftools.patientreporter;

import java.io.IOException;

import com.hartwig.hmftools.common.cosmic.fusions.CosmicFusionModel;
import com.hartwig.hmftools.common.cosmic.fusions.CosmicFusions;
import com.hartwig.hmftools.common.cosmic.genes.CosmicGeneModel;
import com.hartwig.hmftools.common.cosmic.genes.CosmicGenes;
import com.hartwig.hmftools.common.exception.HartwigException;
import com.hartwig.hmftools.common.gene.GeneModel;
import com.hartwig.hmftools.genepanel.HmfGenePanelSupplier;
import com.hartwig.hmftools.patientreporter.filters.DrupFilter;
import com.hartwig.hmftools.patientreporter.variants.ImmutableMicrosatelliteAnalyzer;
import com.hartwig.hmftools.patientreporter.variants.MicrosatelliteAnalyzer;

import org.jetbrains.annotations.NotNull;

final class HmfReporterDataLoader {
    private HmfReporterDataLoader() {
    }

    @NotNull
    static HmfReporterData buildFromFiles(@NotNull final String cosmicGeneFile, @NotNull final String cosmicFusionFile,
            @NotNull final String drupFilterFile, @NotNull final String fastaFileLocation) throws IOException, HartwigException {
        final GeneModel panelGeneModel = new GeneModel(HmfGenePanelSupplier.hmfPanelGeneMap());
        final CosmicGeneModel cosmicGeneModel = CosmicGenes.readFromCSV(cosmicGeneFile);
        final CosmicFusionModel cosmicFusionModel = CosmicFusions.readFromCSV(cosmicFusionFile);
        final DrupFilter drupFilter = new DrupFilter(drupFilterFile);
        final MicrosatelliteAnalyzer microsatelliteAnalyzer = ImmutableMicrosatelliteAnalyzer.of(fastaFileLocation);
        return ImmutableHmfReporterData.of(panelGeneModel, cosmicGeneModel, cosmicFusionModel, drupFilter, microsatelliteAnalyzer);
    }
}
