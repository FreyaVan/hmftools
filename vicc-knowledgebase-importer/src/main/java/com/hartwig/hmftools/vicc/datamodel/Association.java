package com.hartwig.hmftools.vicc.datamodel;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class Association {

    @Nullable
    public abstract String variantName();

    @NotNull
    public abstract List<Evidence> evidence();

    @NotNull
    public abstract String evidenceLevel();

    @NotNull
    public abstract String evidenceLabel();

    @Nullable
    public abstract String responseType();

    @Nullable
    public abstract String drugLabels();

    @Nullable
    public abstract String sourceLink();

    @Nullable
    public abstract List<String> publicationUrls();

    @NotNull
    public abstract Phenotype phenotype();

    @NotNull
    public abstract String description();

    @Nullable
    public abstract List<EnvironmentalContext> environmentalContexts();

    @Nullable
    public abstract String oncogenic();
}
