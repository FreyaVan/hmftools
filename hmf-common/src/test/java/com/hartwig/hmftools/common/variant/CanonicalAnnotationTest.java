package com.hartwig.hmftools.common.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelFactoryTest;
import com.hartwig.hmftools.common.genome.region.CanonicalTranscript;
import com.hartwig.hmftools.common.genome.region.CanonicalTranscriptFactory;
import com.hartwig.hmftools.common.variant.snpeff.ImmutableSnpEffAnnotation;
import com.hartwig.hmftools.common.variant.snpeff.SnpEffAnnotation;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class CanonicalAnnotationTest {

    private static final String CDKN2A = "ENST00000498124";
    private static final String CDKN2A_P14ARF = "ENST00000361570";
    private static final String CDKN2A_OTHER = "ENST00000000000";

    private final DriverGenePanel genePanel = DriverGenePanelFactoryTest.testGenePanel();
    private final List<CanonicalTranscript> transcripts = CanonicalTranscriptFactory.create37();

    @Test
    public void testTrimEnsembleTranscriptId() {
        assertEquals("ENST00000361570", CanonicalAnnotation.trimEnsembleVersion("ENST00000361570"));
        assertEquals("ENST00000361570", CanonicalAnnotation.trimEnsembleVersion("ENST00000361570.v8"));
    }

    @Test
    public void favourCDKN2ASnpEffAnnotation() {
        SnpEffAnnotation p16 = createSnpEffAnnotation("CDKN2A", CDKN2A, VariantConsequence.MISSENSE_VARIANT);
        SnpEffAnnotation p14 = createSnpEffAnnotation("CDKN2A", CDKN2A_P14ARF, VariantConsequence.MISSENSE_VARIANT);
        SnpEffAnnotation other = createSnpEffAnnotation("CDKN2A", CDKN2A_OTHER, VariantConsequence.MISSENSE_VARIANT);

        List<SnpEffAnnotation> all = Lists.newArrayList(other, p14, p16);

        CanonicalAnnotation victim = new CanonicalAnnotation(genePanel.driverGenes(), transcripts);
        assertEquals(p16, victim.canonicalSnpEffAnnotation(all).get());
        assertFalse(victim.canonicalSnpEffAnnotation(Lists.newArrayList(p14)).isPresent());
    }

    @Test
    public void favourDriverCatalogGenes() {
        TranscriptAnnotation nonCanonicalDriverGene =
                createSnpEffAnnotation("ATP1A1", "ENST00000295598", VariantConsequence.MISSENSE_VARIANT);
        TranscriptAnnotation noDriverGene = createSnpEffAnnotation("AL136376.1", "ENST00000598661", VariantConsequence.MISSENSE_VARIANT);
        TranscriptAnnotation canonicalDriverGene = createSnpEffAnnotation("ATP1A1", "ENST00000537345", VariantConsequence.MISSENSE_VARIANT);

        CanonicalAnnotation victim = new CanonicalAnnotation(genePanel.driverGenes(), transcripts);
        assertEquals(Optional.empty(), victim.pickCanonicalFavourDriverGene(Lists.newArrayList(nonCanonicalDriverGene)));

        Optional<TranscriptAnnotation> annotationSecond =
                victim.pickCanonicalFavourDriverGene(Lists.newArrayList(nonCanonicalDriverGene, noDriverGene));
        assertTrue(annotationSecond.isPresent());
        assertEquals(noDriverGene, annotationSecond.get());

        Optional<TranscriptAnnotation> annotationThird =
                victim.pickCanonicalFavourDriverGene(Lists.newArrayList(nonCanonicalDriverGene, noDriverGene, canonicalDriverGene));
        assertTrue(annotationThird.isPresent());
        assertEquals(canonicalDriverGene, annotationThird.get());
    }

    @NotNull
    private static SnpEffAnnotation createSnpEffAnnotation(@NotNull final String gene, @NotNull final String transcript,
            @NotNull VariantConsequence consequence) {
        return ImmutableSnpEffAnnotation.builder()
                .allele("")
                .effects("")
                .consequences(Lists.newArrayList(consequence))
                .severity("")
                .gene(gene)
                .geneID(gene)
                .featureType("transcript")
                .featureID(transcript)
                .transcriptBioType("")
                .rank("")
                .hgvsCoding("")
                .hgvsProtein("")
                .cDNAPosAndLength("")
                .cdsPosAndLength("")
                .aaPosAndLength("")
                .distance("")
                .addition("")
                .build();
    }
}
