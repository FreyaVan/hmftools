package com.hartwig.hmftools.cup;

public class CuppaRefFiles
{
    public static final String CUP_REF_FILE_PREFIX = "cup_ref";

    private static String formatRefFilename(final String fileType)
    {
        return String.format("%s_%s.csv", CUP_REF_FILE_PREFIX, fileType);
    }

    public static final String REF_FILE_SAMPLE_DATA = formatRefFilename("sample_data");
    public static final String REF_FILE_SNV_COUNTS = formatRefFilename("snv_counts");
    public static final String REF_FILE_CANCER_POS_FREQ_COUNTS = formatRefFilename("cancer_pos_freq_counts");
    public static final String REF_FILE_SAMPLE_POS_FREQ_COUNTS = formatRefFilename("sample_pos_freq_counts");
    public static final String REF_FILE_FEATURE_PREV = formatRefFilename("feature_prev");
    public static final String REF_FILE_DRIVER_AVG = formatRefFilename("driver_avg");
    public static final String REF_FILE_TRAIT_PERC = formatRefFilename("sample_trait_percentiles");
    public static final String REF_FILE_TRAIT_RATES = formatRefFilename("sample_trait_rates");
    public static final String REF_FILE_SIG_PERC = formatRefFilename("sig_percentiles");
    public static final String REF_FILE_SV_PERC = formatRefFilename("sv_percentiles");
    public static final String REF_FILE_GENE_EXP_CANCER = formatRefFilename("gene_exp_cancer");
    public static final String REF_FILE_GENE_EXP_SAMPLE = formatRefFilename("gene_exp_sample");
    public static final String REF_FILE_GENE_EXP_PERC = formatRefFilename("gene_exp_percentiles");
}
