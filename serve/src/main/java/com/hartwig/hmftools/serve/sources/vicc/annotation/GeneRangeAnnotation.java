package com.hartwig.hmftools.serve.sources.vicc.annotation;

import com.hartwig.hmftools.serve.actionability.range.MutationTypeFilter;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class GeneRangeAnnotation {

    @NotNull
    public abstract String gene();

    @NotNull
    public abstract String chromosome();

    public abstract long start();

    public abstract long end();

    //TODO: can be removed when range positions are verified
    public abstract int rangeInfo();

    //TODO: can be removed when range positions are verified
    @Nullable
    public abstract String exonId();

    @NotNull
    public abstract MutationTypeFilter mutationType();

}
