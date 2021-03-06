package com.hartwig.hmftools.serve.actionability;

import java.util.List;

import com.hartwig.hmftools.serve.actionability.fusion.ActionableFusion;
import com.hartwig.hmftools.serve.actionability.gene.ActionableGene;
import com.hartwig.hmftools.serve.actionability.hotspot.ActionableHotspot;
import com.hartwig.hmftools.serve.actionability.range.ActionableRange;
import com.hartwig.hmftools.serve.actionability.signature.ActionableSignature;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public interface ActionableEvents {

    @NotNull
    List<ActionableHotspot> hotspots();

    @NotNull
    List<ActionableRange> ranges();

    @NotNull
    List<ActionableGene> genes();

    @NotNull
    List<ActionableFusion> fusions();

    @NotNull
    List<ActionableSignature> signatures();
}
