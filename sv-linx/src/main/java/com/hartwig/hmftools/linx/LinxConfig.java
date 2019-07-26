package com.hartwig.hmftools.linx;

import static com.hartwig.hmftools.linx.analysis.SvClusteringMethods.DEFAULT_PROXIMITY_DISTANCE;
import static com.hartwig.hmftools.linx.types.SvaConstants.DEFAULT_CHAINING_SV_LIMIT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinxConfig
{
    final public int ProximityDistance;
    final public String OutputDataPath;
    final public String PurpleDataPath;
    final public String SvDataPath;
    final public boolean UploadToDB;
    final public String FragileSiteFile;
    final public String LineElementFile;
    final public String ReplicationOriginsFile;
    final public String ViralHostsFile;
    final public int MaxSamples;
    final public boolean WriteVisualisationData;
    final public int ChainingSvLimit; // for analysis and chaining

    public boolean LogVerbose;
    public String RequiredAnnotations;
    public int LogChainingMaxSize;

    private List<String> mSampleIds;

    // config options
    public static final String PURPLE_DATA_DIR = "purple_dir";
    public static final String DATA_OUTPUT_DIR = "output_dir";
    public static final String DATA_OUTPUT_PATH = "data_output_path"; // old config name support
    public static final String SV_DATA_DIR = "sv_data_dir";
    public static final String SAMPLE = "sample";
    public static final String GENE_TRANSCRIPTS_DIR = "gene_transcripts_dir";
    public static final String UPLOAD_TO_DB = "upload_to_db"; // true by default when in single-sample mode, false for batch

    // clustering analysis options
    private static final String CLUSTER_BASE_DISTANCE = "proximity_distance";
    private static final String CHAINING_SV_LIMIT = "chaining_sv_limit";
    private static final String REQUIRED_ANNOTATIONS = "annotations";

    // reference files
    private static final String FRAGILE_SITE_FILE = "fragile_site_file";
    private static final String LINE_ELEMENT_FILE = "line_element_file";
    private static final String VIRAL_HOSTS_FILE = "viral_hosts_file";
    private static final String REPLICATION_ORIGINS_FILE = "replication_origins_file";

    // logging options
    private static final String WRITE_VISUALISATION_DATA = "write_vis_data";
    public static final String LOG_DEBUG = "log_debug";
    private static final String LOG_VERBOSE = "log_verbose";
    private static final String LOG_CHAIN_MAX_SIZE = "log_chain_size";
    private static final String LOG_CLUSTER_ID = "log_cluster_id"; // for logging and breakends
    private static final String LOG_SV_ID = "log_sv_id";

    // for testing only
    private static final String MAX_SAMPLES = "max_samples";

    public static int SPECIFIC_CLUSTER_ID = -1;
    public static String SPECIFIC_SV_ID = "";

    private static final Logger LOGGER = LogManager.getLogger(LinxConfig.class);

    public LinxConfig(final CommandLine cmd)
    {
        String configSampleStr = cmd.getOptionValue(SAMPLE);

        mSampleIds = Lists.newArrayList();

        if(configSampleStr != null && !configSampleStr.equals("*"))
        {
            if (configSampleStr.contains(","))
            {
                String[] tumorList = configSampleStr.split(",");
                mSampleIds = Arrays.stream(tumorList).collect(Collectors.toList());
            }
            else if(configSampleStr.contains(".csv"))
            {
                mSampleIds = loadSampleListFile(configSampleStr);
            }
            else
            {
                // assume refers to a single sample
                mSampleIds.add(configSampleStr);
            }
        }

        if(cmd.hasOption(UPLOAD_TO_DB))
        {
            UploadToDB = Boolean.parseBoolean(cmd.getOptionValue(UPLOAD_TO_DB));
        }
        else
        {
            UploadToDB = mSampleIds.size() == 1;
        }

        PurpleDataPath = cmd.getOptionValue(PURPLE_DATA_DIR, "");

        String dataOutputDir = "";
        if(cmd.hasOption(DATA_OUTPUT_DIR))
            dataOutputDir = cmd.getOptionValue(DATA_OUTPUT_DIR);
        else if(cmd.hasOption(DATA_OUTPUT_PATH))
            dataOutputDir = cmd.getOptionValue(DATA_OUTPUT_PATH);

        OutputDataPath = formOutputPath(dataOutputDir);

        SvDataPath = cmd.hasOption(SV_DATA_DIR) ? cmd.getOptionValue(SV_DATA_DIR) : OutputDataPath;

        ProximityDistance = cmd.hasOption(CLUSTER_BASE_DISTANCE) ? Integer.parseInt(cmd.getOptionValue(CLUSTER_BASE_DISTANCE))
                : DEFAULT_PROXIMITY_DISTANCE;

        FragileSiteFile = cmd.getOptionValue(FRAGILE_SITE_FILE, "");
        LineElementFile = cmd.getOptionValue(LINE_ELEMENT_FILE, "");
        ViralHostsFile = cmd.getOptionValue(VIRAL_HOSTS_FILE, "");
        ReplicationOriginsFile = cmd.getOptionValue(REPLICATION_ORIGINS_FILE, "");
        RequiredAnnotations = cmd.getOptionValue(REQUIRED_ANNOTATIONS, "");
        MaxSamples = Integer.parseInt(cmd.getOptionValue(MAX_SAMPLES, "0"));

        LogChainingMaxSize = Integer.parseInt(cmd.getOptionValue(LOG_CHAIN_MAX_SIZE, "0"));

        LogVerbose = cmd.hasOption(LOG_VERBOSE);
        WriteVisualisationData = cmd.hasOption(WRITE_VISUALISATION_DATA);

        ChainingSvLimit = cmd.hasOption(CHAINING_SV_LIMIT) ? Integer.parseInt(cmd.getOptionValue(CHAINING_SV_LIMIT)) : DEFAULT_CHAINING_SV_LIMIT;

        SPECIFIC_CLUSTER_ID = Integer.parseInt(cmd.getOptionValue(LOG_CLUSTER_ID, "-1"));
        SPECIFIC_SV_ID = cmd.getOptionValue(LOG_SV_ID, "");
    }

    public static final String formOutputPath(final String dir)
    {
        return dir.endsWith(File.separator) ? dir : dir + File.separator;
    }

    public final List<String> getSampleIds() { return mSampleIds; }
    public void setSampleIds(final List<String> list) { mSampleIds.addAll(list); }
    public boolean hasMultipleSamples() { return mSampleIds.size() > 1; }
    public boolean isSingleSample() { return mSampleIds.size() == 1; }

    public LinxConfig(int proximityDistance)
    {
        ProximityDistance = proximityDistance;
        PurpleDataPath = "";
        OutputDataPath = "";
        SvDataPath = "";
        UploadToDB = false;
        FragileSiteFile = "";
        LineElementFile = "";
        ViralHostsFile = "";
        ReplicationOriginsFile = "";
        RequiredAnnotations = "";
        mSampleIds = Lists.newArrayList();
        MaxSamples = 0;
        LogVerbose = false;
        WriteVisualisationData = false;
        ChainingSvLimit = DEFAULT_CHAINING_SV_LIMIT;
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(PURPLE_DATA_DIR, true, "Sample purple data directory");
        options.addOption(DATA_OUTPUT_PATH, true, "Linx output directory");
        options.addOption(DATA_OUTPUT_DIR, true, "Linx output directory");
        options.addOption(SV_DATA_DIR, true, "Optional: directory for per-sample SV data, default is to use output_dir");
        options.addOption(SAMPLE, true, "Sample Id, or list separated by ';' or '*' for all in DB");
        options.addOption(UPLOAD_TO_DB, true, "Upload all LINX data to DB (true/false), single-sample default=true, batch-mode default=false");
        options.addOption(CLUSTER_BASE_DISTANCE, true, "Clustering base distance, defaults to 5000");
        options.addOption(LINE_ELEMENT_FILE, true, "Line Elements file");
        options.addOption(VIRAL_HOSTS_FILE, true, "Viral hosts file");
        options.addOption(FRAGILE_SITE_FILE, true, "Fragile Site file");
        options.addOption(REPLICATION_ORIGINS_FILE, true, "Origins of replication file");
        options.addOption(MAX_SAMPLES, true, "Limit to X samples for testing");
        options.addOption(WRITE_VISUALISATION_DATA, false, "Optional: write files for Circos");
        options.addOption(CHAINING_SV_LIMIT, true, "Optional: max cluster size for chaining");
        options.addOption(REQUIRED_ANNOTATIONS, true, "Optional: string list of annotations");
        options.addOption(LOG_DEBUG, false, "Sets log level to Debug, off by default");
        options.addOption(LOG_VERBOSE, false, "Log extra detail");
        options.addOption(LOG_CHAIN_MAX_SIZE, true, "Write file with chaining diagnostics for chains less than this (off by default)");
        options.addOption(LOG_CLUSTER_ID, true, "Optional: log specific cluster details");
        options.addOption(LOG_SV_ID, true, "Optional: log specific SV details");
    }

    private List<String> loadSampleListFile(final String filename)
    {
        List<String> sampleIds = Lists.newArrayList();

        try
        {
            BufferedReader fileReader = new BufferedReader(new FileReader(filename));

            String line = fileReader.readLine(); // skip header

            while ((line = fileReader.readLine()) != null)
            {
                String[] items = line.split(",");

                final String sampleId = items[0];
                sampleIds.add(sampleId);
            }

            LOGGER.info("Loaded {} specific sample IDs", sampleIds.size());

        }
        catch (IOException exception)
        {
            LOGGER.error("Failed to read sample list input CSV file({}): {}", filename, exception.toString());
        }

        return sampleIds;
    }

}
