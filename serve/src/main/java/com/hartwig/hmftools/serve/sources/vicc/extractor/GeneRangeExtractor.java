package com.hartwig.hmftools.serve.sources.vicc.extractor;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;
import com.hartwig.hmftools.common.genome.region.HmfExonRegion;
import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;
import com.hartwig.hmftools.serve.actionability.range.MutationTypeFilter;
import com.hartwig.hmftools.serve.sources.vicc.annotation.GeneRangeAnnotation;
import com.hartwig.hmftools.serve.sources.vicc.annotation.ImmutableGeneRangeAnnotation;
import com.hartwig.hmftools.vicc.annotation.FeatureType;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class GeneRangeExtractor {

    private static final Logger LOGGER = LogManager.getLogger(GeneRangeExtractor.class);

    @NotNull
    private final Map<String, HmfTranscriptRegion> transcriptPerGeneMap;

    public GeneRangeExtractor(@NotNull Map<String, HmfTranscriptRegion> transcriptPerGeneMap) {
        this.transcriptPerGeneMap = transcriptPerGeneMap;
    }

    @NotNull
    public Map<Feature, List<GeneRangeAnnotation>> extractGeneRanges(@NotNull ViccEntry viccEntry, @NotNull List<DriverGene> driverGenes) {
        Map<Feature, List<GeneRangeAnnotation>> geneRangesPerFeature = Maps.newHashMap();
        List<GeneRangeAnnotation> geneRangeAnnotation = Lists.newArrayList();
        for (Feature feature : viccEntry.features()) {
            HmfTranscriptRegion canonicalTranscript = transcriptPerGeneMap.get(feature.geneSymbol());
            if (canonicalTranscript == null) {
                break;
            }
            FeatureType featureType = feature.type();

            if (featureType == FeatureType.FUSION_PAIR_AND_GENE_RANGE_EXON) {
                String transcriptIdVicc = viccEntry.transcriptId();
                if (transcriptIdVicc == null || transcriptIdVicc.equals(canonicalTranscript.transcriptID())) {
                    if (feature.name().matches("[0-9]+")) {
                        String exonNumber = feature.name();
                        extractGeneRangesPerFeature(exonNumber, feature, canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else {
                        LOGGER.warn("Could not determine range of feature {}", feature);
                    }
                } else {
                    LOGGER.warn("transcript IDs not equal for transcript VICC {} and HMF {} for {} ",
                            transcriptIdVicc,
                            canonicalTranscript.transcriptID(),
                            feature);
                }
            }
            if (featureType == FeatureType.GENE_RANGE_EXON) {
                String transcriptIdVicc = viccEntry.transcriptId();
                if (transcriptIdVicc == null || transcriptIdVicc.equals(canonicalTranscript.transcriptID())) {
                    if (feature.name().matches("[0-9]+")) {
                        String exonNumber = feature.name();
                        extractGeneRangesPerFeature(exonNumber,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().contains(",")) {
                        String[] exons = feature.name()
                                .substring((feature.name().toLowerCase().indexOf("exon")))
                                .replace(" or ", ",")
                                .replace("exon ", "")
                                .split(",");
                        extractGeneRangesPerFeatureMultipleExons(exons,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().contains("or")) {
                        String[] exons = feature.name()
                                .substring((feature.name().toLowerCase().indexOf("exon")))
                                .replace(" or ", ",")
                                .replace("exon ", "")
                                .split(",");
                        extractGeneRangesPerFeatureMultipleExons(exons,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().contains("-")) {
                        String exons = feature.name();
                        List<HmfExonRegion> exonRegions = canonicalTranscript.exome();

                        if (exons.equals("mutation")) {
                            exons = feature.name().substring((feature.name().toLowerCase().indexOf("exon"))).replace("exon ", "");
                        }

                        if (isInteger(exons.split("-")[0]) && isInteger(exons.split("-")[1])) {
                            // HmfExonRegion start with count 0 so exonNumber is one below
                            int startExon = Integer.parseInt(exons.split("-")[0]) - 1;
                            // HmfExonRegion start with count 0 so exonNumber is one below
                            int endExon = Integer.parseInt(exons.split("-")[1]) - 1;
                            HmfExonRegion hmfExonRegionStart = exonRegions.get(startExon);
                            HmfExonRegion hmfExonRegionEnd = exonRegions.get(endExon);
                            long start = hmfExonRegionStart.start();
                            long end = hmfExonRegionEnd.end();
                            String chromosome = hmfExonRegionStart.chromosome();

                            geneRangeAnnotation.add(ImmutableGeneRangeAnnotation.builder()
                                    .gene(feature.geneSymbol())
                                    .start(start)
                                    .end(end)
                                    .chromosome(chromosome)
                                    .rangeInfo(exons)
                                    .mutationType(extractMutationFilter(driverGenes,
                                            feature.geneSymbol(),
                                            extractSpecificMutationTypeFilter(feature),
                                            feature))
                                    .build());
                            geneRangesPerFeature.put(feature, geneRangeAnnotation);
                        }
                    } else if (feature.name().equals("mutation") && !feature.name().contains("-")) {
                        String exonNumber = feature.name().substring((feature.name().toLowerCase().indexOf("exon"))).replace("exon ", "");
                        extractGeneRangesPerFeature(exonNumber,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().equals("exon")) {
                        String exonNumber = feature.name()
                                .substring((feature.name().toLowerCase().indexOf("exon")))
                                .replace("exon ", "")
                                .replace(" deletions", "")
                                .replace(" insertions", "");
                        extractGeneRangesPerFeature(exonNumber,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().equals("proximal")) {
                        String exonNumber = feature.name().substring((feature.name().toLowerCase().indexOf("exon"))).replace("exon ", "");
                        extractGeneRangesPerFeature(exonNumber,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else if (feature.name().equals("(Partial")) {
                        String[] exons = feature.name()
                                .substring((feature.name().toLowerCase().indexOf("exons")))
                                .replace("exons ", "")
                                .replace(")", "")
                                .replace("Exons ", "")
                                .replace(" ", "")
                                .split("&");
                        extractGeneRangesPerFeatureMultipleExons(exons,
                                feature,
                                canonicalTranscript,
                                driverGenes,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                extractSpecificMutationTypeFilter(feature));
                    } else {
                        LOGGER.warn("Could not determine range of feature {}", feature);
                    }
                } else {
                    LOGGER.warn("transcript IDs not equal for transcript VICC {} and HMF {} for {} ",
                            transcriptIdVicc,
                            canonicalTranscript.transcriptID(),
                            feature);
                }
            } else if (featureType == FeatureType.GENE_RANGE_CODON) {
                //TODO remove EX and T148HFSX9 from gene range codon featureType
                if (!feature.name().equals("T148HFSX9") && !feature.name().equals("EX")) {
                    if (!feature.name().contains("_")) {
                        geneRangesPerFeature = determineRanges(viccEntry,
                                feature,
                                feature.name(),
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                canonicalTranscript,
                                driverGenes,
                                extractSpecificMutationTypeFilter(feature));
                        geneRangesPerFeature.put(feature, geneRangeAnnotation);
                    } else if (feature.name().contains("_") && isInteger(feature.name().split("_")[0].replaceAll("\\D+", "")) && isInteger(
                            feature.name().split("_")[1].replaceAll("\\D+", ""))) { //example L485_P490 BRAF
                        int startCodon = Integer.parseInt(feature.name().split("_")[0].replaceAll("\\D+", ""));
                        int endCodon = Integer.parseInt(feature.name().split("_")[1].replaceAll("\\D+", ""));
                        geneRangesPerFeature = determineRangesMulti(viccEntry,
                                feature,
                                startCodon,
                                endCodon,
                                geneRangeAnnotation,
                                geneRangesPerFeature,
                                canonicalTranscript,
                                driverGenes,
                                extractSpecificMutationTypeFilter(feature));
                        geneRangesPerFeature.put(feature, geneRangeAnnotation);
                    }
                }
            }
        }
        return geneRangesPerFeature;

    }

    @NotNull
    private static MutationTypeFilter extractMutationFilter(@NotNull List<DriverGene> driverGenes, @NotNull String gene,
            @NotNull MutationTypeFilter specificMutationType, @NotNull Feature feature) {
        for (DriverGene driverGene : driverGenes) {
            if (driverGene.gene().equals(gene)) {
                if (driverGene.likelihoodType() == DriverCategory.ONCO) {
                    if (specificMutationType == MutationTypeFilter.UNKNOWN) {
                        return MutationTypeFilter.MISSENSE_ANY;
                    } else {
                        return specificMutationType;
                    }
                } else if (driverGene.likelihoodType() == DriverCategory.TSG) {
                    if (specificMutationType == MutationTypeFilter.UNKNOWN) {
                        //TODO which inactivation event for TSG?
                        if (feature.biomarkerType() != null && feature.biomarkerType().contains("inactivation")) {
                            return MutationTypeFilter.MISSENSE_ANY;
                        } else {
                            return MutationTypeFilter.ANY;
                        }
                    } else {
                        return specificMutationType;
                    }
                }
            }
        }
        LOGGER.warn("Gene {} is not present in driver catalog", gene);
        return MutationTypeFilter.UNKNOWN;
    }

    private static void extractGeneRangesPerFeature(@NotNull String exonNumber, @NotNull Feature feature,
            @NotNull HmfTranscriptRegion canonicalTranscript, @NotNull List<DriverGene> driverGenes,
            @NotNull List<GeneRangeAnnotation> geneRangeAnnotation, @NotNull Map<Feature, List<GeneRangeAnnotation>> geneRangesPerFeature,
            @NotNull MutationTypeFilter specificMutationType) {
        int exonNumberList = Integer.parseInt(exonNumber) - 1; // HmfExonRegion start with count 0 so exonNumber is one below

        geneRangeAnnotation.add(extractExonGenomicPositions(feature,
                canonicalTranscript,
                exonNumberList,
                driverGenes,
                exonNumber,
                specificMutationType));
        geneRangesPerFeature.put(feature, geneRangeAnnotation);
    }

    private static void extractGeneRangesPerFeatureMultipleExons(@NotNull String[] exonNumbers, @NotNull Feature feature,
            @NotNull HmfTranscriptRegion canonicalTranscript, @NotNull List<DriverGene> driverGenes,
            @NotNull List<GeneRangeAnnotation> geneRangeAnnotation, Map<Feature, List<GeneRangeAnnotation>> geneRangesPerFeature,
            @NotNull MutationTypeFilter specificMutationType) {
        for (String exon : exonNumbers) {
            int exonNumberList = Integer.parseInt(exon) - 1; // HmfExonRegion start with count 0 so exonNumber is one below
            geneRangeAnnotation.add(extractExonGenomicPositions(feature,
                    canonicalTranscript,
                    exonNumberList,
                    driverGenes,
                    exon,
                    specificMutationType));
        }
        geneRangesPerFeature.put(feature, geneRangeAnnotation);
    }

    @NotNull
    private static MutationTypeFilter extractSpecificMutationTypeFilter(@NotNull Feature feature) {
        String featureEvent = feature.name().toLowerCase();
        String extractSpecificInfoOfEvent = featureEvent.substring(featureEvent.lastIndexOf(" ") + 1);
        if (featureEvent.contains("skipping mutation") || featureEvent.contains("splice site insertion")) {
            return MutationTypeFilter.SPLICE;
        } else if (extractSpecificInfoOfEvent.equals("deletions")) {
            return MutationTypeFilter.MISSENSE_INFRAME_DELETION;
        } else if (extractSpecificInfoOfEvent.equals("insertions")) {
            return MutationTypeFilter.MISSENSE_INFRAME_INSERTION;
        } else if (extractSpecificInfoOfEvent.equals("deletion/insertion") || extractSpecificInfoOfEvent.equals("insertions/deletions")) {
            return MutationTypeFilter.MISSENSE_INFRAME_ANY;
        } else if (extractSpecificInfoOfEvent.equals("frameshift")) {
            return MutationTypeFilter.NONSENSE_OR_FRAMESHIFT;
        }

        return MutationTypeFilter.UNKNOWN;
    }

    @NotNull
    private static GeneRangeAnnotation extractExonGenomicPositions(@NotNull Feature feature,
            @NotNull HmfTranscriptRegion canonicalTranscript, int exonNumberList, @NotNull List<DriverGene> driverGenes,
            @NotNull String exonNumber, @NotNull MutationTypeFilter specificMutationType) {
        List<HmfExonRegion> exonRegions = canonicalTranscript.exome();
        HmfExonRegion hmfExonRegion = exonRegions.get(exonNumberList);
        long start = hmfExonRegion.start();
        long end = hmfExonRegion.end();
        String chromosome = hmfExonRegion.chromosome();

        return ImmutableGeneRangeAnnotation.builder()
                .gene(feature.geneSymbol())
                .start(start)
                .end(end)
                .chromosome(chromosome)
                .rangeInfo(exonNumber)
                .mutationType(extractMutationFilter(driverGenes, feature.geneSymbol(), specificMutationType, feature))
                .build();
    }

    @NotNull
    private static Map<Feature, List<GeneRangeAnnotation>> determineRanges(@NotNull ViccEntry viccEntry, @NotNull Feature feature,
            @NotNull String featureName, @NotNull List<GeneRangeAnnotation> geneRangeAnnotations,
            @NotNull Map<Feature, List<GeneRangeAnnotation>> geneRangesPerFeature, @NotNull HmfTranscriptRegion canonicalTranscript,
            @NotNull List<DriverGene> driverGenes, @NotNull MutationTypeFilter specificMutationType) {
        String transcriptIdVicc = viccEntry.transcriptId();

        if (transcriptIdVicc == null || transcriptIdVicc.equals(canonicalTranscript.transcriptID())) {
            String geneSymbol = feature.geneSymbol();
            if (!featureName.isEmpty() && isInteger(featureName.replaceAll("\\D+", ""))) {
                int codonNumber = Integer.parseInt(featureName.replaceAll("\\D+", ""));
                List<GenomeRegion> genomeRegions = canonicalTranscript.codonByIndex(codonNumber);
                if (genomeRegions != null && genomeRegions.size() == 1) {
                    long start = genomeRegions.get(0).start();
                    long end = genomeRegions.get(0).end();
                    String chromosome = genomeRegions.get(0).chromosome();

                    geneRangeAnnotations.add(ImmutableGeneRangeAnnotation.builder()
                            .gene(geneSymbol)
                            .start(start)
                            .end(end)
                            .chromosome(chromosome)
                            .rangeInfo(featureName)
                            .mutationType(extractMutationFilter(driverGenes, feature.geneSymbol(), specificMutationType, feature))
                            .build());
                    geneRangesPerFeature.put(feature, geneRangeAnnotations);
                }

            } else {
                LOGGER.warn("Multiple genomic regions known for event {}", feature);
            }
        } else {
            LOGGER.warn("transcript IDs not equal for transcript VICC {} and HMF {} for {} ",
                    transcriptIdVicc,
                    canonicalTranscript.transcriptID(),
                    feature);
        }
        return geneRangesPerFeature;
    }

    @NotNull
    private static Map<Feature, List<GeneRangeAnnotation>> determineRangesMulti(@NotNull ViccEntry viccEntry, @NotNull Feature feature,
            int startCodon, int endCodon, @NotNull List<GeneRangeAnnotation> geneRangeAnnotation,
            @NotNull Map<Feature, List<GeneRangeAnnotation>> geneRangesPerFeature, @NotNull HmfTranscriptRegion canonicalTranscript,
            @NotNull List<DriverGene> driverGenes, @NotNull MutationTypeFilter specificMutationType) {
        String transcriptIdVicc = viccEntry.transcriptId();

        if (transcriptIdVicc == null || transcriptIdVicc.equals(canonicalTranscript.transcriptID())) {
            String geneSymbol = feature.geneSymbol();
            List<GenomeRegion> genomeRegionsStart = canonicalTranscript.codonByIndex(startCodon);
            List<GenomeRegion> genomeRegionsEnd = canonicalTranscript.codonByIndex(endCodon);

            if (genomeRegionsStart.size() == 1 && genomeRegionsEnd.size() == 1) {
                long start = genomeRegionsStart.get(0).start();
                long end = genomeRegionsEnd.get(0).end();
                String chromosomeStart = genomeRegionsStart.get(0).chromosome();
                String chromosomeEnd = genomeRegionsEnd.get(0).chromosome();

                String chromosome = Strings.EMPTY;
                if (chromosomeStart.equals(chromosomeEnd)) {
                    chromosome = chromosomeStart;
                }
                geneRangeAnnotation.add(ImmutableGeneRangeAnnotation.builder()
                        .gene(geneSymbol)
                        .start(start)
                        .end(end)
                        .chromosome(chromosome)
                        .rangeInfo(startCodon + "-" + endCodon)
                        .mutationType(extractMutationFilter(driverGenes, feature.geneSymbol(), specificMutationType, feature))
                        .build());
                geneRangesPerFeature.put(feature, geneRangeAnnotation);

            } else {
                LOGGER.warn("Multiple genomic regions known for event {}", feature);
            }
        } else {
            LOGGER.warn("transcript IDs not equal for transcript VICC {} and HMF {} for {} ",
                    transcriptIdVicc,
                    canonicalTranscript.transcriptID(),
                    feature);
        }
        return geneRangesPerFeature;
    }

    private static boolean isInteger(@NotNull String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }

    }
}
