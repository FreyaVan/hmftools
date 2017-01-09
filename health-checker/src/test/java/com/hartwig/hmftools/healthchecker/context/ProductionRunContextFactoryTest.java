package com.hartwig.hmftools.healthchecker.context;

import static org.junit.Assert.assertEquals;

import java.io.File;

import com.google.common.io.Resources;
import com.hartwig.hmftools.common.exception.HartwigException;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ProductionRunContextFactoryTest {

    private static final String RESOURCE_DIR = "ProductionRunContextFactory";

    private static final String VALID_CPCT_RUNDIR = "160101_HMFregCPCT_FR10002000_FR20003000_CPCT12345678";
    private static final String VALID_CPCT_RUN_NAME = VALID_CPCT_RUNDIR;
    private static final String VALID_CPCT_REF = "CPCT12345678R";
    private static final String VALID_CPCT_TUMOR = "CPCT12345678TII";

    private static final String VALID_DRUP_RUNDIR = "170101_HMFregDRUP_FR10002000_FR10012001_DRUP01020005";
    private static final String VALID_DRUP_RUN_NAME = VALID_DRUP_RUNDIR;
    private static final String VALID_DRUP_REF = "DRUP01020005R";
    private static final String VALID_DRUP_TUMOR = "DRUP01020005T";

    private static final String LOW_QUAL_CPCT_RUNDIR = "160102_HMFregCPCT_FR10002000_FR20003000_CPCT12345678_LowQual";
    private static final String LOW_QUAL_CPCT_RUN_NAME = "160102_HMFregCPCT_FR10002000_FR20003000_CPCT12345678";
    private static final String LOW_QUAL_CPCT_REF = "CPCT12345678R";
    private static final String LOW_QUAL_CPCT_TUMOR = "CPCT12345678T";

    private static final String INVALID_CPCT_PATIENT_RUNDIR = "160103_HMFregCPCT_FR10002000_FR20003000_CPCT1234";
    private static final String MISSING_REF_CPCT_RUNDIR = "160104_HMFregCPCT_FR10002000_FR20003000_CPCT12345678";

    @Test
    public void alsoWorksForDRUP() throws HartwigException {
        final RunContext runContext = ProductionRunContextFactory.fromRunDirectory(toPath(VALID_DRUP_RUNDIR));
        assertEquals(VALID_DRUP_REF, runContext.refSample());
        assertEquals(VALID_DRUP_TUMOR, runContext.tumorSample());
        assertEquals(VALID_DRUP_RUN_NAME, runContext.runName());
        assertEquals(true, runContext.hasPassedTests());
    }

    @Test
    public void resolveCorrectlyForValidCPCTRunWithTII() throws HartwigException {
        final RunContext runContext = ProductionRunContextFactory.fromRunDirectory(toPath(VALID_CPCT_RUNDIR));
        assertEquals(VALID_CPCT_REF, runContext.refSample());
        assertEquals(VALID_CPCT_TUMOR, runContext.tumorSample());
        assertEquals(VALID_CPCT_RUN_NAME, runContext.runName());
        assertEquals(true, runContext.hasPassedTests());
    }

    @Test
    public void resolveCorrectlyForLowQualCPCTRun() throws HartwigException {
        final RunContext runContextLowQual = ProductionRunContextFactory.fromRunDirectory(
                toPath(LOW_QUAL_CPCT_RUNDIR));
        assertEquals(LOW_QUAL_CPCT_REF, runContextLowQual.refSample());
        assertEquals(LOW_QUAL_CPCT_TUMOR, runContextLowQual.tumorSample());
        assertEquals(LOW_QUAL_CPCT_RUN_NAME, runContextLowQual.runName());
        assertEquals(false, runContextLowQual.hasPassedTests());
    }

    @Test(expected = MalformedRunDirException.class)
    public void exceptionOnCPCTRunDirWithTooShortPatientName() throws HartwigException {
        ProductionRunContextFactory.fromRunDirectory(toPath(INVALID_CPCT_PATIENT_RUNDIR));
    }

    @Test(expected = MalformedRunDirException.class)
    public void exceptionOnCPCTRunDirWithMissingRef() throws HartwigException {
        ProductionRunContextFactory.fromRunDirectory(toPath(MISSING_REF_CPCT_RUNDIR));
    }

    @NotNull
    private static String toPath(@NotNull final String runDirectory) {
        return Resources.getResource(RESOURCE_DIR + File.separator + runDirectory).getPath();
    }
}