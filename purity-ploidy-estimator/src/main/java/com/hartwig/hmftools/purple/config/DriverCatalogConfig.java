package com.hartwig.hmftools.purple.config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.hartwig.hmftools.common.cli.DriverGenePanelConfig;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelAssembly;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelFactory;
import com.hartwig.hmftools.common.genome.chromosome.Chromosome;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspotFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.util.Strings;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface DriverCatalogConfig {

    String DRIVER_ENABLED = "driver_catalog";
    String HOTSPOT = "hotspots";

    static void addOptions(@NotNull Options options) {
        options.addOption(DRIVER_ENABLED, false, "Persist data to DB.");
        options.addOption(HOTSPOT, true, "Path to hotspot VCF");
        DriverGenePanelConfig.addGenePanelOption(false, options);
    }

    boolean enabled();

    @NotNull
    DriverGenePanel genePanel();

    @NotNull
    ListMultimap<Chromosome, VariantHotspot> hotspots();

    @NotNull
    static DriverCatalogConfig createConfig(@NotNull final CommandLine cmd, @NotNull RefGenomeData refGenomeData)
            throws ParseException, IOException {
        boolean enabled = cmd.hasOption(DRIVER_ENABLED);
        String hotspotVcf = cmd.getOptionValue(HOTSPOT, Strings.EMPTY);
        final DriverGenePanel genePanel;

        if (enabled) {
            if (!DriverGenePanelConfig.isConfigured(cmd)) {
                throw new ParseException(
                        DriverGenePanelConfig.DRIVER_GENE_PANEL_OPTION + " is a mandatory argument when " + DRIVER_ENABLED + " enabled");
            }

            if (hotspotVcf.isEmpty()) {
                throw new ParseException(HOTSPOT + " is a mandatory argument when " + DRIVER_ENABLED + " enabled");
            }

            if (!new File(hotspotVcf).exists()) {
                throw new IOException("Unable to open " + HOTSPOT + " file " + hotspotVcf);
            }

            final List<DriverGene> driverGenes = DriverGenePanelConfig.driverGenes(cmd);
            final DriverGenePanelAssembly driverGenePanelAssembly =
                    refGenomeData.isHg38() ? DriverGenePanelAssembly.HG38 : DriverGenePanelAssembly.HG19;
            genePanel = DriverGenePanelFactory.create(driverGenePanelAssembly, driverGenes);
        } else {
            genePanel = DriverGenePanelFactory.empty();
        }

        ListMultimap<Chromosome, VariantHotspot> hotspots =
                hotspotVcf.equals(Strings.EMPTY) ? ArrayListMultimap.create() : VariantHotspotFile.readFromVCF(hotspotVcf);


        return ImmutableDriverCatalogConfig.builder().enabled(enabled).hotspots(hotspots).genePanel(genePanel).build();
    }
}
