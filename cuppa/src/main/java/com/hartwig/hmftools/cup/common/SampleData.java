package com.hartwig.hmftools.cup.common;

import static com.hartwig.hmftools.cup.CuppaConfig.CANCER_SUBTYPE_OTHER;
import static com.hartwig.hmftools.cup.CuppaConfig.DATA_DELIM;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_OTHER;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_OVARY;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_PROSTATE;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_UNKNOWN;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_UTERUS;

import java.util.Map;

import com.hartwig.hmftools.common.purple.gender.Gender;

public class SampleData
{
    public final String Id;
    public final String CancerType;
    public final String CancerSubtype;
    public final String PrimaryLocation;
    public final String PrimarySubLocation;
    public final String PrimaryType;
    public final String PrimarySubtype;

    private boolean mIsRefSample;
    private Gender mGenderType;

    public SampleData(final String id, final String cancerType, final String cancerSubtype)
    {
        this(id, cancerType, cancerSubtype, "", "", "", "");
    }

    public SampleData(
            final String id, final String cancerType, final String cancerSubtype,
            final String location, final String locationSubtype, final String primaryType, final String primarySubtype)
    {
        Id = id;
        CancerType = cancerType;
        CancerSubtype = cancerSubtype;
        PrimaryLocation = location;
        PrimarySubLocation = locationSubtype;
        PrimaryType = primaryType;
        PrimarySubtype = primarySubtype;

        mGenderType = null;
        mIsRefSample = false;
    }

    public Gender gender() { return mGenderType; }
    public void setGender(final Gender gender) { mGenderType = gender; }

    public boolean isRefSample() { return mIsRefSample; }
    public void setRefSample() { mIsRefSample = true; }

    public boolean isKnownCancerType() { return isKnownCancerType(CancerType); }

    public static boolean isKnownCancerType(final String cancerType)
    {
        return !cancerType.equals(CANCER_TYPE_OTHER) && !cancerType.equals(CANCER_TYPE_UNKNOWN);
    }

    public static SampleData from(final Map<String,Integer> fieldsIndexMap, final String data)
    {
        final String[] items = data.split(DATA_DELIM, -1);
        final String sampleId = items[fieldsIndexMap.get("SampleId")];

        String cancerType = extractOptionalField(fieldsIndexMap, items, "CancerType", CANCER_TYPE_UNKNOWN);
        String cancerSubtype = extractOptionalField(fieldsIndexMap, items, "CancerSubtype", CANCER_TYPE_UNKNOWN);
        String primaryLocation = extractOptionalField(fieldsIndexMap, items, "PrimaryTumorLocation", CANCER_SUBTYPE_OTHER);
        String primarySubLocation = extractOptionalField(fieldsIndexMap, items, "PrimaryTumorSubLocation", "");
        String primaryType = extractOptionalField(fieldsIndexMap, items, "PrimaryTumorType", "");
        String primarySubtype = extractOptionalField(fieldsIndexMap, items, "PrimaryTumorSubType", "");

        return new SampleData(sampleId, cancerType, cancerSubtype, primaryLocation, primarySubLocation, primaryType, primarySubtype);
    }

    private static String extractOptionalField(
            final Map<String,Integer> fieldsIndexMap, final String[] items, final String fieldName, final String defaultValue)
    {
        return fieldsIndexMap.containsKey(fieldName) ?items[fieldsIndexMap.get(fieldName)] : defaultValue;
    }

    public boolean isCandidateCancerType(final String cancerType)
    {
        if(mGenderType == null)
            return true;

        if(cancerType.contains(CANCER_TYPE_UTERUS) || cancerType.contains(CANCER_TYPE_OVARY))
        {
            if(mGenderType != Gender.FEMALE)
                return false;
        }
        if(cancerType.contains(CANCER_TYPE_PROSTATE))
        {
            if(mGenderType == Gender.FEMALE)
                return false;
        }

        return true;
    }
}
