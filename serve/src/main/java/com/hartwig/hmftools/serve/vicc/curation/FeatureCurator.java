package com.hartwig.hmftools.serve.vicc.curation;

import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ImmutableFeature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureCurator {

    private static final Logger LOGGER = LogManager.getLogger(FeatureCurator.class);

    @NotNull
    private final Set<CurationKey> evaluatedCurationKeys = Sets.newHashSet();

    public FeatureCurator() {
    }

    @Nullable
    public Feature curate(@NotNull ViccEntry entry, @NotNull Feature feature) {
        CurationKey key = new CurationKey(feature.geneSymbol(), entry.transcriptId(), feature.name());
        evaluatedCurationKeys.add(key);

        if (entry.source() == ViccSource.ONCOKB) {
            if (CurationFactory.ONCOKB_FEATURE_BLACKLIST.contains(key)) {
                LOGGER.debug("Blacklisting feature '{}' for gene {} in {}", feature.name(), feature.geneSymbol(), entry.source());
                return null;
            } else {
                String mappedFeatureName = CurationFactory.ONCOKB_FEATURE_NAME_MAPPINGS.get(key);
                if (mappedFeatureName != null) {
                    LOGGER.debug("Mapping feature '{}' to '{}' for gene {} in {}",
                            feature.name(),
                            mappedFeatureName,
                            feature.geneSymbol(),
                            entry.source());
                    return ImmutableFeature.builder().from(feature).name(mappedFeatureName).build();
                }
            }
        }

        return feature;
    }

    @NotNull
    public Set<CurationKey> unusedCurationKeys() {
        Set<CurationKey> unusedKeys = Sets.newHashSet();
        unusedKeys.addAll(CurationFactory.ONCOKB_FEATURE_BLACKLIST);
        unusedKeys.addAll(CurationFactory.ONCOKB_FEATURE_NAME_MAPPINGS.keySet());
        unusedKeys.removeAll(evaluatedCurationKeys);
        return unusedKeys;
    }
}