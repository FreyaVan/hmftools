package com.hartwig.hmftools.isofox.cohort;

import static com.hartwig.hmftools.isofox.IsofoxConfig.DATA_OUTPUT_DIR;
import static com.hartwig.hmftools.isofox.IsofoxConfig.EXCLUDED_GENE_ID_FILE;
import static com.hartwig.hmftools.isofox.IsofoxConfig.GENE_ID_FILE;
import static com.hartwig.hmftools.isofox.IsofoxConfig.GENE_TRANSCRIPTS_DIR;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.IsofoxConfig.LOG_DEBUG;
import static com.hartwig.hmftools.isofox.IsofoxConfig.OUTPUT_ID;
import static com.hartwig.hmftools.isofox.IsofoxConfig.loadGeneIdsFile;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.EXTERNAL_EXPRESSION_COMPARE;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.FUSION;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.GENE_DISTRIBUTION;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.GENE_EXPRESSION_COMPARE;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.GENE_EXPRESSION_MATRIX;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.SAMPLE_GENE_PERCENTILES;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.TRANSCRIPT_DISTRIBUTION;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.TRANSCRIPT_EXPRESSION_MATRIX;
import static com.hartwig.hmftools.isofox.cohort.CohortAnalysisType.getFileId;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.ISOFOX_ID;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.ITEM_DELIM;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.isofox.expression.cohort.ExpressionCohortConfig;
import com.hartwig.hmftools.isofox.fusion.cohort.FusionCohortConfig;
import com.hartwig.hmftools.isofox.novel.cohort.AltSpliceJunctionCohort;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class CohortConfig
{
    public static final String ROOT_DATA_DIRECTORY = "root_data_dir";
    public static final String SAMPLE_DATA_FILE = "sample_data_file";
    public static final String USE_SAMPLE_DIRS = "use_sample_dir";
    public static final String ALL_AVAILABLE_FILES = "all_available_files";
    public static final String LOAD_TYPES = "load_types";
    public static final String FAIL_MISSING = "fail_on_missing_file";

    public static final String SAMPLE_MUT_FILE = "sample_mut_file";
    private static final String THREADS = "threads";

    public final String RootDataDir;
    public final String OutputDir;
    public final String OutputIdentifier;
    public final SampleDataCache SampleData;
    public final boolean UseSampleDirectories;
    public final boolean AllAvailableFiles;
    public final List<String> RestrictedGeneIds;
    public final List<String> ExcludedGeneIds;
    public final boolean FailOnMissingSample;
    public final boolean ConvertUnmatchedCancerToOther;

    public final List<CohortAnalysisType> LoadTypes;

    public final String EnsemblDataCache;

    public final String SampleMutationsFile;

    public final FusionCohortConfig Fusions;

    public final ExpressionCohortConfig Expression;

    public final int Threads;

    public CohortConfig(final CommandLine cmd)
    {
        String rootDir = cmd.getOptionValue(ROOT_DATA_DIRECTORY);

        if(!rootDir.endsWith(File.separator))
            rootDir += File.separator;

        RootDataDir = rootDir;

        UseSampleDirectories = cmd.hasOption(USE_SAMPLE_DIRS);
        AllAvailableFiles = !UseSampleDirectories && cmd.hasOption(ALL_AVAILABLE_FILES);
        FailOnMissingSample = cmd.hasOption(FAIL_MISSING);

        String outputdir = cmd.getOptionValue(DATA_OUTPUT_DIR);
        if(!outputdir.endsWith(File.separator))
            outputdir += File.separator;
        OutputDir = outputdir;
        OutputIdentifier = cmd.getOptionValue(OUTPUT_ID);

        final String sampleDataFile = cmd.getOptionValue(SAMPLE_DATA_FILE);

        SampleData = new SampleDataCache(sampleDataFile);

        if(!SampleData.isValid())
        {
            ISF_LOGGER.warn("invalid sample data file({})", sampleDataFile);
        }

        LoadTypes = Arrays.stream(cmd.getOptionValue(LOAD_TYPES).split(ITEM_DELIM))
                .map(x -> CohortAnalysisType.valueOf(x)).collect(Collectors.toList());

        RestrictedGeneIds = Lists.newArrayList();
        ExcludedGeneIds = Lists.newArrayList();

        if(cmd.hasOption(GENE_ID_FILE))
        {
            final String inputFile = cmd.getOptionValue(GENE_ID_FILE);
            loadGeneIdsFile(inputFile, RestrictedGeneIds);
            ISF_LOGGER.info("file({}) loaded {} restricted genes", inputFile, RestrictedGeneIds.size());
        }

        if(cmd.hasOption(EXCLUDED_GENE_ID_FILE))
        {
            final String inputFile = cmd.getOptionValue(EXCLUDED_GENE_ID_FILE);
            loadGeneIdsFile(inputFile, ExcludedGeneIds);
            ISF_LOGGER.info("file({}) loaded {} excluded genes", inputFile, ExcludedGeneIds.size());
        }

        EnsemblDataCache = cmd.getOptionValue(GENE_TRANSCRIPTS_DIR);

        ConvertUnmatchedCancerToOther = true;

        SampleMutationsFile = cmd.getOptionValue(SAMPLE_MUT_FILE);

        Fusions = LoadTypes.contains(FUSION) ? new FusionCohortConfig(cmd) : null;

        Expression = requiresExpressionConfig(LoadTypes) ? new ExpressionCohortConfig(cmd) : null;

        Threads = Integer.parseInt(cmd.getOptionValue(THREADS, "0"));
    }

    private static boolean requiresExpressionConfig(final List<CohortAnalysisType> loadTypes)
    {
        return loadTypes.contains(GENE_DISTRIBUTION) || loadTypes.contains(TRANSCRIPT_DISTRIBUTION)
                || loadTypes.contains(SAMPLE_GENE_PERCENTILES) || loadTypes.contains(GENE_EXPRESSION_COMPARE)
                || loadTypes.contains(EXTERNAL_EXPRESSION_COMPARE) || loadTypes.contains(GENE_EXPRESSION_MATRIX)
                || loadTypes.contains(TRANSCRIPT_EXPRESSION_MATRIX);
    }

    public static boolean isValid(final CommandLine cmd)
    {
        if(!cmd.hasOption(ROOT_DATA_DIRECTORY) || !cmd.hasOption(DATA_OUTPUT_DIR) || !cmd.hasOption(SAMPLE_DATA_FILE)
        || !cmd.hasOption(LOAD_TYPES))
        {
            return false;
        }

        return true;
    }

    public String formCohortFilename(final String fileId)
    {
        if(OutputIdentifier != null)
            return OutputDir + "isofox_" + OutputIdentifier + "." + fileId;
        else
            return OutputDir + "isofox_" + fileId;
    }

    public static String formSampleFilename(final CohortConfig config, final String sampleId, final CohortAnalysisType dataType)
    {
        String filename = config.RootDataDir;

        if(config.UseSampleDirectories)
            filename += File.separator + sampleId + File.separator;

        filename += sampleId + ISOFOX_ID;
        filename += getFileId(dataType);
        return filename;
    }

    public static boolean formSampleFilenames(final CohortConfig config, final CohortAnalysisType dataType, final List<Path> filenames)
    {
        List<String> missingSampleIds = Lists.newArrayList();

        for(final String sampleId : config.SampleData.SampleIds)
        {
            String filename = formSampleFilename(config, sampleId, dataType);

            final Path path = Paths.get(filename);

            if (!Files.exists(path))
            {
                if(config.FailOnMissingSample)
                {
                    ISF_LOGGER.error("sampleId({}) file({}) not found", sampleId, filename);
                    filenames.clear();
                    return false;
                }
                else
                {
                    ISF_LOGGER.info("sampleId({}) file({}) not found, skipping", sampleId, filename);
                    missingSampleIds.add(sampleId);
                    continue;
                }
            }

            filenames.add(path);
        }

        config.SampleData.removeMissingSamples(missingSampleIds);

        return true;
    }

    public static Options createCmdLineOptions()
    {
        final Options options = new Options();
        options.addOption(ROOT_DATA_DIRECTORY, true, "Root data directory for input files or sample directories");
        options.addOption(SAMPLE_DATA_FILE, true, "File with list of samples and cancer types to load data for");
        options.addOption(DATA_OUTPUT_DIR, true, "Output directory");
        options.addOption(USE_SAMPLE_DIRS, false, "File with list of samples to load data for");
        options.addOption(FAIL_MISSING, false, "Exit if sample input file isn't found");
        options.addOption(ALL_AVAILABLE_FILES, false, "Load all files in root directory matching expeted Isofox file names");
        options.addOption(LOAD_TYPES, true, "List of data types to load & process");
        options.addOption(GENE_TRANSCRIPTS_DIR, true, "Path to Ensembl data cache");
        options.addOption(GENE_ID_FILE, true, "Optional CSV file of genes to analyse");
        options.addOption(EXCLUDED_GENE_ID_FILE, true, "Optional CSV file of genes to ignore");
        options.addOption(OUTPUT_ID, true, "Optionally add identifier to output files");


        options.addOption(SAMPLE_MUT_FILE, true, "Sample mutations by gene and cancer type");

        AltSpliceJunctionCohort.addCmdLineOptions(options);
        FusionCohortConfig.addCmdLineOptions(options);
        ExpressionCohortConfig.addCmdLineOptions(options);

        options.addOption(LOG_DEBUG, false, "Log verbose");
        options.addOption(THREADS, true, "Number of threads for task execution, default is 0 (off)");

        return options;
    }
}
