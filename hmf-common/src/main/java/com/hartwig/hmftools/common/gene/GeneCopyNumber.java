package com.hartwig.hmftools.common.gene;

import com.hartwig.hmftools.common.copynumber.CopyNumber;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface GeneCopyNumber extends GeneRegion, CopyNumber {

    @NotNull
    String gene();

    double maxCopyNumber();

    double minCopyNumber();

    double meanCopyNumber();

    int regions();

    @Override
    default int value() {
        return (int) Math.round(minCopyNumber());
    }
}
