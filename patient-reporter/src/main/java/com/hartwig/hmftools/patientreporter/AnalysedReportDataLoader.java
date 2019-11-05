package com.hartwig.hmftools.patientreporter;

import java.io.IOException;

import com.hartwig.hmftools.common.actionability.ActionabilityAnalyzer;
import com.hartwig.hmftools.patientreporter.summary.SummaryFile;
import com.hartwig.hmftools.patientreporter.summary.SummaryModel;
import com.hartwig.hmftools.common.driverGene.DriverGeneViewFactory;
import com.hartwig.hmftools.common.bachelor.GermlineReportingFile;
import com.hartwig.hmftools.common.bachelor.GermlineReportingModel;

import org.jetbrains.annotations.NotNull;

final class AnalysedReportDataLoader {

    private AnalysedReportDataLoader() {
    }

    @NotNull
    static AnalysedReportData buildFromFiles(@NotNull ReportData reportData, @NotNull String knowledgebaseDir,
             @NotNull String germlineGenesCsv, @NotNull String sampleSummaryTsv) throws IOException {
        final ActionabilityAnalyzer actionabilityAnalyzer = ActionabilityAnalyzer.fromKnowledgebase(knowledgebaseDir);

        final GermlineReportingModel germlineReportingModel = GermlineReportingFile.buildFromCsv(germlineGenesCsv);
        final SummaryModel summaryModel = SummaryFile.buildFromTsv(sampleSummaryTsv);

        return ImmutableAnalysedReportData.builder()
                .from(reportData)
                .driverGeneView(DriverGeneViewFactory.create())
                .actionabilityAnalyzer(actionabilityAnalyzer)
                .germlineReportingModel(germlineReportingModel)
                .summaryModel(summaryModel)
                .build();
    }
}
