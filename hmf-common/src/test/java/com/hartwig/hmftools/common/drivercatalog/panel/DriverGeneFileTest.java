package com.hartwig.hmftools.common.drivercatalog.panel;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class DriverGeneFileTest {

    @Test
    public void testFormats() {
        List<DriverGene> newFormat = resourceFile("driver.gene.panel.with.germline.tsv");
        List<DriverGene> oldFormat = resourceFile("driver.gene.panel.without.germline.tsv");

        assertEquals(3, newFormat.size());
        assertEquals(3, oldFormat.size());
    }

    @NotNull
    private static List<DriverGene> resourceFile(String resource) {
        final InputStream inputStream = DriverGenePanelFactoryTest.class.getResourceAsStream("/drivercatalog/" + resource);
        return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(x -> !x.startsWith("gene") && !x.startsWith("HG"))
                .map(DriverGeneFile::fromString)
                .collect(Collectors.toList());
    }

}
