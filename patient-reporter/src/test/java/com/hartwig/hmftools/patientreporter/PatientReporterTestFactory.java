package com.hartwig.hmftools.patientreporter;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hartwig.hmftools.common.actionability.ActionabilityAnalyzer;
import com.hartwig.hmftools.common.clinical.PatientPrimaryTumor;
import com.hartwig.hmftools.common.lims.Lims;
import com.hartwig.hmftools.common.lims.LimsFactory;
import com.hartwig.hmftools.patientreporter.qcfail.ImmutableQCFailReportData;
import com.hartwig.hmftools.patientreporter.summary.SummaryFile;
import com.hartwig.hmftools.patientreporter.summary.SummaryModel;
import com.hartwig.hmftools.protect.variants.germline.GermlineReportingModel;

import org.jetbrains.annotations.NotNull;

public final class PatientReporterTestFactory {

    private static final String SIGNATURE_PATH = Resources.getResource("signature/signature_test.png").getPath();
    private static final String RVA_LOGO_PATH = Resources.getResource("rva_logo/rva_logo_test.jpg").getPath();
    private static final String COMPANY_LOGO_PATH = Resources.getResource("company_logo/hartwig_logo_test.jpg").getPath();

    private static final String KNOWLEDGEBASE_DIRECTORY = Resources.getResource("actionability").getPath();

    private static final String SAMPLE_SUMMARY_TSV = Resources.getResource("sample_summary/sample_summary.tsv").getPath();

    private PatientReporterTestFactory() {
    }

    @NotNull
    public static ActionabilityAnalyzer loadTestActionabilityAnalyzer() {
        try {
            return ActionabilityAnalyzer.fromKnowledgebase(KNOWLEDGEBASE_DIRECTORY);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load test actionability analyzer: " + exception.getMessage());
        }
    }

    @NotNull
    public static ReportData loadTestReportData() {
        List<PatientPrimaryTumor> patientPrimaryTumors = Lists.newArrayList();
        Lims lims = LimsFactory.empty();

        return ImmutableQCFailReportData.of(patientPrimaryTumors, lims, SIGNATURE_PATH, RVA_LOGO_PATH, COMPANY_LOGO_PATH);
    }

    @NotNull
    public static AnalysedReportData loadTestAnalysedReportData() {
        try {
            SummaryModel summaryModel = SummaryFile.buildFromTsv(SAMPLE_SUMMARY_TSV);

            return ImmutableAnalysedReportData.builder()
                    .from(loadTestReportData())
                    .actionabilityAnalyzer(loadTestActionabilityAnalyzer())
                    .germlineReportingModel(new GermlineReportingModel(Lists.newArrayList()))
                    .summaryModel(summaryModel)
                    .build();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load test analysed report data: " + exception.getMessage());
        }
    }
}
