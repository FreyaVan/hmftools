package com.hartwig.hmftools.protect.variants.germline;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.google.common.io.Resources;

import org.junit.Test;

public class GermlineReportingEntryFileTest {

    private static final String GERMLINE_REPORTING_TSV = Resources.getResource("germline/germline_reporting.tsv").getPath();

    @Test
    public void canLoadGermlineReportingTsv() throws IOException {
        GermlineReportingModel germlineReportingModel = GermlineReportingFile.buildFromTsv(GERMLINE_REPORTING_TSV);

        assertEquals(3, germlineReportingModel.reportableGermlineGenes().size());
    }
}