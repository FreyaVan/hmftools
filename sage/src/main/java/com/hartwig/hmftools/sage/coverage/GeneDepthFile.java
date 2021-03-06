package com.hartwig.hmftools.sage.coverage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class GeneDepthFile {
    private static final String DELIMITER = "\t";

    public static void write(@NotNull final String filename, @NotNull final List<GeneDepth> depths) throws IOException {
        Files.write(new File(filename).toPath(), toLines(depths));
    }

    @NotNull
    private static List<String> toLines(@NotNull final List<GeneDepth> depths) {
        if (!depths.isEmpty()) {
            final List<String> lines = Lists.newArrayList();
            lines.add(header());
            depths.stream().map(GeneDepthFile::toString).forEach(lines::add);
            return lines;
        }

        return Collections.emptyList();
    }

    static String header() {
        StringJoiner joiner = new StringJoiner(DELIMITER).add("gene");
        for (int i = 0; i < 30; i++) {
            joiner.add(String.valueOf(i));
        }

        for (int i = 30; i < 100; i += 10) {
            joiner.add(i + "-" + (i + 9));
        }

        joiner.add("100+");

        return joiner.toString();
    }

    @NotNull
    static String toString(@NotNull final GeneDepth depth) {
        StringJoiner joiner = new StringJoiner(DELIMITER).add(depth.gene());
        for (int i : depth.depthCounts()) {
            joiner.add(String.valueOf(i));
        }

        return joiner.toString();
    }

}
