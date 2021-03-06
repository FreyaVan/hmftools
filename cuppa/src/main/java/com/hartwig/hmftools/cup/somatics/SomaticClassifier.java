package com.hartwig.hmftools.cup.somatics;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import static com.hartwig.hmftools.common.sigs.CosineSimilarity.calcCosineSim;
import static com.hartwig.hmftools.common.sigs.Percentiles.getPercentile;
import static com.hartwig.hmftools.common.sigs.VectorUtils.sumVector;
import static com.hartwig.hmftools.cup.CuppaConfig.CUP_LOGGER;
import static com.hartwig.hmftools.cup.common.CategoryType.CLASSIFIER;
import static com.hartwig.hmftools.cup.common.CategoryType.SAMPLE_TRAIT;
import static com.hartwig.hmftools.cup.common.CategoryType.SNV;
import static com.hartwig.hmftools.cup.common.ClassifierType.SNV_96_PAIRWISE_SIMILARITY;
import static com.hartwig.hmftools.cup.common.ClassifierType.GENOMIC_POSITION_SIMILARITY;
import static com.hartwig.hmftools.cup.common.CupCalcs.calcPercentilePrevalence;
import static com.hartwig.hmftools.cup.common.CupConstants.CSS_SIMILARITY_CUTOFF;
import static com.hartwig.hmftools.cup.common.CupConstants.CSS_SIMILARITY_MAX_MATCHES;
import static com.hartwig.hmftools.cup.common.CupConstants.SNV_CSS_DIFF_EXPONENT;
import static com.hartwig.hmftools.cup.common.CupConstants.SNV_CSS_THRESHOLD;
import static com.hartwig.hmftools.cup.common.CupConstants.SNV_POS_FREQ_CSS_THRESHOLD;
import static com.hartwig.hmftools.cup.common.CupConstants.SNV_POS_FREQ_DIFF_EXPONENT;
import static com.hartwig.hmftools.cup.common.ResultType.LIKELIHOOD;
import static com.hartwig.hmftools.cup.common.ResultType.PERCENTILE;
import static com.hartwig.hmftools.cup.common.SampleData.isKnownCancerType;
import static com.hartwig.hmftools.cup.common.SampleResult.checkIsValidCancerType;
import static com.hartwig.hmftools.cup.common.SampleSimilarity.recordCssSimilarity;
import static com.hartwig.hmftools.cup.somatics.RefSomatics.populateReportableSignatures;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadRefSampleCounts;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadRefSignaturePercentileData;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadSampleCountsFromFile;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadSamplePosFreqFromFile;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadSigContribsFromCohortFile;
import static com.hartwig.hmftools.cup.somatics.SomaticDataLoader.loadSigContribsFromDatabase;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.sigs.SigMatrix;
import com.hartwig.hmftools.common.sigs.SignatureAllocation;
import com.hartwig.hmftools.common.sigs.SignatureAllocationFile;
import com.hartwig.hmftools.cup.CuppaConfig;
import com.hartwig.hmftools.cup.common.CategoryType;
import com.hartwig.hmftools.cup.common.CuppaClassifier;
import com.hartwig.hmftools.cup.common.SampleData;
import com.hartwig.hmftools.cup.common.SampleDataCache;
import com.hartwig.hmftools.cup.common.SampleResult;
import com.hartwig.hmftools.cup.common.SampleSimilarity;

public class SomaticClassifier implements CuppaClassifier
{
    private final CuppaConfig mConfig;
    private final SampleDataCache mSampleDataCache;

    private SigMatrix mRefSampleCounts;
    private final List<String> mRefSampleNames;
    private final Map<String,Map<String,double[]>> mRefCancerSigContribPercentiles;
    private final Map<String,double[]> mRefCancerSnvCountPercentiles;

    private SigMatrix mRefCancerSnvPosFrequencies;
    private final List<String> mRefSnvPosFreqCancerTypes;

    private SigMatrix mRefSampleSnvPosFrequencies;
    private final Map<String,Integer> mRefSamplePosFreqIndex;

    private SigMatrix mSampleCounts;
    private final Map<String,Integer> mSampleCountsIndex;

    private final Map<String,Map<String,Double>> mSampleSigContributions;

    private SigMatrix mSamplePosFrequencies;
    private final Map<String,Integer> mSamplePosFreqIndex;

    private boolean mIsValid;

    private static final int SNV_POS_FREQ_SNV_TOTAL_THRESHOLD = 20000;

    public SomaticClassifier(final CuppaConfig config, final SampleDataCache sampleDataCache)
    {
        mConfig = config;
        mSampleDataCache = sampleDataCache;

        mSampleCounts = null;
        mRefCancerSnvPosFrequencies = null;
        mRefSampleSnvPosFrequencies = null;
        mSampleSigContributions = Maps.newHashMap();
        mSampleCountsIndex = Maps.newHashMap();
        mSamplePosFreqIndex = Maps.newHashMap();

        mRefSampleCounts = null;
        mRefSampleNames = Lists.newArrayList();
        mRefCancerSigContribPercentiles = Maps.newHashMap();
        mRefCancerSnvCountPercentiles = Maps.newHashMap();
        mRefSnvPosFreqCancerTypes = Lists.newArrayList();
        mRefSamplePosFreqIndex = Maps.newHashMap();

        mIsValid = true;

        if(mConfig.RefSnvCountsFile.isEmpty() && mConfig.RefSigContributionFile.isEmpty() && mConfig.RefSnvCancerPosFreqFile.isEmpty())
            return;

        populateReportableSignatures();

        loadRefSignaturePercentileData(mConfig.RefSigContributionFile, mRefCancerSigContribPercentiles, mRefCancerSnvCountPercentiles);
        mRefSampleCounts = loadRefSampleCounts(mConfig.RefSnvCountsFile, mRefSampleNames);

        mRefCancerSnvPosFrequencies = loadRefSampleCounts(mConfig.RefSnvCancerPosFreqFile, mRefSnvPosFreqCancerTypes);
        mRefSampleSnvPosFrequencies = loadSamplePosFreqFromFile(mConfig.RefSnvSamplePosFreqFile, mRefSamplePosFreqIndex);

        if(mRefSampleCounts == null || mRefCancerSnvPosFrequencies == null)
            mIsValid = false;

        mIsValid &= loadSampleCounts();
        mIsValid &= loadSigContributions();
    }

    public CategoryType categoryType() { return SNV; }
    public boolean isValid() { return mIsValid; }

    private boolean loadSampleCounts()
    {
        if(!mConfig.SampleSnvCountsFile.isEmpty() && !mConfig.SampleSnvPosFreqFile.isEmpty())
        {
            mSampleCounts = loadSampleCountsFromFile(mConfig.SampleSnvCountsFile, mSampleCountsIndex);

            if(mConfig.SampleSnvPosFreqFile.equals(mConfig.RefSnvSamplePosFreqFile))
            {
                mSamplePosFrequencies = mRefSampleSnvPosFrequencies;
                mSamplePosFreqIndex.putAll(mRefSamplePosFreqIndex);
            }
            else
            {
                mSamplePosFrequencies = loadSamplePosFreqFromFile(mConfig.SampleSnvPosFreqFile, mSamplePosFreqIndex);
            }

            return mSampleCounts != null && mSamplePosFrequencies != null;
        }

        if(mSampleDataCache.isSingleSample())
        {
            final String sampleId = mSampleDataCache.SampleIds.get(0);

            /*
            if(!mConfig.SampleSomaticVcf.isEmpty())
            {
                final List<SomaticVariant> somaticVariants = loadSomaticVariants(sampleId, mConfig.SampleSomaticVcf);
                populateSomaticCounts(somaticVariants);
            }
            */

            final String snvCountsFile = !mConfig.SampleSnvCountsFile.isEmpty() ?
                    mConfig.SampleSnvCountsFile : mConfig.SampleDataDir + sampleId + ".sig.snv_counts.csv";

            final String snvPosFreqFile = !mConfig.SampleSnvPosFreqFile.isEmpty() ?
                    mConfig.SampleSnvPosFreqFile : mConfig.SampleDataDir + sampleId + ".sig.pos_freq_counts.csv";

            mSampleCounts = loadSampleCountsFromFile(snvCountsFile, mSampleCountsIndex);
            mSamplePosFrequencies = loadSamplePosFreqFromFile(snvPosFreqFile, mSamplePosFreqIndex);

            return mSampleCounts != null && mSamplePosFrequencies != null;
        }
        else if(mConfig.DbAccess != null)
        {
            CUP_LOGGER.error("somatic variants from DB not supported");

            /*
            final String sampleId = mSampleDataCache.SampleIds.get(0);
            final List<SomaticVariant> somaticVariants = loadSomaticVariants(sampleId, mConfig.DbAccess);
            populateSomaticCounts(somaticVariants);

            return mSampleCounts != null && mSamplePosFrequencies != null;
            */

            return false;
        }
        else
        {
            CUP_LOGGER.error("no sample SNV count source specified");
            return false;
        }
    }

    private boolean loadSigContributions()
    {
        if(!mConfig.SampleSigContribFile.isEmpty())
        {
            return loadSigContribsFromCohortFile(mConfig.SampleSigContribFile, mSampleSigContributions);
        }
        else if(mConfig.DbAccess != null)
        {
            return loadSigContribsFromDatabase(mConfig.DbAccess, mSampleDataCache.SampleIds, mSampleSigContributions);
        }

        final String sampleId = mSampleDataCache.SampleIds.get(0);
        final String sigAllocFile = SignatureAllocationFile.generateFilename(mConfig.SampleDataDir, sampleId);

        try
        {
            final List<SignatureAllocation> sigAllocations = SignatureAllocationFile.read(sigAllocFile);
            Map<String,Double> sigContribs = Maps.newHashMap();
            for(final SignatureAllocation sigAllocation : sigAllocations)
            {
                sigContribs.put(sigAllocation.signature(), sigAllocation.allocation());
            }

            mSampleSigContributions.put(sampleId, sigContribs);
        }
        catch(Exception e)
        {
            CUP_LOGGER.error("sample({}) failed to load sig allocations file({}): {}",
                    sampleId, sigAllocFile, e.toString());
            return false;
        }

        return true;
    }

    public void processSample(final SampleData sample, final List<SampleResult> results, final List<SampleSimilarity> similarities)
    {
        if(!mIsValid || mRefSampleCounts == null)
            return;

        Integer sampleCountsIndex = mSampleCountsIndex.get(sample.Id);

        if(sampleCountsIndex == null)
        {
            CUP_LOGGER.info("sample({}) has no SNV data", sample.Id);
            return;
        }

        final double[] sampleCounts = mSampleCounts.getCol(sampleCountsIndex);
        int snvTotal = (int)sumVector(sampleCounts);

        addCssResults(sample, sampleCounts, snvTotal, results, similarities);
        addPosFreqCssResults(sample, results, similarities);

        addSigContributionResults(sample, results);

        // add a percentile result

        final Map<String, Double> cancerTypeValues = Maps.newHashMap();

        for(Map.Entry<String, double[]> cancerPercentiles : mRefCancerSnvCountPercentiles.entrySet())
        {
            final String cancerType = cancerPercentiles.getKey();
            double percentile = getPercentile(cancerPercentiles.getValue(), snvTotal, true);
            cancerTypeValues.put(cancerType, percentile);
        }

        SampleResult result = new SampleResult(
                sample.Id, SAMPLE_TRAIT, PERCENTILE, "SNV_COUNT", snvTotal, cancerTypeValues);

        results.add(result);

        int cancerTypeCount = mSampleDataCache.RefCancerSampleData.size();
        int cancerSampleCount = sample.isRefSample() ? mSampleDataCache.getCancerSampleCount(sample.CancerType) : 0;

        final Map<String,Double> cancerPrevsLow = calcPercentilePrevalence(
                sample, cancerSampleCount, cancerTypeCount, mRefCancerSnvCountPercentiles, snvTotal,  true);

        results.add(new SampleResult(sample.Id, SNV, LIKELIHOOD, "SNV_COUNT_LOW", snvTotal, cancerPrevsLow));

        final Map<String,Double> cancerPrevsHigh = calcPercentilePrevalence(
                sample, cancerSampleCount, cancerTypeCount, mRefCancerSnvCountPercentiles, snvTotal, false);

        results.add(new SampleResult(sample.Id, SNV, LIKELIHOOD, "SNV_COUNT_HIGH", snvTotal, cancerPrevsHigh));
    }

    private void addCssResults(
            final SampleData sample, final double[] sampleCounts, int snvTotal,
            final List<SampleResult> results, final List<SampleSimilarity> similarities)
    {
        int refSampleCount = mRefSampleCounts.Cols;

        final List<SampleSimilarity> topMatches = Lists.newArrayList();

        final Map<String,Double> cancerCssTotals = Maps.newHashMap();

        for(int s = 0; s < refSampleCount; ++s)
        {
            final String refSampleId = mRefSampleNames.get(s);

            if(!mSampleDataCache.hasRefSample(refSampleId))
                continue;

            if(refSampleId.equals(sample.Id))
                continue;

            final String refCancerType = mSampleDataCache.RefSampleCancerTypeMap.get(refSampleId);

            if(!checkIsValidCancerType(sample, refCancerType, cancerCssTotals))
                continue;

            final double[] otherSampleCounts = mRefSampleCounts.getCol(s);

            double css = calcCosineSim(sampleCounts, otherSampleCounts);

            if(css < SNV_CSS_THRESHOLD)
                continue;

            if(mConfig.WriteSimilarities)
            {
                recordCssSimilarity(
                        topMatches, sample.Id, refSampleId, css, SNV_96_PAIRWISE_SIMILARITY.toString(),
                        CSS_SIMILARITY_MAX_MATCHES, CSS_SIMILARITY_CUTOFF);
            }

            if(!isKnownCancerType(refCancerType))
                continue;

            double cssWeight = pow(SNV_CSS_DIFF_EXPONENT, -100 * (1 - css));

            double otherSnvTotal = sumVector(otherSampleCounts);
            double mutLoadWeight = min(otherSnvTotal, snvTotal) / max(otherSnvTotal, snvTotal);

            int cancerTypeCount = mSampleDataCache.getCancerSampleCount(refCancerType);
            double weightedCss = css * cssWeight * mutLoadWeight / sqrt(cancerTypeCount);

            Double total = cancerCssTotals.get(refCancerType);

            if(total == null)
                cancerCssTotals.put(refCancerType, weightedCss);
            else
                cancerCssTotals.put(refCancerType, total + weightedCss);
        }

        double totalCss = cancerCssTotals.values().stream().mapToDouble(x -> x).sum();

        for(Map.Entry<String,Double> entry : cancerCssTotals.entrySet())
        {
            double prob = totalCss > 0 ? entry.getValue() / totalCss : 0;
            cancerCssTotals.put(entry.getKey(), prob);
        }

        results.add(new SampleResult(
                sample.Id, CLASSIFIER, LIKELIHOOD, SNV_96_PAIRWISE_SIMILARITY.toString(), String.format("%.4g", totalCss), cancerCssTotals));

        // for non-ref cohorts, also report closest matches from amongst these
        if(mConfig.WriteSimilarities && mSampleDataCache.isMultiSampleNonRef())
        {
            for(Map.Entry<String,Integer> entry : mSampleCountsIndex.entrySet())
            {
                final String nonRefSampleId = entry.getKey();

                if(nonRefSampleId.equals(sample.Id))
                    continue;

                final double[] otherSampleCounts = mSampleCounts.getCol(entry.getValue());

                double css = calcCosineSim(sampleCounts, otherSampleCounts);

                if(mConfig.WriteSimilarities)
                {
                    recordCssSimilarity(
                            topMatches, sample.Id, nonRefSampleId, css, SNV_96_PAIRWISE_SIMILARITY.toString(),
                            CSS_SIMILARITY_MAX_MATCHES, CSS_SIMILARITY_CUTOFF);
                }
            }
        }

        similarities.addAll(topMatches);
    }

    private void addPosFreqCssResults(
            final SampleData sample, final List<SampleResult> results, final List<SampleSimilarity> similarities)
    {
        Integer sampleCountsIndex = mSamplePosFreqIndex.get(sample.Id);

        if(sampleCountsIndex == null)
        {
            CUP_LOGGER.debug("sample({}) has no SNV pos-freq data", sample.Id);
            return;
        }

        final double[] sampleCounts = mSamplePosFrequencies.getCol(sampleCountsIndex);
        double sampleTotal = sumVector(sampleCounts);

        // first run CSS against cancer cohorts
        int refCancerCount = mRefCancerSnvPosFrequencies.Cols;

        final Map<String,Double> cancerCssTotals = Maps.newHashMap();

        for(int i = 0; i < refCancerCount; ++i)
        {
            final String refCancerType = mRefSnvPosFreqCancerTypes.get(i);

            if(!isKnownCancerType(refCancerType))
                continue;

            if(!checkIsValidCancerType(sample, refCancerType, cancerCssTotals))
                continue;

            boolean matchesCancerType = sample.CancerType.equals(refCancerType);

            final double[] refPosFreqs = sample.isRefSample() && matchesCancerType ?
                    adjustRefPosFreqCounts(mRefCancerSnvPosFrequencies.getCol(i), sampleCounts, sampleTotal) : mRefCancerSnvPosFrequencies.getCol(i);

            double css = calcCosineSim(sampleCounts, refPosFreqs);

            if(css < SNV_POS_FREQ_CSS_THRESHOLD)
                continue;

            double cssWeight = pow(SNV_POS_FREQ_DIFF_EXPONENT, -100 * (1 - css));

            double weightedCss = css * cssWeight;

            Double total = cancerCssTotals.get(refCancerType);

            if(total == null)
                cancerCssTotals.put(refCancerType, weightedCss);
            else
                cancerCssTotals.put(refCancerType, total + weightedCss);
        }

        double totalCss = cancerCssTotals.values().stream().mapToDouble(x -> x).sum();

        for(Map.Entry<String,Double> entry : cancerCssTotals.entrySet())
        {
            cancerCssTotals.put(entry.getKey(), entry.getValue() / totalCss);
        }

        results.add(new SampleResult(
                sample.Id, CLASSIFIER, LIKELIHOOD, GENOMIC_POSITION_SIMILARITY.toString(), String.format("%.4g", totalCss), cancerCssTotals));

        // then run pairwise analysis if similarities are being analysed
        // addSnvPosSimilarities(sample, sampleCounts, similarities);
    }

    private double[] adjustRefPosFreqCounts(final double[] refPosFreqs, final double[] sampleCounts, final double sampleTotal)
    {
        double adjustMultiplier = sampleTotal > SNV_POS_FREQ_SNV_TOTAL_THRESHOLD ? SNV_POS_FREQ_SNV_TOTAL_THRESHOLD / sampleTotal : 1;

        double[] adjustedCounts = new double[refPosFreqs.length];

        for(int b = 0; b < refPosFreqs.length; ++b)
        {
            adjustedCounts[b] = max(refPosFreqs[b] - (sampleCounts[b] * adjustMultiplier), 0);
        }

        return adjustedCounts;
    }

    private void addSnvPosSimilarities(final SampleData sample, final double[] sampleCounts, final List<SampleSimilarity> similarities)
    {
        if(mRefSampleSnvPosFrequencies != null && mConfig.WriteSimilarities)
        {
            final List<SampleSimilarity> topMatches = Lists.newArrayList();

            for(Map.Entry<String,Integer> entry : mRefSamplePosFreqIndex.entrySet())
            {
                final String refSampleId = entry.getKey();

                if(refSampleId.equals(sample.Id))
                    continue;

                final String refCancerType = mSampleDataCache.RefSampleCancerTypeMap.get(refSampleId);

                if(refCancerType == null || !sample.isCandidateCancerType(refCancerType))
                    continue;

                final double[] otherSampleCounts = mRefSampleSnvPosFrequencies.getCol(entry.getValue());

                double css = calcCosineSim(sampleCounts, otherSampleCounts);

                if(css < SNV_CSS_THRESHOLD)
                    continue;

                if(mConfig.WriteSimilarities)
                {
                    recordCssSimilarity(
                            topMatches, sample.Id, refSampleId, css, GENOMIC_POSITION_SIMILARITY.toString(),
                            CSS_SIMILARITY_MAX_MATCHES, CSS_SIMILARITY_CUTOFF);
                }
            }

            similarities.addAll(topMatches);
        }
    }

    private static final String SIG_NAME_2 = "Sig2";
    private static final String SIG_NAME_13 = "Sig13";

    private static final String signatureDisplayName(final String sigName)
    {
        final String displayName = RefSomatics.REPORTABLE_SIGS.get(sigName);
        return displayName != null ? displayName : "UNKNOWN";
    }

    private void addSigContributionResults(final SampleData sample, final List<SampleResult> results)
    {
        final Map<String,Double> sampleSigContribs = mSampleSigContributions.get(sample.Id);

        if(sampleSigContribs == null)
        {
            CUP_LOGGER.error("sample({}) sig contributions not found", sample.Id);
            return;
        }

        // report on every one of the designated set

        for(final String sigName : RefSomatics.REPORTABLE_SIGS.keySet())
        {
            double sampleSigContrib = sampleSigContribs.containsKey(sigName) ? sampleSigContribs.get(sigName) : 0;

            // report the AID/APOBEC sigs 2 & 13 together
            if(sigName.equalsIgnoreCase(SIG_NAME_2))
            {
                Double otherAlloc = sampleSigContribs.get(SIG_NAME_13);
                if(otherAlloc != null)
                    sampleSigContrib += otherAlloc;
            }
            else if(sigName.equalsIgnoreCase(SIG_NAME_13))
            {
                continue;
            }

            for(Map.Entry<String,Map<String,double[]>> cancerContribs : mRefCancerSigContribPercentiles.entrySet())
            {
                final String cancerType = cancerContribs.getKey();
                final double[] refSigPercentiles = cancerContribs.getValue().get(sigName);

                if(refSigPercentiles == null)
                {
                    CUP_LOGGER.error("missing sig({}) data for cancerType({})", sigName, cancerType);
                    return;
                }

                Map<String,Double> cancerResults = Maps.newHashMap();
                double percentile = getPercentile(refSigPercentiles, sampleSigContrib, true);
                cancerResults.put(cancerType, percentile);

                results.add(new SampleResult(sample.Id, SNV, PERCENTILE, signatureDisplayName(sigName), round(sampleSigContrib), cancerResults));
            }
        }
    }

}
