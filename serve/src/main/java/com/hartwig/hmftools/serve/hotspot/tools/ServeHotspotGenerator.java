package com.hartwig.hmftools.serve.hotspot.tools;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspotComparator;
import com.hartwig.hmftools.serve.RefGenomeVersion;
import com.hartwig.hmftools.serve.hotspot.HotspotFunctions;
import com.hartwig.hmftools.serve.hotspot.KnownHotspot;
import com.hartwig.hmftools.serve.hotspot.ProteinKeyFormatter;
import com.hartwig.hmftools.serve.hotspot.ProteinResolver;
import com.hartwig.hmftools.serve.hotspot.ProteinResolverFactory;
import com.hartwig.hmftools.serve.sources.docm.DocmEntry;
import com.hartwig.hmftools.serve.sources.docm.DocmExtractor;
import com.hartwig.hmftools.serve.sources.docm.DocmFileReader;
import com.hartwig.hmftools.serve.sources.docm.curation.DocmCurator;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigEntry;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigExtractor;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigFileReader;
import com.hartwig.hmftools.serve.sources.vicc.ViccExtractor;
import com.hartwig.hmftools.serve.sources.vicc.ViccExtractorFactory;
import com.hartwig.hmftools.serve.sources.vicc.ViccReader;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

public class ServeHotspotGenerator {

    private static final Logger LOGGER = LogManager.getLogger(ServeHotspotGenerator.class);

    private static final Set<ViccSource> VICC_SOURCES_TO_INCLUDE = Sets.newHashSet(ViccSource.CIVIC, ViccSource.CGI);
    private static final Integer MAX_VICC_ENTRIES = null;

    private static final String INFO_SOURCES = "sources";
    private static final String INFO_INPUT = "input";

    public static void main(String[] args) throws IOException {
        Configurator.setRootLevel(Level.DEBUG);

        String hostname = InetAddress.getLocalHost().getHostName();
        LOGGER.debug("Running on '{}'", hostname);

        String refGenomeFastaFile;
        String serveSourceDir;

        boolean generateHotspots;
        String hotspotVcf = null;

        if (hostname.toLowerCase().contains("datastore")) {
            refGenomeFastaFile = "/data/common/refgenomes/Homo_sapiens.GRCh37.GATK.illumina/Homo_sapiens.GRCh37.GATK.illumina.fasta";
            serveSourceDir = "/data/common/dbs/serve";

            generateHotspots = true;
            hotspotVcf = System.getProperty("user.home") + "/tmp/hotspotsServe.vcf";
        } else {
            refGenomeFastaFile = System.getProperty("user.home") + "/hmf/refgenome/Homo_sapiens.GRCh37.GATK.illumina.fasta";
            serveSourceDir = System.getProperty("user.home") + "/hmf/projects/serve";

            generateHotspots = false;
        }

        String viccJson = serveSourceDir + "/vicc/all.json";
        String docmTsv = serveSourceDir + "/docm/docm_v3.2.tsv";
        String hartwigCohortTsv = serveSourceDir + "/hartwig/hartwig_cohort.tsv";
        String hartwigCuratedTsv = serveSourceDir + "/hartwig/hartwig_curated.tsv";

        RefGenomeVersion refGenomeVersion = RefGenomeVersion.HG19;
        LOGGER.debug("Configured '{}' as the reference fasta path", refGenomeFastaFile);
        LOGGER.debug("Configured '{}' as the VICC json path", viccJson);
        LOGGER.debug("Configured '{}' as the DoCM TSV path", docmTsv);
        LOGGER.debug("Configured '{}' as the Hartwig Cohort TSV path", hartwigCohortTsv);
        LOGGER.debug("Configured '{}' as the Hartwig Curated TSV path", hartwigCuratedTsv);
        LOGGER.debug("Configured '{}' as the hotspot output VCF", hotspotVcf);
        LOGGER.debug("Configured '{}' for generating hotspots yes/no", generateHotspots);

        ProteinResolver proteinResolver = generateHotspots
                ? ProteinResolverFactory.transvarWithRefGenome(refGenomeVersion, refGenomeFastaFile)
                : ProteinResolverFactory.dummy();

        List<KnownHotspot> hartwigCohortHotspots = hartwigCohortHotspots(hartwigCohortTsv, proteinResolver, generateHotspots);

        // Temporary code to just check cohort hotspot generation.
        System.exit(0);

        List<KnownHotspot> hartwigCuratedHotspots = hartwigCuratedHotspots(hartwigCuratedTsv, proteinResolver, generateHotspots);
        List<KnownHotspot> viccHotspots = viccHotspots(viccJson, proteinResolver);
        List<KnownHotspot> docmHotspots = docmHotspots(docmTsv, proteinResolver);

        LOGGER.info("Merging {} VICC hotspots with {} DoCM hotspots and {} Hartwig Cohort hotspots and {} Hartwig Curated hotspots",
                viccHotspots.size(),
                docmHotspots.size(),
                hartwigCohortHotspots.size(),
                hartwigCuratedHotspots.size());
        List<KnownHotspot> allHotspots = Lists.newArrayList();
        allHotspots.addAll(viccHotspots);
        allHotspots.addAll(docmHotspots);
        allHotspots.addAll(hartwigCohortHotspots);
        allHotspots.addAll(hartwigCuratedHotspots);

        List<KnownHotspot> mergedConsolidatedHotspots = HotspotFunctions.consolidateHotspots(allHotspots);

        if (generateHotspots && hotspotVcf != null) {
            writeHotspots(hotspotVcf, mergedConsolidatedHotspots);

            Set<String> unresolvedProteinAnnotations = proteinResolver.unresolvedProteinAnnotations();
            if (!unresolvedProteinAnnotations.isEmpty()) {
                LOGGER.warn("Protein resolver could not resolve {} protein annotations", unresolvedProteinAnnotations.size());
                for (String unresolvedProteinAnnotation : unresolvedProteinAnnotations) {
                    LOGGER.warn("Protein resolver could not resolve protein annotation '{}'", unresolvedProteinAnnotation);
                }
            } else {
                LOGGER.info("Protein resolver could resolve hotspots for every protein annotation");
            }
        }
    }

    @NotNull
    private static List<KnownHotspot> viccHotspots(@NotNull String viccJson, @NotNull ProteinResolver proteinResolver) throws IOException {
        List<ViccEntry> viccEntries = ViccReader.readAndCurateRelevantEntries(viccJson, VICC_SOURCES_TO_INCLUDE, MAX_VICC_ENTRIES);

        ViccExtractor viccExtractor = ViccExtractorFactory.buildViccExtractor(proteinResolver, Lists.newArrayList());
        return viccExtractor.extractFromViccEntries(viccEntries).knownHotspots();
    }

    @NotNull
    private static List<KnownHotspot> docmHotspots(@NotNull String docmTsv, @NotNull ProteinResolver proteinResolver) throws IOException {
        LOGGER.info("Reading DoCM TSV from '{}'", docmTsv);
        List<DocmEntry> docmEntries = DocmFileReader.readDcomFile(docmTsv);
        LOGGER.info(" Read {} entries", docmEntries.size());

        DocmCurator curator = new DocmCurator();
        LOGGER.info("Curating {} DoCM entries", docmEntries.size());
        List<DocmEntry> curatedDocmEntries = curator.curate(docmEntries);
        LOGGER.info(" Finished DoCM curation. {} entries remaining after curation. {} entries have been removed",
                curatedDocmEntries.size(),
                docmEntries.size() - curatedDocmEntries.size());
        curator.reportUnusedBlacklistEntries();

        return new DocmExtractor(proteinResolver).extractFromDocmEntries(curatedDocmEntries);
    }

    @NotNull
    private static List<KnownHotspot> hartwigCohortHotspots(@NotNull String hartwigCohortTsv, @NotNull ProteinResolver proteinResolver,
            boolean addExplicitHotspots) throws IOException {
        LOGGER.info("Reading Hartwig Cohort TSV from '{}'", hartwigCohortTsv);
        List<HartwigEntry> hartwigCohortEntries = HartwigFileReader.read(hartwigCohortTsv);
        LOGGER.info(" Read {} entries", hartwigCohortEntries.size());

        HartwigExtractor hartwigExtractor = new HartwigExtractor(Knowledgebase.HARTWIG_COHORT, proteinResolver, addExplicitHotspots);

        return hartwigExtractor.extractFromHartwigEntries(hartwigCohortEntries);
    }

    @NotNull
    private static List<KnownHotspot> hartwigCuratedHotspots(@NotNull String hartwigCuratedTsv, @NotNull ProteinResolver proteinResolver,
            boolean addExplicitHotspots) throws IOException {
        LOGGER.info("Reading Hartwig Curated TSV from '{}'", hartwigCuratedTsv);
        List<HartwigEntry> hartwigCuratedEntries = HartwigFileReader.read(hartwigCuratedTsv);
        LOGGER.info(" Read {} entries", hartwigCuratedEntries.size());

        HartwigExtractor hartwigExtractor = new HartwigExtractor(Knowledgebase.HARTWIG_CURATED, proteinResolver, addExplicitHotspots);

        return hartwigExtractor.extractFromHartwigEntries(hartwigCuratedEntries);
    }

    private static void writeHotspots(@NotNull String hotspotVcf, @NotNull List<KnownHotspot> hotspots) {
        VariantContextWriter writer = new VariantContextWriterBuilder().setOutputFile(hotspotVcf)
                .setOutputFileType(VariantContextWriterBuilder.OutputType.VCF)
                .modifyOption(Options.INDEX_ON_THE_FLY, false)
                .build();

        VCFHeader header = new VCFHeader(Sets.newHashSet(), Lists.newArrayList());
        header.addMetaDataLine(new VCFInfoHeaderLine(INFO_INPUT, VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "input"));
        header.addMetaDataLine(new VCFInfoHeaderLine(INFO_SOURCES,
                VCFHeaderLineCount.UNBOUNDED,
                VCFHeaderLineType.String,
                "sources [civic,cgi,docm,hartwigcurated,hartwigcohort]"));

        writer.writeHeader(header);

        hotspots.sort(new VariantHotspotComparator());
        for (KnownHotspot hotspot : hotspots) {
            List<Allele> hotspotAlleles = buildAlleles(hotspot);

            VariantContext variantContext = new VariantContextBuilder().noGenotypes()
                    .source("SERVE")
                    .chr(hotspot.chromosome())
                    .start(hotspot.position())
                    .alleles(hotspotAlleles)
                    .computeEndFromAlleles(hotspotAlleles, (int) hotspot.position())
                    .attribute(INFO_SOURCES, buildSourcesString(hotspot.sources()))
                    .attribute(INFO_INPUT,
                            ProteinKeyFormatter.toProteinKey(hotspot.gene(), hotspot.transcript(), hotspot.proteinAnnotation()))
                    .make();

            LOGGER.debug("Writing {}", variantContext);
            writer.add(variantContext);

        }
        writer.close();
    }

    @NotNull
    private static List<Allele> buildAlleles(@NotNull VariantHotspot hotspot) {
        Allele ref = Allele.create(hotspot.ref(), true);
        Allele alt = Allele.create(hotspot.alt(), false);

        return Lists.newArrayList(ref, alt);
    }

    @VisibleForTesting
    @NotNull
    static String buildSourcesString(@NotNull Set<Knowledgebase> sources) {
        StringJoiner sourceJoiner = new StringJoiner(",");
        for (Knowledgebase source : sources) {
            sourceJoiner.add(source.display().toLowerCase());
        }
        return sourceJoiner.toString();
    }
}
