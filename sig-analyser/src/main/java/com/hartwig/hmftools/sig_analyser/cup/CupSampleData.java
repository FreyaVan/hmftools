package com.hartwig.hmftools.sig_analyser.cup;

import static com.hartwig.hmftools.sig_analyser.cup.CupConstants.CT_UNKNOWN;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.commons.compress.utils.Lists;

public class CupSampleData
{
    public final String SampleId;
    public final String CancerType;
    public final String CancerSubtype;

    private int mIndex;

    // SNV signature results
    private final Map<String,Double> mCancerCssTotals;
    private final Map<String,Double> mSnvSigAllocations;

    // drive genes and results
    private final List<CupSampleDriverData> mDrivers;
    private final Map<String,Double> mDriverCancerTypeProbs;

    public CupSampleData(final String sampleId, final String cancerType, final String cancerSubtype)
    {
        mIndex = -1;
        SampleId = sampleId;
        CancerType = cancerType;
        CancerSubtype = cancerSubtype;
        mCancerCssTotals = Maps.newHashMap();
        mSnvSigAllocations = Maps.newHashMap();
        mDriverCancerTypeProbs = Maps.newHashMap();
        mDrivers = Lists.newArrayList();
    }

    public void setSampleIndex(final int sampleIndex) { mIndex = sampleIndex; }
    public int index() { return mIndex; }

    public boolean isUnknownCancerType() { return CancerType.equalsIgnoreCase(CT_UNKNOWN); }

    public void addSampleCss(final String cancerType, double cssWeight)
    {
        Double total = mCancerCssTotals.get(cancerType);

        if(total == null)
            mCancerCssTotals.put(cancerType, cssWeight);
        else
            mCancerCssTotals.put(cancerType, total + cssWeight);
    }

    public final Map<String,Double> getCancerCssTotals() { return mCancerCssTotals; }
    public final Map<String,Double> getSnvSigAllocations() { return mSnvSigAllocations; }

    public double getTotalWeightedCss()
    {
        return mCancerCssTotals.values().stream().mapToDouble(x -> x).sum();
    }

    public final List<CupSampleDriverData> getDrivers() { return mDrivers; }
    public final Map<String,Double> getDriverCancerTypeProbs() { return mDriverCancerTypeProbs; }
}
