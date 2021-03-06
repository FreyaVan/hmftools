package com.hartwig.hmftools.common.drivercatalog.panel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

public class DndsGeneNameMap {

    private final Set<String> hg19Set = Sets.newHashSet();
    private final Map<String, String> hg38Map = Maps.newHashMap();
    private final Map<String, String> hg19Map = Maps.newHashMap();

    public DndsGeneNameMap() {
        final InputStream inputStream = DriverGenePanel.class.getResourceAsStream("/drivercatalog/DndsGeneNameMap.tsv");
        new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(x -> !x.startsWith("hg19"))
                .map(x -> x.split("\t"))
                .forEach(strings -> {
                    String hg19 = strings[0];
                    String hg38 = strings[1];
                    if (!hg38.equals("NA")) {
                        hg38Map.put(hg38, hg19);
                    }
                    hg19Set.add(hg19);
                    hg19Map.put(hg19, hg38);
                });
    }

    public boolean isValidHg38Gene(@NotNull final String hg38GeneId) {
        return hg38Map.containsKey(hg38GeneId) && !hg38Map.get(hg38GeneId).equals("NA");
    }

    public boolean isValidHg19Gene(@NotNull final String hg19GeneId) {
        return hg19Map.containsKey(hg19GeneId);
    }

    @NotNull
    public String hg19Gene(@NotNull final String hg38GeneId) {
        String result = hg38Map.get(hg38GeneId);
        if (result == null || result.equals("NA")) {
            throw new IllegalArgumentException("Invalid hg38 gene " + hg38GeneId);
        }
        return result;
    }

    @NotNull
    String hg38Gene(@NotNull final String hg19GeneId) {
        String result = hg19Map.get(hg19GeneId);
        if (result == null ) {
            return "NA";
        }
        return result;
    }
}
