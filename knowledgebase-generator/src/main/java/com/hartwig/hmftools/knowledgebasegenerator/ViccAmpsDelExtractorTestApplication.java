package com.hartwig.hmftools.knowledgebasegenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.ActionableAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.KnownAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.eventtype.DetermineEventOfGenomicMutation;
import com.hartwig.hmftools.knowledgebasegenerator.eventtype.EventType;
import com.hartwig.hmftools.knowledgebasegenerator.eventtype.EventTypeAnalyzer;
import com.hartwig.hmftools.knowledgebasegenerator.fusion.KnownFusions;
import com.hartwig.hmftools.knowledgebasegenerator.signatures.Signatures;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.reader.ViccJsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViccAmpsDelExtractorTestApplication {
    private static final Logger LOGGER = LogManager.getLogger(ViccAmpsDelExtractorTestApplication.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String viccJsonPath = System.getProperty("user.home") + "/hmf/projects/vicc/all.json";

        String source = "civic";
        LOGGER.info("Reading VICC json from {} with source '{}'", viccJsonPath, source);
        List<ViccEntry> viccEntries = ViccJsonReader.readSingleKnowledgebase(viccJsonPath, source);
        LOGGER.info("Read {} entries", viccEntries.size());

        //Lists of known genomic events
        List<KnownAmplificationDeletion> listKnownAmplification = Lists.newArrayList();
        List<KnownAmplificationDeletion> listKnownDeletion = Lists.newArrayList();
        List<String> listKnownVariants = Lists.newArrayList();
        List<String> listKnownRange = Lists.newArrayList();
        List<KnownFusions> listKnownFusionPairs = Lists.newArrayList();
        List<KnownFusions> listKnownFusionPromiscuousFive = Lists.newArrayList();
        List<KnownFusions> listKnownFusionPromiscuousThree = Lists.newArrayList();
        List<Signatures> listSignatures = Lists.newArrayList();

        //Lists of actionable genomic events
        List<ActionableAmplificationDeletion> listActionableDeletion = Lists.newArrayList();
        List<ActionableAmplificationDeletion> listActionableAmplification = Lists.newArrayList();

        for (ViccEntry viccEntry : viccEntries) {

            List<EventType> eventType = EventTypeAnalyzer.determineEventType(viccEntry);

            for (EventType type : eventType) {
                LOGGER.info("gene: " + type.gene() + " name: " + type.name() + " eventMap: " + type.eventMap() + " source: " + type.source());
                // Generating known events
                //TODO: map every genomic event to one object
                //TODO: if combined event use single event for determine known events

                for (Map.Entry<String, List<String>> entryDB : type.eventMap().entrySet()) {
                    for (String event : entryDB.getValue()) {

                        listKnownAmplification.add(DetermineEventOfGenomicMutation.checkKnownAmplification(viccEntry,
                                entryDB.getKey(),
                                event));
                        listKnownDeletion.add(DetermineEventOfGenomicMutation.checkKnownDeletion(viccEntry, entryDB.getKey(), event));
                        DetermineEventOfGenomicMutation.checkVariants(viccEntry, entryDB.getKey(), event);
                        DetermineEventOfGenomicMutation.checkRange(viccEntry, entryDB.getKey(), event);
                        listKnownFusionPairs.add(DetermineEventOfGenomicMutation.checkFusions(viccEntry, entryDB.getKey(), event));
                        listKnownFusionPromiscuousFive.add(DetermineEventOfGenomicMutation.checkFusions(viccEntry,
                                entryDB.getKey(),
                                event));
                        listKnownFusionPromiscuousThree.add(DetermineEventOfGenomicMutation.checkFusions(viccEntry,
                                entryDB.getKey(),
                                event));
                        listSignatures.add(DetermineEventOfGenomicMutation.checkSignatures(viccEntry, event));
                    }
                }
            }
        }

        List<KnownAmplificationDeletion> listAmpsFilter = Lists.newArrayList();
        List<KnownAmplificationDeletion> listDelsFIlter = Lists.newArrayList();
        Set<String> uniqueAmps = Sets.newHashSet();
        Set<String> uniqueDels = Sets.newHashSet();
        for (KnownAmplificationDeletion amps : listKnownAmplification) {
            if (!amps.eventType().isEmpty()) {
                listAmpsFilter.add(amps);
                uniqueAmps.add(amps.gene());
            }
        }

        List<String> sortedUniqueAmps = new ArrayList<String>(uniqueAmps);
        Collections.sort(sortedUniqueAmps);

        for (KnownAmplificationDeletion dels : listKnownDeletion) {
            if (!dels.eventType().isEmpty()) {
                listDelsFIlter.add(dels);
                uniqueDels.add(dels.gene());
            }
        }
        List<String> sortedUniqueDels = new ArrayList<String>(uniqueDels);
        Collections.sort(sortedUniqueDels);

        List<Signatures> listSignaturesFilter = Lists.newArrayList();
        for (Signatures signatures : listSignatures) {
            if (!signatures.eventType().isEmpty()) {
                listSignaturesFilter.add(signatures);
            }
        }

        List<ActionableAmplificationDeletion> listFilterActionableAmplifications = Lists.newArrayList();

        // If drug info/tumor location is known then variant is an actionable variant
        for (ActionableAmplificationDeletion actionableAmplification : listActionableAmplification) {
            if (actionableAmplification.level() != null && actionableAmplification.drug() != null
                    && actionableAmplification.drugType() != null && actionableAmplification.direction() != null
                    && actionableAmplification.sourceLink() != null && actionableAmplification.cancerType() != null) {
                listFilterActionableAmplifications.add(actionableAmplification);
            }
        }

        List<ActionableAmplificationDeletion> listFilterActionableDeletion = Lists.newArrayList();

        for (ActionableAmplificationDeletion actionableDeletion : listActionableDeletion) {
            if (actionableDeletion.level() != null && actionableDeletion.drug() != null && actionableDeletion.drugType() != null
                    && actionableDeletion.direction() != null && actionableDeletion.sourceLink() != null
                    && actionableDeletion.cancerType() != null) {
                listFilterActionableDeletion.add(actionableDeletion);
            }
        }

    }
}
