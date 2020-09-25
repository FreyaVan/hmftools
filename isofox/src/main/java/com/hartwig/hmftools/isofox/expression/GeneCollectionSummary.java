package com.hartwig.hmftools.isofox.expression;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.isofox.results.GeneResult;
import com.hartwig.hmftools.isofox.results.TranscriptResult;

public class GeneCollectionSummary
{
    public final String ChrId;
    public final List<String> GeneIds;
    public final String GeneNames;
    public final List<CategoryCountsData> TransCategoryCounts;

    public final List<GeneResult> GeneResults;
    public final List<TranscriptResult> TranscriptResults;

    private final Map<String,Double> mFitAllocations; // results from the expected rate vs counts fit routine, stored per transcript
    private double mFitResiduals;

    public GeneCollectionSummary(
            final String chrId, final List<String> geneIds, final String geneNames, final List<CategoryCountsData> transCategoryCounts)
    {
        ChrId = chrId;
        GeneIds = geneIds;
        GeneNames = geneNames;
        TransCategoryCounts = Lists.newArrayList(transCategoryCounts);
        GeneResults = Lists.newArrayList();
        TranscriptResults = Lists.newArrayList();

        mFitAllocations = Maps.newHashMap();
        mFitResiduals = 0;
    }

    public void setFitResiduals(double residuals) { mFitResiduals = residuals; }
    public double getFitResiduals() { return mFitResiduals; }

    public Map<String,Double> getFitAllocations() { return mFitAllocations; }

    public double getFitAllocation(final String transName)
    {
        Double allocation = mFitAllocations.get(transName);
        return allocation != null ? allocation : 0;
    }

    public void setFitAllocations()
    {
        Map<String,Double> geneSpliceTotals = Maps.newHashMap();

        for (final TranscriptResult transResult : TranscriptResults)
        {
            final String transName = transResult.Trans.TransName;
            double fitAllocation = getFitAllocation(transName);

            transResult.setFitAllocation(fitAllocation);

            Double geneFitAllocation = geneSpliceTotals.get(transResult.Trans.GeneId);
            if(geneFitAllocation == null)
                geneSpliceTotals.put(transResult.Trans.GeneId, fitAllocation);
            else
                geneSpliceTotals.put(transResult.Trans.GeneId, geneFitAllocation + fitAllocation);

        }

        for(final GeneResult geneResult : GeneResults)
        {
            Double geneFitAllocation = geneSpliceTotals.get(geneResult.GeneData.GeneId);
            geneResult.setFitAllocation(
                    geneFitAllocation != null ? geneFitAllocation : 0, getFitAllocation(geneResult.GeneData.GeneId));
        }
    }

    public void assignLowMapQualityFragments()
    {
        int totalLowMqFragments = TransCategoryCounts.stream().mapToInt(x -> x.lowMapQualityCount()).sum();

        if(totalLowMqFragments == 0)
            return;

        double totalTranscriptAlloc = TranscriptResults.stream().mapToDouble(x -> x.getFitAllocation()).sum();
        double totalUnsplicedAlloc = GeneResults.stream().mapToDouble(x -> x.getUnsplicedAlloc()).sum();
        double totalAlloc = totalTranscriptAlloc + totalUnsplicedAlloc;

        if(totalAlloc == 0)
            return;

        double splicedLowMqFrags = totalLowMqFragments * totalTranscriptAlloc / totalAlloc;
        double unsplicedLowMqFrags = totalLowMqFragments * totalUnsplicedAlloc / totalAlloc;

        // divide amoungst transcripts
        for(final TranscriptResult transResult : TranscriptResults)
        {
            double transAlloc = transResult.getFitAllocation() / totalTranscriptAlloc * splicedLowMqFrags;
            transResult.setLowMapQualsAllocation(transAlloc);
        }

        // and then amongst genes
        for(final GeneResult geneResult : GeneResults)
        {
            double splicedAlloc = geneResult.getSplicedAlloc() / totalTranscriptAlloc * splicedLowMqFrags;
            double unsplicedAlloc = geneResult.getUnsplicedAlloc() / totalUnsplicedAlloc * unsplicedLowMqFrags;
            geneResult.setLowMapQualsAllocation(splicedAlloc + unsplicedAlloc);
        }
    }

    public void allocateResidualsToGenes()
    {
        if(GeneResults.size() == 1)
        {
            GeneResults.get(0).setFitResiduals(mFitResiduals);
        }
        else
        {
            // divvy up residuals between the genes according to their length
            long totalGeneLength = GeneResults.stream().mapToLong(x -> x.GeneData.length()).sum();

            for (final GeneResult geneResult : GeneResults)
            {
                double residualsFraction = geneResult.GeneData.length() / (double) totalGeneLength * mFitResiduals;
                geneResult.setFitResiduals(residualsFraction);
            }
        }
    }

    public void applyGcAdjustments(final double[] gcAdjustments)
    {
        double originalTotal = 0;
        double newTotal = 0;

        for(final CategoryCountsData catCounts : TransCategoryCounts)
        {
            originalTotal += catCounts.fragmentCount();
            catCounts.applyGcAdjustments(gcAdjustments);
            newTotal += catCounts.fragmentCount();
        }

        // ensure no overall net change to counts after the adjustment
        // eg if old total was 10K and new is 2K, then will mutiply all new counts by 5
        double adjustFactor = originalTotal/newTotal;
        TransCategoryCounts.forEach(x -> x.adjustCounts(adjustFactor));
    }
}
