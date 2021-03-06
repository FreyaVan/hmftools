package com.hartwig.hmftools.cup.somatics;

import static com.hartwig.hmftools.common.sigs.DataUtils.convertList;
import static com.hartwig.hmftools.common.sigs.Percentiles.PERCENTILE_COUNT;
import static com.hartwig.hmftools.common.sigs.Percentiles.buildPercentiles;
import static com.hartwig.hmftools.common.sigs.PositionFrequencies.DEFAULT_POS_FREQ_BUCKET_SIZE;
import static com.hartwig.hmftools.common.sigs.SnvSigUtils.SNV_TRINUCLEOTIDE_BUCKET_COUNT;
import static com.hartwig.hmftools.common.sigs.SnvSigUtils.populateBucketMap;
import static com.hartwig.hmftools.common.sigs.VectorUtils.sumVector;
import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.cup.CuppaConfig.CUP_LOGGER;
import static com.hartwig.hmftools.cup.CuppaConfig.DATA_DELIM;
import static com.hartwig.hmftools.cup.CuppaRefFiles.REF_FILE_CANCER_POS_FREQ_COUNTS;
import static com.hartwig.hmftools.cup.CuppaRefFiles.REF_FILE_SAMPLE_POS_FREQ_COUNTS;
import static com.hartwig.hmftools.cup.CuppaRefFiles.REF_FILE_SIG_PERC;
import static com.hartwig.hmftools.cup.CuppaRefFiles.REF_FILE_SNV_COUNTS;
import static com.hartwig.hmftools.cup.common.CategoryType.SNV;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_OTHER;
import static com.hartwig.hmftools.cup.common.CupConstants.POS_FREQ_BUCKET_SIZE;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.extractPositionFrequencyCounts;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.extractTrinucleotideCounts;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadRefSampleCounts;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadRefSigContributions;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadSomaticVariants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.sigs.PositionFrequencies;
import com.hartwig.hmftools.common.sigs.SigMatrix;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.cup.common.CategoryType;
import com.hartwig.hmftools.cup.common.SampleData;
import com.hartwig.hmftools.cup.common.SampleDataCache;
import com.hartwig.hmftools.cup.ref.RefDataConfig;
import com.hartwig.hmftools.cup.rna.RefClassifier;

public class RefSomatics implements RefClassifier
{
    private final RefDataConfig mConfig;
    private final SampleDataCache mSampleDataCache;

    private final Map<String,Map<String,List<Double>>> mCancerSigContribs;
    private final Map<String,List<Double>> mCancerSnvCounts;

    private SigMatrix mTriNucCounts; // counts per tri-nucleotide bucket
    private final Map<String,Integer> mTriNucCountsIndex;

    private SigMatrix mPosFreqCounts; // counts per genomic position
    private final Map<String,Integer> mPosFreqCountsIndex;

    private final PositionFrequencies mPositionFrequencies;

    private BufferedWriter mRefDataWriter;

    public static final String REF_SIG_TYPE_SNV_COUNT = "SnvCount";

    public static final Map<String,String> REPORTABLE_SIGS = Maps.newHashMap();

    public RefSomatics(final RefDataConfig config, final SampleDataCache sampleDataCache)
    {
        mConfig = config;
        mSampleDataCache = sampleDataCache;

        mCancerSigContribs = Maps.newHashMap();
        mCancerSnvCounts = Maps.newHashMap();

        mRefDataWriter = null;

        mTriNucCounts = null;
        mTriNucCountsIndex = Maps.newHashMap();

        mPosFreqCounts = null;
        mPosFreqCountsIndex = Maps.newHashMap();

        mPositionFrequencies = new PositionFrequencies(POS_FREQ_BUCKET_SIZE, DEFAULT_POS_FREQ_BUCKET_SIZE);

        populateReportableSignatures();
    }

    public CategoryType categoryType() { return SNV; }

    public static boolean requiresBuild(final RefDataConfig config)
    {
        return config.DbAccess != null || !config.RefSigContribsFile.isEmpty() && !config.RefSnvCountsFile.isEmpty();
    }

    public void buildRefDataSets()
    {
        CUP_LOGGER.info("building SNV and signatures reference data");

        mTriNucCounts = loadReferenceSnvCounts(mConfig.RefSnvCountsFile, mTriNucCountsIndex, "trinucleotide");
        mPosFreqCounts = loadReferenceSnvCounts(mConfig.RefSnvPositionDataFile, mPosFreqCountsIndex, "position frequency");

        retrieveMissingSampleCounts();

        buildSignaturePercentiles();
        buildSnvCountPercentiles();
        buildCancerPosFrequencies();

        closeBufferedWriter(mRefDataWriter);
    }

    private SigMatrix loadReferenceSnvCounts(final String refFilename, final Map<String,Integer> sampleCountsIndex, final String type)
    {
        if(refFilename.isEmpty())
            return null;

        CUP_LOGGER.debug("loading SNV {} reference data", type);

        // check if complete file has already been provided (eg if only other reference data is being built)
        SigMatrix refMatrix = null;

        final List<String> existingRefSampleIds = Lists.newArrayList();
        final SigMatrix existingRefSampleCounts = loadRefSampleCounts(refFilename, existingRefSampleIds);

        final List<String> refSampleIds = mSampleDataCache.refSampleIds(false);
        boolean hasMissingSamples = refSampleIds.stream().anyMatch(x -> !existingRefSampleIds.contains(x));

        if(!hasMissingSamples && existingRefSampleIds.size() == refSampleIds.size())
        {
            CUP_LOGGER.debug("using existing SNV {} reference counts", type);

            for(int i = 0; i < existingRefSampleIds.size(); ++i)
            {
                sampleCountsIndex.put(existingRefSampleIds.get(i), i);
            }

            return existingRefSampleCounts;
        }

        // take any existing counts
        if(existingRefSampleCounts != null)
        {
            existingRefSampleCounts.cacheTranspose();

            refMatrix = new SigMatrix(existingRefSampleCounts.Rows, refSampleIds.size());

            int refSampleIndex = 0;

            for(int i = 0; i < existingRefSampleIds.size(); ++i)
            {
                final String sampleId = existingRefSampleIds.get(i);

                if(!mSampleDataCache.hasRefSample(sampleId))
                    continue;

                refMatrix.setCol(refSampleIndex, existingRefSampleCounts.getCol(i));
                sampleCountsIndex.put(existingRefSampleIds.get(i), i);
                ++refSampleIndex;
            }
        }

        return refMatrix;
    }

    private void retrieveMissingSampleCounts()
    {
        final List<String> refSampleIds = mSampleDataCache.refSampleIds(false);

        long missingSamples = refSampleIds.stream()
                .filter(x -> !mTriNucCountsIndex.containsKey(x) || !mPosFreqCountsIndex.containsKey(x)).count();

        if(missingSamples == 0)
            return;

        int refSampleCount = refSampleIds.size();
        CUP_LOGGER.debug("retrieving SNV data for {} samples from refSampleCount({})", missingSamples, refSampleCount);

        if(mTriNucCounts == null)
        {
            mTriNucCounts = new SigMatrix(SNV_TRINUCLEOTIDE_BUCKET_COUNT, refSampleCount);
        }

        if(mPosFreqCounts == null)
        {
            mPosFreqCounts = new SigMatrix(mPositionFrequencies.getBucketCount(), refSampleCount);
        }

        final Map<String,Integer> triNucBucketNameMap = Maps.newHashMap();
        populateBucketMap(triNucBucketNameMap);

        int nextLog = 100;
        int retrievedSamples = 0;

        for(int i = 0; i < refSampleCount; ++i)
        {
            final String sampleId = refSampleIds.get(i);
            boolean needsTriNucCounts = !mTriNucCountsIndex.containsKey(sampleId);
            boolean needsPosFreqCounts = !mPosFreqCountsIndex.containsKey(sampleId);

            if(!needsPosFreqCounts && !needsTriNucCounts)
                continue;

            ++retrievedSamples;

            if(retrievedSamples >= nextLog)
            {
                nextLog += 100;
                CUP_LOGGER.debug("retrieved SNV data for {} samples", retrievedSamples);
            }

            final List<SomaticVariant> variants = loadSomaticVariants(sampleId, mConfig.DbAccess);

            if(needsTriNucCounts)
            {
                final double[] triNucCounts = extractTrinucleotideCounts(variants, triNucBucketNameMap);

                int refSampleIndex = mTriNucCountsIndex.size();
                mTriNucCounts.setCol(refSampleIndex, triNucCounts);
                mTriNucCountsIndex.put(sampleId, refSampleIndex);
            }

            if(needsPosFreqCounts)
            {
                extractPositionFrequencyCounts(variants, mPositionFrequencies);

                int refSampleIndex = mPosFreqCountsIndex.size();
                mPosFreqCounts.setCol(refSampleIndex, mPositionFrequencies.getCounts());
                mPosFreqCountsIndex.put(sampleId, refSampleIndex);
            }
        }

        mTriNucCounts.cacheTranspose();
        mPosFreqCounts.cacheTranspose();

        // write out sample matrix data
        writeSampleCounts(mTriNucCounts, mTriNucCountsIndex, REF_FILE_SNV_COUNTS);
        writeSampleCounts(mPosFreqCounts, mPosFreqCountsIndex, REF_FILE_SAMPLE_POS_FREQ_COUNTS);
    }

    private void writeSampleCounts(final SigMatrix matrix, final Map<String,Integer> sampleCountsIndex, final String filename)
    {
        try
        {
            BufferedWriter writer = createBufferedWriter(mConfig.OutputDir + filename, false);

            final List<String> sampleIds = sampleCountsIndex.keySet().stream().collect(Collectors.toList());
            writer.write(sampleIds.get(0));
            for(int i = 1; i < sampleIds.size(); ++i)
            {
                writer.write(String.format(",%s", sampleIds.get(i)));
            }

            writer.newLine();

            final double[][] matrixData = matrix.getData();

            for(int b = 0; b < matrix.Rows; ++b)
            {
                writer.write(String.format("%.0f", matrixData[b][sampleCountsIndex.get(sampleIds.get(0))]));

                for(int i = 1; i < sampleIds.size(); ++i)
                {
                    int index = sampleCountsIndex.get(sampleIds.get(i));

                    if(index >= matrix.Cols)
                    {
                        CUP_LOGGER.error("file({}) invalid col({})", filename, i);
                        return;
                    }

                    writer.write(String.format(",%.0f", matrixData[b][sampleCountsIndex.get(sampleIds.get(i))]));
                }

                writer.newLine();
            }

            writer.close();
        }
        catch(IOException e)
        {
            CUP_LOGGER.error("failed to write ref sample SNV counts: {}", e.toString());
        }
    }

    private void buildSignaturePercentiles()
    {
        CUP_LOGGER.debug("building signature allocation reference data");

        initialiseRefDataWriter();

        final Map<String,Map<String,Double>> sampleSigContributions = Maps.newHashMap();

        if(!mConfig.RefSigContribsFile.isEmpty())
        {
            loadRefSigContributions(mConfig.RefSigContribsFile, sampleSigContributions);
        }
        else if(mConfig.DbAccess != null)
        {
            SomaticDataLoader.loadSigContribsFromDatabase(
                    mConfig.DbAccess, mSampleDataCache.refSampleIds(true), sampleSigContributions);
        }

        for(Map.Entry<String,Map<String,Double>> entry : sampleSigContributions.entrySet())
        {
            final String sampleId = entry.getKey();
            final Map<String,Double> sigAllocations = entry.getValue();

            final String cancerType = mSampleDataCache.RefSampleCancerTypeMap.get(sampleId);

            if(cancerType == null)
            {
                // expected if a smaller ref sample set is being run
                // CUP_LOGGER.debug("sample({}) signatures missing cancer type", sampleId);
                continue;
            }

            Map<String,List<Double>> sigDataMap = mCancerSigContribs.get(cancerType);

            if(sigDataMap == null)
            {
                sigDataMap = Maps.newHashMap();
                mCancerSigContribs.put(cancerType, sigDataMap);
            }

            for(Map.Entry<String,Double> sigAllocEntry : sigAllocations.entrySet())
            {
                final String sigName = sigAllocEntry.getKey();

                if(!REPORTABLE_SIGS.containsKey(sigName))
                    continue;

                double sigContrib = sigAllocEntry.getValue();

                List<Double> sigContribs = sigDataMap.get(sigName);

                if(sigContribs == null)
                {
                    sigDataMap.put(sigName, Lists.newArrayList(sigContrib));
                    continue;
                }

                // add in ascending order
                int index = 0;
                while(index < sigContribs.size())
                {
                    if(sigContrib < sigContribs.get(index))
                        break;

                    ++index;
                }

                sigContribs.add(index, sigContrib);
            }
        }

        for(Map.Entry<String,Map<String,List<Double>>> entry : mCancerSigContribs.entrySet())
        {
            final String cancerType = entry.getKey();

            if(!mSampleDataCache.hasRefCancerType(cancerType))
                continue;

            for(Map.Entry<String,List<Double>> sigEntry : entry.getValue().entrySet())
            {
                final String sigName = sigEntry.getKey();
                final double[] percentiles = buildPercentiles(convertList(sigEntry.getValue()));
                writeRefSigData(cancerType, sigName, percentiles);
            }
        }
    }

    private void buildSnvCountPercentiles()
    {
        for(Map.Entry<String,Integer> entry : mTriNucCountsIndex.entrySet())
        {
            final String sampleId = entry.getKey();
            double sampleTotal = sumVector(mTriNucCounts.getCol(entry.getValue()));

            final String cancerType = mSampleDataCache.RefSampleCancerTypeMap.get(sampleId);

            if(cancerType == null)
            {
                // not a ref sample even though in the counts file
                CUP_LOGGER.debug("sample({}) SNV missing cancer type", sampleId);
                continue;
            }

            List<Double> sampleCounts = mCancerSnvCounts.get(cancerType);
            if(sampleCounts == null)
            {
                mCancerSnvCounts.put(cancerType, Lists.newArrayList(sampleTotal));
            }
            else
            {
                int index = 0;
                while(index < sampleCounts.size())
                {
                    if(sampleTotal < sampleCounts.get(index))
                        break;

                    ++index;
                }

                sampleCounts.add(index, sampleTotal);
            }
        }

        for(Map.Entry<String,List<Double>> entry : mCancerSnvCounts.entrySet())
        {
            final String cancerType = entry.getKey();

            final double[] percentiles = buildPercentiles(convertList(entry.getValue()));
            writeRefSnvCountData(cancerType, percentiles);
        }
    }
    
    private void initialiseRefDataWriter()
    {
        try
        {
            final String filename = mConfig.OutputDir + REF_FILE_SIG_PERC;
            mRefDataWriter = createBufferedWriter(filename, false);

            mRefDataWriter.write("CancerType,DataType");

            for(int i = 0; i < PERCENTILE_COUNT; ++i)
            {
                mRefDataWriter.write(String.format(",Pct_%.2f", i * 0.01));
            }

            mRefDataWriter.newLine();
        }
        catch(IOException e)
        {
            CUP_LOGGER.error("failed to write signatures ref data output: {}", e.toString());
        }
    }

    private void writeRefSigData(final String cancerType, final String sigName, final double[] percentileValues)
    {
        try
        {
            mRefDataWriter.write(String.format("%s,%s", cancerType, sigName));

            for(int i = 0; i < percentileValues.length; ++i)
            {
                mRefDataWriter.write(String.format(",%.6f", percentileValues[i]));
            }

            mRefDataWriter.newLine();
        }
        catch(IOException e)
        {
            CUP_LOGGER.error("failed to write signatures ref data output: {}", e.toString());
        }
    }

    private void writeRefSnvCountData(final String cancerType, final double[] percentileValues)
    {
        try
        {
            mRefDataWriter.write(String.format("%s,%s", cancerType, REF_SIG_TYPE_SNV_COUNT));

            for(int i = 0; i < percentileValues.length; ++i)
            {
                mRefDataWriter.write(String.format(",%.0f", percentileValues[i]));
            }

            mRefDataWriter.newLine();
        }
        catch(IOException e)
        {
            CUP_LOGGER.error("failed to write signatures ref data output: {}", e.toString());
        }
    }

    public static boolean populateRefPercentileData(
            final String filename, final Map<String,Map<String,double[]>> cancerSigContribs, final Map<String,double[]> cancerSnvCounts)
    {
        try
        {
            final List<String> fileData = Files.readAllLines(new File(filename).toPath());

            final String header = fileData.get(0);
            fileData.remove(0);

            for(final String line : fileData)
            {
                // SampleId,DataType,Pct_0.00 etc
                final String[] items = line.split(DATA_DELIM, -1);
                String cancerType = items[0];

                String dataType = items[1];

                double[] percentileData = new double[PERCENTILE_COUNT];

                int startIndex = 2;

                for(int i = startIndex; i < items.length; ++i)
                {
                    double value = Double.parseDouble(items[i]);
                    percentileData[i - startIndex] = value;
                }

                if(dataType.equals(REF_SIG_TYPE_SNV_COUNT))
                {
                    cancerSnvCounts.put(cancerType, percentileData);
                }
                else
                {
                    String sigName = dataType;

                    Map<String, double[]> sigContribsMap = cancerSigContribs.get(cancerType);

                    if(sigContribsMap == null)
                    {
                        sigContribsMap = Maps.newHashMap();
                        cancerSigContribs.put(cancerType, sigContribsMap);
                    }

                    sigContribsMap.put(sigName, percentileData);
                }
            }
        }
        catch (IOException e)
        {
            CUP_LOGGER.error("failed to read sig contrib percentile data file({}): {}", filename, e.toString());
            return false;
        }

        return true;
    }

    private void buildCancerPosFrequencies()
    {
        if(mPosFreqCounts == null)
            return;

        try
        {
            final String filename = mConfig.OutputDir + REF_FILE_CANCER_POS_FREQ_COUNTS;
            BufferedWriter writer = createBufferedWriter(filename, false);

            int maxSampleCount = mPositionFrequencies.getMaxSampleCount();
            int bucketCount = mPositionFrequencies.getBucketCount();

            final Map<String,double[]> cancerPosCounts = Maps.newHashMap();

            for(Map.Entry<String,List<SampleData>> entry : mSampleDataCache.RefCancerSampleData.entrySet())
            {
                final String cancerType = entry.getKey();

                if(cancerType.equals(CANCER_TYPE_OTHER))
                    continue;

                final double[] cancerCounts = new double[bucketCount];

                for(final SampleData sample : entry.getValue())
                {
                    final double[] sampleCounts = mPosFreqCounts.getCol(mPosFreqCountsIndex.get(sample.Id));

                    if(sampleCounts == null)
                        continue;

                    double sampleTotal = sumVector(sampleCounts);

                    double reductionFactor = sampleTotal > maxSampleCount ? maxSampleCount / sampleTotal : 1.0;

                    for(int b = 0; b < sampleCounts.length; ++b)
                    {
                        cancerCounts[b] += reductionFactor * sampleCounts[b];
                    }
                }

                cancerPosCounts.put(cancerType, cancerCounts);
            }

            final List<String> cancerTypes = cancerPosCounts.keySet().stream().collect(Collectors.toList());
            writer.write(cancerTypes.get(0));
            for(int i = 1; i < cancerTypes.size(); ++i)
            {
                writer.write(String.format(",%s", cancerTypes.get(i)));
            }

            writer.newLine();

            for(int b = 0; b < bucketCount; ++b)
            {
                writer.write(String.format("%.1f", cancerPosCounts.get(cancerTypes.get(0))[b]));

                for(int i = 1; i < cancerTypes.size(); ++i)
                {
                    writer.write(String.format(",%.1f", cancerPosCounts.get(cancerTypes.get(i))[b]));
                }

                writer.newLine();
            }

            writer.close();
        }
        catch(IOException e)
        {
            CUP_LOGGER.error("failed to write sample pos data output: {}", e.toString());
        }
    }

    public static void populateReportableSignatures()
    {
        REPORTABLE_SIGS.clear();
        REPORTABLE_SIGS.put("Sig1", "SIG_1");
        REPORTABLE_SIGS.put("Sig2", "SIG_2_13_AID_APOBEC");
        REPORTABLE_SIGS.put("Sig4", "SIG_4_SMOKING");
        REPORTABLE_SIGS.put("Sig6", "SIG_6_MMR");
        REPORTABLE_SIGS.put("Sig7", "SIG_7_UV");
        REPORTABLE_SIGS.put("Sig10", "SIG_10_POLE");
        REPORTABLE_SIGS.put("Sig11", "SIG_11");
        REPORTABLE_SIGS.put("Sig17", "SIG_17");
    }

}
