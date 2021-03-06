package com.hartwig.hmftools.purple.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.chromosome.Chromosome;
import com.hartwig.hmftools.common.genome.chromosome.ChromosomeLength;
import com.hartwig.hmftools.common.genome.chromosome.ChromosomeLengthFactory;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.genepanel.HmfGenePanelSupplier;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.genome.position.GenomePositions;
import com.hartwig.hmftools.common.genome.refgenome.RefGenome;
import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;
import com.hartwig.hmftools.common.genome.region.ImmutableHmfExonRegion;
import com.hartwig.hmftools.common.genome.region.ImmutableHmfTranscriptRegion;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface RefGenomeData {

    Logger LOGGER = LogManager.getLogger(RefGenomeData.class);

    String REF_GENOME = "ref_genome";

    static void addOptions(@NotNull Options options) {
        options.addOption(REF_GENOME, true, "Path to the ref genome fasta file.");
    }

    boolean isHg38();

    @NotNull
    String refGenome();

    @NotNull
    Map<Chromosome, GenomePosition> length();

    @NotNull
    Map<Chromosome, GenomePosition> centromere();

    @NotNull
    List<HmfTranscriptRegion> genePanel();

    @NotNull
    static RefGenomeData createRefGenomeConfig(@NotNull CommandLine cmd) throws ParseException, IOException {

        if (!cmd.hasOption(REF_GENOME)) {
            throw new ParseException(REF_GENOME + " is a mandatory argument");
        }

        final String refGenomePath = cmd.getOptionValue(REF_GENOME);
        final Map<Chromosome, GenomePosition> lengthPositions;
        try (final IndexedFastaSequenceFile indexedFastaSequenceFile = new IndexedFastaSequenceFile(new File(refGenomePath))) {
            SAMSequenceDictionary sequenceDictionary = indexedFastaSequenceFile.getSequenceDictionary();
            if (sequenceDictionary == null) {
                throw new ParseException("Supplied ref genome must have associated sequence dictionary");
            }

            lengthPositions = fromLengths(ChromosomeLengthFactory.create(indexedFastaSequenceFile.getSequenceDictionary()));
        }

        final GenomePosition chr1Length = lengthPositions.get(HumanChromosome._1);
        final RefGenome refGenome;
        if (chr1Length != null && chr1Length.position() == RefGenome.HG38.lengths().get(HumanChromosome._1)) {
            refGenome = RefGenome.HG38;
        } else {
            refGenome = RefGenome.HG19;
        }
        LOGGER.info("Using ref genome: {}", refGenome);

        final Map<Chromosome, String> contigMap =
                lengthPositions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().chromosome()));

        final List<HmfTranscriptRegion> rawGenePanel =
                refGenome == RefGenome.HG38 ? HmfGenePanelSupplier.allGeneList38() : HmfGenePanelSupplier.allGeneList37();

        final Function<String, String> chromosomeToContig = s -> contigMap.get(HumanChromosome.fromString(s));

        final List<HmfTranscriptRegion> genePanel = rawGenePanel.stream()
                .filter(x -> HumanChromosome.contains(x.chromosome()) && contigMap.containsKey(HumanChromosome.fromString(x.chromosome())))
                .map(x -> updateContig(x, chromosomeToContig))
                .collect(Collectors.toList());

        return ImmutableRefGenomeData.builder()
                .length(toPosition(refGenome.lengths(), contigMap))
                .centromere(toPosition(refGenome.centromeres(), contigMap))
                .refGenome(refGenomePath)
                .isHg38(refGenome.equals(RefGenome.HG38))
                .genePanel(genePanel)
                .build();
    }

    @NotNull
    static HmfTranscriptRegion updateContig(HmfTranscriptRegion region, Function<String, String> fixContigFunction) {
        final String correctContig = fixContigFunction.apply(region.chromosome());

        return ImmutableHmfTranscriptRegion.builder()
                .from(region)
                .chromosome(correctContig)
                .exome(region.exome()
                        .stream()
                        .map(x -> ImmutableHmfExonRegion.builder().from(x).chromosome(correctContig).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @NotNull
    static Map<Chromosome, GenomePosition> toPosition(@NotNull final Map<Chromosome, Long> longs,
            @NotNull final Map<Chromosome, String> contigMap) {
        final Map<Chromosome, GenomePosition> result = Maps.newHashMap();

        for (Map.Entry<Chromosome, String> entry : contigMap.entrySet()) {
            final Chromosome chromosome = entry.getKey();
            final String contig = entry.getValue();
            if (longs.containsKey(chromosome)) {
                result.put(chromosome, GenomePositions.create(contig, longs.get(chromosome)));
            }

        }

        return result;
    }

    @NotNull
    static Map<Chromosome, GenomePosition> fromLengths(@NotNull final Collection<ChromosomeLength> lengths) {
        return lengths.stream()
                .filter(x -> HumanChromosome.contains(x.chromosome()))
                .collect(Collectors.toMap(x -> HumanChromosome.fromString(x.chromosome()),
                        item -> GenomePositions.create(item.chromosome(), item.length())));
    }

}
