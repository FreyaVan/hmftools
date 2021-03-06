package com.hartwig.hmftools.serve.sources.vicc;

import java.util.List;
import java.util.Map;

import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.genome.genepanel.HmfGenePanelSupplier;
import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;
import com.hartwig.hmftools.serve.hotspot.ProteinResolver;
import com.hartwig.hmftools.serve.sources.vicc.extractor.CopyNumberExtractor;
import com.hartwig.hmftools.serve.sources.vicc.extractor.FusionExtractor;
import com.hartwig.hmftools.serve.sources.vicc.extractor.GeneLevelExtractor;
import com.hartwig.hmftools.serve.sources.vicc.extractor.GeneRangeExtractor;
import com.hartwig.hmftools.serve.sources.vicc.extractor.HotspotExtractor;
import com.hartwig.hmftools.serve.sources.vicc.extractor.SignaturesExtractor;
import com.hartwig.hmftools.vicc.annotation.ProteinAnnotationExtractor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ViccExtractorFactory {

    private ViccExtractorFactory() {
    }

    @NotNull
    public static ViccExtractor buildViccExtractor(@NotNull ProteinResolver proteinResolver, @NotNull List<DriverGene> driverGenes) {
        return buildViccExtractorWithInterpretationTsv(proteinResolver, driverGenes, null);
    }

    @NotNull
    public static ViccExtractor buildViccExtractorWithInterpretationTsv(@NotNull ProteinResolver proteinResolver,
            @NotNull List<DriverGene> driverGenes, @Nullable String featureInterpretationTsv) {
        Map<String, HmfTranscriptRegion> transcriptPerGeneMap = HmfGenePanelSupplier.allGenesMap37();

        return new ViccExtractor(new HotspotExtractor(proteinResolver, new ProteinAnnotationExtractor()),
                new CopyNumberExtractor(transcriptPerGeneMap),
                new FusionExtractor(transcriptPerGeneMap),
                new GeneLevelExtractor(transcriptPerGeneMap, driverGenes),
                new GeneRangeExtractor(transcriptPerGeneMap, driverGenes),
                new SignaturesExtractor(),
                featureInterpretationTsv);
    }
}
