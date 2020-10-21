package com.hartwig.hmftools.common.clinical;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.lims.LimsStudy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PatientTumorLocationV2Functions {

    private static final Logger LOGGER = LogManager.getLogger(PatientTumorLocationV2Functions.class);

    private PatientTumorLocationV2Functions() {
    }

    @Nullable
    public static PatientTumorLocationV2 findTumorLocationForSample(@NotNull List<PatientTumorLocationV2> patientTumorLocations,
            @NotNull String sample) {
        String patientIdentifier = toPatientIdentifier(sample);

        List<PatientTumorLocationV2> matchingIdTumorLocations = patientTumorLocations.stream()
                .filter(patientTumorLocation -> patientTumorLocation.patientIdentifier().equals(patientIdentifier))
                .collect(Collectors.toList());

        // We should never have more than one tumor location for a single patient.
        assert matchingIdTumorLocations.size() < 2;

        if (matchingIdTumorLocations.size() == 1) {
            return matchingIdTumorLocations.get(0);
        } else {
            LOGGER.warn("Could not find patient '{}' in list of primary tumor locations!", patientIdentifier);
            return null;
        }
    }

    @NotNull
    private static String toPatientIdentifier(@NotNull String sample) {
        LimsStudy study = LimsStudy.fromSampleId(sample);
        if (sample.length() >= 12 && (study == LimsStudy.CPCT || study == LimsStudy.DRUP || study == LimsStudy.WIDE
                || study == LimsStudy.CORE)) {
            return sample.substring(0, 12);
        } else if (sample.toUpperCase().startsWith("COLO829")) {
            return "COLO829";
        }

        // If we want to fetch the tumor location for a sample we cannot derive patient of, we assume sample and patient are equal.
        return sample;
    }
}