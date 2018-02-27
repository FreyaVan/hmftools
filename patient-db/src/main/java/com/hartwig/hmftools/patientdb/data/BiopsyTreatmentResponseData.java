package com.hartwig.hmftools.patientdb.data;

import java.time.LocalDate;

import com.hartwig.hmftools.common.ecrf.formstatus.FormStatusState;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class BiopsyTreatmentResponseData {

    @Nullable
    public abstract Integer treatmentId();

    @Nullable
    public abstract LocalDate assessmentDate();

    @Nullable
    public abstract LocalDate responseDate();

    @Nullable
    public abstract String response();

    @Nullable
    public abstract String measurementDone();

    @Nullable
    public abstract String boneOnlyDisease();

    @NotNull
    public abstract FormStatusState formStatus();

    public abstract boolean formLocked();

    @NotNull
    public static BiopsyTreatmentResponseData of(@Nullable final LocalDate assessmentDate, @Nullable final LocalDate responseDate,
            @Nullable final String response, @Nullable final String measurementDone, @Nullable final String boneOnlyDisease,
            @NotNull final FormStatusState formStatus, final boolean formLocked) {
        return ImmutableBiopsyTreatmentResponseData.of(null,
                assessmentDate,
                responseDate,
                response,
                measurementDone,
                boneOnlyDisease,
                formStatus,
                formLocked);
    }

    @Nullable
    public LocalDate date() {
        if (assessmentDate() == null) {
            return responseDate();
        }
        return assessmentDate();
    }

    @Override
    public String toString() {
        return response() + "(" + date() + ")";
    }
}
