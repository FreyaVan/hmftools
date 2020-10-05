package com.hartwig.hmftools.serve.actionability.gene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public final class ActionableGeneFile {

    private static final String DELIMITER = "\t";
    private static final String ACTIONABLE_GENE_TSV = "actionableGenes.tsv";

    private ActionableGeneFile() {
    }

    @NotNull
    public static String actionableGeneTsvPath(@NotNull String serveActionabilityDir) {
        return serveActionabilityDir + File.separator + ACTIONABLE_GENE_TSV;
    }

    public static void write(@NotNull String actionableGeneTsv, @NotNull List<ActionableGene> actionableGenes) throws IOException {
        List<String> lines = Lists.newArrayList();
        lines.add(header());
        lines.addAll(toLines(actionableGenes));
        Files.write(new File(actionableGeneTsv).toPath(), lines);
    }

    @NotNull
    public static List<ActionableGene> read(@NotNull String actionableGeneTsv) throws IOException {
        List<String> lines = Files.readAllLines(new File(actionableGeneTsv).toPath());
        // Skip header
        return fromLines(lines.subList(1, lines.size()));
    }

    @NotNull
    private static String header() {
        return new StringJoiner(DELIMITER, "", "").add("gene")
                .add("type")
                .add("source")
                .add("treatment")
                .add("cancerType")
                .add("doid")
                .add("level")
                .add("direction")
                .toString();
    }

    @NotNull
    @VisibleForTesting
    static List<ActionableGene> fromLines(@NotNull List<String> lines) {
        List<ActionableGene> actionableGenes = Lists.newArrayList();
        for (String line : lines) {
            actionableGenes.add(fromLine(line));
        }
        return actionableGenes;
    }

    @NotNull
    private static ActionableGene fromLine(@NotNull String line) {
         String[] values = line.split(DELIMITER);
        return ImmutableActionableGene.builder()
                .gene(values[0])
                .type(values[1])
                .source(values[2])
                .treatment(values[3])
                .cancerType(values[4])
                .doid(values[5])
                .level(values[6])
                .direction(values[7])
                .build();
    }

    @NotNull
    @VisibleForTesting
    static List<String> toLines(@NotNull List<ActionableGene> actionableGenes) {
        List<String> lines = Lists.newArrayList();
        for (ActionableGene actionableGene : actionableGenes) {
            lines.add(toLine(actionableGene));
        }
        return lines;
    }

    @NotNull
    private static String toLine(@NotNull ActionableGene gene) {
        return new StringJoiner(DELIMITER).add(gene.gene())
                .add(gene.type())
                .add(gene.source())
                .add(gene.treatment())
                .add(gene.cancerType())
                .add(gene.doid())
                .add(gene.level())
                .add(gene.direction())
                .toString();
    }
}