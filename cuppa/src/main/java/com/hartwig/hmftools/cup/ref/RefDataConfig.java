package com.hartwig.hmftools.cup.ref;

import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.cup.CuppaConfig.LOG_DEBUG;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SAMPLE_DATA_FILE;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SNV_COUNTS_FILE;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SNV_SAMPLE_POS_FREQ_FILE;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.addDatabaseCmdLineArgs;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.createDatabaseAccess;

import com.hartwig.hmftools.cup.rna.RefRnaExpression;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class RefDataConfig
{
    public final String OutputDir;

    // reference data
    public final String RefSampleDataFile;
    public final String RefSampleTraitsFile;
    public final String RefSampleSvDataFile;
    public final String RefSigContribsFile;
    public final String RefSnvPositionDataFile;
    public final String RefSnvCountsFile;
    public final String RefRnaGeneExpFile;

    public final DatabaseAccess DbAccess;

    // config strings
    public static final String REF_SAMPLE_TRAITS_FILE = "ref_sample_traits_file";
    public static final String REF_SIG_CONTRIBS_FILE = "ref_sig_contribs_file";
    public static final String REF_SV_DATA_FILE = "ref_sv_data_file";
    public static final String REF_RNA_GENE_EXP_DATA_FILE = "ref_rna_gene_exp_file";

    public RefDataConfig(final CommandLine cmd)
    {
        RefSampleDataFile = cmd.getOptionValue(REF_SAMPLE_DATA_FILE, "");
        RefSampleTraitsFile = cmd.getOptionValue(REF_SAMPLE_TRAITS_FILE, "");
        RefSigContribsFile = cmd.getOptionValue(REF_SIG_CONTRIBS_FILE, "");
        RefSampleSvDataFile = cmd.getOptionValue(REF_SV_DATA_FILE, "");
        RefSnvPositionDataFile = cmd.getOptionValue(REF_SNV_SAMPLE_POS_FREQ_FILE, "");
        RefSnvCountsFile = cmd.getOptionValue(REF_SNV_COUNTS_FILE, "");
        RefRnaGeneExpFile = cmd.getOptionValue(REF_RNA_GENE_EXP_DATA_FILE, "");

        DbAccess = createDatabaseAccess(cmd);

        OutputDir = parseOutputDir(cmd);
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(REF_SAMPLE_DATA_FILE, true, "Ref sample data file");
        options.addOption(REF_SAMPLE_TRAITS_FILE, true, "Ref sample traits data file");
        options.addOption(REF_SIG_CONTRIBS_FILE, true, "Ref signature contributions data file");
        options.addOption(REF_SV_DATA_FILE, true, "Ref sample SV data file");
        options.addOption(REF_SNV_SAMPLE_POS_FREQ_FILE, true, "Ref SNV position frequency matrix data file");
        options.addOption(REF_SNV_COUNTS_FILE, true, "Ref SNV trinucleotide matrix data file");
        options.addOption(REF_RNA_GENE_EXP_DATA_FILE, true, "Ref sample RNA gene expression cohort data file");

        addDatabaseCmdLineArgs(options);

        options.addOption(OUTPUT_DIR, true, "Path to output files");
        options.addOption(LOG_DEBUG, false, "Sets log level to Debug, off by default");

        RefRnaExpression.addCmdLineArgs(options);
    }

}
