package com.hartwig.hmftools.vicc.annotation;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.classification.CompositeEventMatcher;
import com.hartwig.hmftools.common.serve.classification.EventMatcher;

import org.jetbrains.annotations.NotNull;

class SignatureClassifier implements EventMatcher {

    private static final Set<String> SIGNATURES = Sets.newHashSet("Microsatellite Instability-High");

    @NotNull
    public static EventMatcher create(@NotNull List<EventMatcher> noMatchEventMatchers) {
        return new CompositeEventMatcher(noMatchEventMatchers, new SignatureClassifier());
    }

    private SignatureClassifier() {
    }

    @Override
    public boolean matches(@NotNull String gene, @NotNull String event) {
        return SIGNATURES.contains(event);
    }
}
