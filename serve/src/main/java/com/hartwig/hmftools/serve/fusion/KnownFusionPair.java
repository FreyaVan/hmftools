package com.hartwig.hmftools.serve.fusion;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class KnownFusionPair {

    @NotNull
    public abstract String geneUp();

    @Nullable
    public abstract Integer exonUp();

    @NotNull
    public abstract String geneDown();

    @Nullable
    public abstract Integer exonDown();
}