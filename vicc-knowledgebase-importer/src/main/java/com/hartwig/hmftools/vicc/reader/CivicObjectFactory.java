package com.hartwig.hmftools.vicc.reader;

import static com.hartwig.hmftools.vicc.reader.JsonFunctions.jsonArrayToStringList;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hartwig.hmftools.vicc.datamodel.civic.Civic;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicAvatars;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicClinicalTrial;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicCoordinates;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicDescription;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicDisease;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicDrugs;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicError;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicEvidenceItems;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicLastCommentedOn;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicLastModified;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicLastReviewed;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicLifecycleActions;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicOrganization;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicProfileImage;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicPublicationDate;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicSource;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicUser;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicVariantGroup;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicVariantTypes;
import com.hartwig.hmftools.vicc.datamodel.civic.CivicVariants;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivic;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicAvatars;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicClinicalTrial;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicCoordinates;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicDescription;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicDisease;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicDrugs;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicError;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicEvidenceItems;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicLastCommentedOn;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicLastModified;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicLastReviewed;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicLifecycleActions;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicOrganization;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicProfileImage;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicPublicationDate;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicSource;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicUser;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicVariantGroup;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicVariantTypes;
import com.hartwig.hmftools.vicc.datamodel.civic.ImmutableCivicVariants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

final class CivicObjectFactory {

    private static final Logger LOGGER = LogManager.getLogger(CivicObjectFactory.class);

    private static final List<Integer> EXPECTED_CIVIC_ELEMENT_SIZES = Lists.newArrayList(21);
    private static final List<Integer> EXPECTED_CIVIC_AVATARS_SIZES = Lists.newArrayList(4);
    private static final List<Integer> EXPECTED_CIVIC_COORDINATES_SIZES = Lists.newArrayList(12);
    private static final List<Integer> EXPECTED_CIVIC_DISEASES_SIZES = Lists.newArrayList(5);
    private static final List<Integer> EXPECTED_CIVIC_DRUGS_SIZES = Lists.newArrayList(3);
    private static final List<Integer> EXPECTED_CIVIC_EVIDENCE_ITEMS_SIZES = Lists.newArrayList(17);
    private static final List<Integer> EXPECTED_CIVIC_LAST_COMMENTED_ON_SIZES = Lists.newArrayList(2);
    private static final List<Integer> EXPECTED_CIVIC_LAST_MODIFIED_SIZES = Lists.newArrayList(2);
    private static final List<Integer> EXPECTED_CIVIC_LAST_REVIEWED_SIZES = Lists.newArrayList(2);
    private static final List<Integer> EXPECTED_CIVIC_LIFECYCLE_ACTIONS_SIZES = Lists.newArrayList(0, 3);
    private static final List<Integer> EXPECTED_CIVIC_ORGANIZATION_SIZES = Lists.newArrayList(0, 5);
    private static final List<Integer> EXPECTED_CIVIC_PROFILE_IMAGE_SIZES = Lists.newArrayList(5);
    private static final List<Integer> EXPECTED_CIVIC_PUBLICATIONS_DATE_SIZES = Lists.newArrayList(0, 1, 2, 3);
    private static final List<Integer> EXPECTED_CIVIC_SOURCE_SIZES = Lists.newArrayList(13);
    private static final List<Integer> EXPECTED_CIVIC_USER_SIZES = Lists.newArrayList(21);
    private static final List<Integer> EXPECTED_CIVIC_VARIANTTYPES_SIZES = Lists.newArrayList(6);
    private static final List<Integer> EXPECTED_CIVIC_VARIANTGROUPS_SIZES = Lists.newArrayList(5);
    private static final List<Integer> EXPECTED_CIVIC_VARIANTS_SIZES = Lists.newArrayList(10);
    private static final List<Integer> EXPECTED_CIVIC_PROVISIONALVALUES_SIZES = Lists.newArrayList(0, 1);
    private static final List<Integer> EXPECTED_CIVIC_DESCRIPTION_SIZES = Lists.newArrayList(2);
    private static final List<Integer> EXPECTED_CIVIC_ERROR_SIZES = Lists.newArrayList(0);
    private static final List<Integer> EXPECTED_CIVIC_CLINICALTRIAL_SIZES = Lists.newArrayList(4);

    private CivicObjectFactory() {
    }

    @NotNull
    static Civic create(@NotNull JsonObject objectCivic) {
        Set<String> keysCivic = objectCivic.keySet();
        if (!EXPECTED_CIVIC_ELEMENT_SIZES.contains(keysCivic.size())) {
            LOGGER.warn("Found " + keysCivic.size() + " in civic rather than the expected " + EXPECTED_CIVIC_ELEMENT_SIZES);
            LOGGER.warn(keysCivic);
        }

        return ImmutableCivic.builder()
                .variantGroups(!objectCivic.has("variant_groups")
                        ? null
                        : createVariantsGroups(objectCivic.getAsJsonArray("variant_groups")))
                .entrezName(objectCivic.getAsJsonPrimitive("entrez_name").getAsString())
                .variantTypes(createVariantTypes(objectCivic.getAsJsonArray("variant_types")))
                .civicActionabilityScore(objectCivic.get("civic_actionability_score").isJsonNull()
                        ? null
                        : objectCivic.getAsJsonPrimitive("civic_actionability_score").getAsString())
                .clinvarEntries(jsonArrayToStringList(objectCivic.getAsJsonArray("clinvar_entries")))
                .lifecycleActions(createLifeCycleActions(objectCivic.getAsJsonObject("lifecycle_actions")))
                .variantAliases(jsonArrayToStringList(objectCivic.getAsJsonArray("variant_aliases")))
                .alleleRegistryId(objectCivic.get("allele_registry_id").isJsonNull()
                        ? null
                        : objectCivic.getAsJsonPrimitive("allele_registry_id").getAsString())
                .provisional_values(createCivicDesription(objectCivic.getAsJsonObject("provisional_values")))
                .geneId(objectCivic.getAsJsonPrimitive("gene_id").getAsString())
                .name(objectCivic.getAsJsonPrimitive("name").getAsString())
                .evidenceItem(createEvidenceitems(objectCivic.getAsJsonArray("evidence_items")))
                .sources(createCivicSource(objectCivic.getAsJsonArray("sources")))
                .entrezId(objectCivic.getAsJsonPrimitive("entrez_id").getAsString())
                .assertions(jsonArrayToStringList(objectCivic.getAsJsonArray("assertions")))
                .hgvs_expressions(jsonArrayToStringList(objectCivic.getAsJsonArray("hgvs_expressions")))
                .errors(createCivicError(objectCivic.getAsJsonObject("errors")))
                .coordinates(createCoordinates(objectCivic.getAsJsonObject("coordinates")))
                .type(objectCivic.getAsJsonPrimitive("type").getAsString())
                .id(objectCivic.getAsJsonPrimitive("id").getAsString())
                .description(objectCivic.getAsJsonPrimitive("description").getAsString())
                .build();
    }

    @NotNull
    private static List<CivicSource> createCivicSource(@NotNull JsonArray arraySource) {
        List<CivicSource> sourceList = Lists.newArrayList();
        for (JsonElement source : arraySource) {
            Set<String> keysSource = source.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_SOURCE_SIZES.contains(keysSource.size())) {
                LOGGER.warn("Found " + keysSource.size() + " in civic source rather than the expected " + EXPECTED_CIVIC_SOURCE_SIZES);
                LOGGER.warn(keysSource);
            }
            sourceList.add(ImmutableCivicSource.builder()
                    .status(source.getAsJsonObject().getAsJsonPrimitive("status").getAsString())
                    .openAccess(source.getAsJsonObject().get("open_access").isJsonNull()
                            ? null
                            : source.getAsJsonObject().getAsJsonPrimitive("open_access").getAsString())
                    .name(source.getAsJsonObject().get("name").isJsonNull()
                            ? null
                            : source.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .journal(source.getAsJsonObject().get("journal").isJsonNull()
                            ? null
                            : source.getAsJsonObject().getAsJsonPrimitive("journal").getAsString())
                    .citation(source.getAsJsonObject().getAsJsonPrimitive("citation").getAsString())
                    .pmc_Id(source.getAsJsonObject().get("pmc_id").isJsonNull()
                            ? null
                            : source.getAsJsonObject().getAsJsonPrimitive("pmc_id").getAsString())
                    .fullJournalTitle(source.getAsJsonObject().get("full_journal_title").isJsonNull()
                            ? null
                            : source.getAsJsonObject().getAsJsonPrimitive("full_journal_title").getAsString())
                    .sourceUrl(source.getAsJsonObject().getAsJsonPrimitive("source_url").getAsString())
                    .clinicalTrials(createCivicClinicalTrials(source.getAsJsonObject().getAsJsonArray("clinical_trials")))
                    .pubmedId(source.getAsJsonObject().getAsJsonPrimitive("pubmed_id").getAsString())
                    .isReview(source.getAsJsonObject().getAsJsonPrimitive("is_review").getAsString())
                    .publicationDate(createPublicationDate(source.getAsJsonObject().getAsJsonObject("publication_date")))
                    .id(source.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .build());
        }

        return sourceList;
    }

    @NotNull
    private static CivicError createCivicError(@NotNull JsonObject objectError) {
        Set<String> keysError = objectError.getAsJsonObject().keySet();
        if (!EXPECTED_CIVIC_ERROR_SIZES.contains(keysError.size())) {
            LOGGER.warn("Found " + keysError.size() + " in civic error rather than the expected " + EXPECTED_CIVIC_ERROR_SIZES);
            LOGGER.warn(keysError);
        }
        return ImmutableCivicError.builder().build();
    }

    @NotNull
    private static CivicDescription createCivicDesription(@NotNull JsonObject objectProvisionalValues) {
        Set<String> keysProvisionalValues = objectProvisionalValues.getAsJsonObject().keySet();
        if (!EXPECTED_CIVIC_PROVISIONALVALUES_SIZES.contains(keysProvisionalValues.size())) {
            LOGGER.warn("Found " + keysProvisionalValues.size() + " in civic provisional values rather than the expected "
                    + EXPECTED_CIVIC_PROVISIONALVALUES_SIZES);
            LOGGER.warn(keysProvisionalValues);
        }

        JsonObject description =
                !objectProvisionalValues.has("description") ? null : objectProvisionalValues.getAsJsonObject("description");
        if (description != null) {
            Set<String> keysDescription = description.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_DESCRIPTION_SIZES.contains(keysDescription.size())) {
                LOGGER.warn("Found " + keysDescription.size() + " in civic description rather than the expected "
                        + EXPECTED_CIVIC_DESCRIPTION_SIZES);
                LOGGER.warn(keysDescription);
            }
            return ImmutableCivicDescription.builder()
                    .revision_id(!description.has("revision_id") || description.get("revision_id").isJsonNull()
                            ? null
                            : description.getAsJsonPrimitive("revision_id").getAsString())
                    .value(!description.has("value") || description.get("value").isJsonNull()
                            ? null
                            : description.getAsJsonPrimitive("value").getAsString())
                    .build();
        } else {
            return ImmutableCivicDescription.builder().revision_id("").value("").build();
        }
    }

    @NotNull
    private static List<CivicVariantGroup> createVariantsGroups(@NotNull JsonArray arrayVariantsGroup) {
        List<CivicVariantGroup> civicVariantGroupList = Lists.newArrayList();
        for (JsonElement variantGroup : arrayVariantsGroup) {
            Set<String> keysVariantGroup = variantGroup.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_VARIANTGROUPS_SIZES.contains(keysVariantGroup.size())) {
                LOGGER.warn("Found " + keysVariantGroup.size() + " in civic variant groups rather than the expected "
                        + EXPECTED_CIVIC_VARIANTGROUPS_SIZES);
                LOGGER.warn(keysVariantGroup);
            }
            civicVariantGroupList.add(ImmutableCivicVariantGroup.builder()
                    .id(variantGroup.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .variants(createVariants(variantGroup.getAsJsonObject().getAsJsonArray("variants")))
                    .type(variantGroup.getAsJsonObject().getAsJsonPrimitive("type").getAsString())
                    .description(variantGroup.getAsJsonObject().getAsJsonPrimitive("description").getAsString())
                    .name(variantGroup.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return civicVariantGroupList;
    }

    @NotNull
    private static List<CivicVariants> createVariants(@NotNull JsonArray arrayVariants) {
        List<CivicVariants> variantsList = Lists.newArrayList();
        for (JsonElement variants : arrayVariants) {
            Set<String> keysVariants = variants.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_VARIANTS_SIZES.contains(keysVariants.size())) {
                LOGGER.warn(
                        "Found " + keysVariants.size() + " in civic variants rather than the expected " + EXPECTED_CIVIC_VARIANTS_SIZES);
                LOGGER.warn(keysVariants);
            }

            variantsList.add(ImmutableCivicVariants.builder()
                    .entrez_name(variants.getAsJsonObject().getAsJsonPrimitive("entrez_name").getAsString())
                    .variant_types(createVariantTypes(variants.getAsJsonObject().getAsJsonArray("variant_types")))
                    .description(variants.getAsJsonObject().getAsJsonPrimitive("description").getAsString())
                    .civic_actionability_score(!variants.getAsJsonObject().has("civic_actionability_score") || variants.getAsJsonObject()
                            .get("civic_actionability_score")
                            .isJsonNull() ? null : variants.getAsJsonObject().getAsJsonPrimitive("civic_actionability_score").getAsString())
                    .gene_id(variants.getAsJsonObject().getAsJsonPrimitive("gene_id").getAsString())
                    .entrez_id(variants.getAsJsonObject().getAsJsonPrimitive("entrez_id").getAsString())
                    .coordinates(!variants.getAsJsonObject().has("createCoordinates")
                            ? null
                            : createCoordinates(variants.getAsJsonObject().getAsJsonObject("createCoordinates")))
                    .type(variants.getAsJsonObject().getAsJsonPrimitive("type").getAsString())
                    .id(variants.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .name(variants.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return variantsList;
    }

    @NotNull
    private static CivicCoordinates createCoordinates(@NotNull JsonObject objectCoordinates) {
        Set<String> keysCoordinates = objectCoordinates.keySet();
        if (!EXPECTED_CIVIC_COORDINATES_SIZES.contains(keysCoordinates.size())) {
            LOGGER.warn("Found " + keysCoordinates.size() + " in civic coordinates rather than the expected "
                    + EXPECTED_CIVIC_COORDINATES_SIZES);
            LOGGER.warn(keysCoordinates);
        }

        return ImmutableCivicCoordinates.builder()
                .chromosome2(objectCoordinates.get("chromosome2").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("chromosome2").getAsString())
                .referenceBases(objectCoordinates.get("reference_bases").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("reference_bases").getAsString())
                .start2(objectCoordinates.get("start2").isJsonNull() ? null : objectCoordinates.getAsJsonPrimitive("start2").getAsString())
                .variantBases(objectCoordinates.get("variant_bases").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("variant_bases").getAsString())
                .stop(objectCoordinates.get("stop").isJsonNull() ? null : objectCoordinates.getAsJsonPrimitive("stop").getAsString())
                .stop2(objectCoordinates.get("stop2").isJsonNull() ? null : objectCoordinates.getAsJsonPrimitive("stop2").getAsString())
                .representativeTranscript2(objectCoordinates.get("representative_transcript2").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("representative_transcript2").getAsString())
                .start(objectCoordinates.get("start").isJsonNull() ? null : objectCoordinates.getAsJsonPrimitive("start").getAsString())
                .representativeTranscript(objectCoordinates.get("representative_transcript").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("representative_transcript").getAsString())
                .ensemblVersion(objectCoordinates.get("ensembl_version").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("ensembl_version").getAsString())
                .chromosome(objectCoordinates.get("chromosome").isJsonNull()
                        ? null
                        : objectCoordinates.getAsJsonPrimitive("chromosome").getAsString())
                .referenceBuild("")
                .build();
    }

    @NotNull
    private static List<CivicEvidenceItems> createEvidenceitems(@NotNull JsonArray evidenceItemsArray) {
        List<CivicEvidenceItems> evidenceItemsList = Lists.newArrayList();
        for (JsonElement evideneItem : evidenceItemsArray) {
            Set<String> keysEvidenceItems = evideneItem.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_EVIDENCE_ITEMS_SIZES.contains(keysEvidenceItems.size())) {
                LOGGER.warn("Found " + keysEvidenceItems.size() + " in civic evidence items rather than the expected "
                        + EXPECTED_CIVIC_EVIDENCE_ITEMS_SIZES);
                LOGGER.warn(keysEvidenceItems);
            }

            evidenceItemsList.add(ImmutableCivicEvidenceItems.builder()
                    .status(evideneItem.getAsJsonObject().getAsJsonPrimitive("status").getAsString())
                    .rating(evideneItem.getAsJsonObject().get("rating").isJsonNull()
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("rating").getAsString())
                    .drugInteractionType(evideneItem.getAsJsonObject().get("drug_interaction_type").isJsonNull()
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("drug_interaction_type").getAsString())
                    .description(evideneItem.getAsJsonObject().getAsJsonPrimitive("description").getAsString())
                    .openChangeCount(evideneItem.getAsJsonObject().getAsJsonPrimitive("open_change_count").getAsString())
                    .evidenceType(evideneItem.getAsJsonObject().getAsJsonPrimitive("evidence_type").getAsString())
                    .drugs(createDrugs(evideneItem.getAsJsonObject().getAsJsonArray("drugs")))
                    .variantOrigin(evideneItem.getAsJsonObject().get("variant_origin").isJsonNull()
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("variant_origin").getAsString())
                    .disease(createDiseases(evideneItem.getAsJsonObject().getAsJsonObject("disease")))
                    .source(createSource(evideneItem.getAsJsonObject().getAsJsonObject("source")))
                    .evidenceDirection(evideneItem.getAsJsonObject().get("evidence_direction").isJsonNull()
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("evidence_direction").toString())
                    .variantId(!evideneItem.getAsJsonObject().has("variant_id")
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("variant_id").getAsString())
                    .clinicalSignificance(evideneItem.getAsJsonObject().get("clinical_significance").isJsonNull()
                            ? null
                            : evideneItem.getAsJsonObject().getAsJsonPrimitive("clinical_significance").getAsString())
                    .evidenceLevel(evideneItem.getAsJsonObject().getAsJsonPrimitive("evidence_level").getAsString())
                    .type(evideneItem.getAsJsonObject().getAsJsonPrimitive("type").getAsString())
                    .id(evideneItem.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .name(evideneItem.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return evidenceItemsList;
    }

    @NotNull
    private static CivicSource createSource(@NotNull JsonObject objectSource) {
        Set<String> keysSource = objectSource.keySet();
        if (!EXPECTED_CIVIC_SOURCE_SIZES.contains(keysSource.size())) {
            LOGGER.warn("Found " + keysSource.size() + " in civic source rather than the expected " + EXPECTED_CIVIC_SOURCE_SIZES);
            LOGGER.warn(keysSource);
        }
        return ImmutableCivicSource.builder()
                .status(objectSource.getAsJsonPrimitive("status").getAsString())
                .openAccess(objectSource.get("open_access").isJsonNull()
                        ? null
                        : objectSource.getAsJsonPrimitive("open_access").getAsString())
                .name(objectSource.get("name").isJsonNull() ? null : objectSource.getAsJsonPrimitive("name").getAsString())
                .journal(objectSource.get("journal").isJsonNull() ? null : objectSource.getAsJsonPrimitive("journal").getAsString())
                .citation(objectSource.getAsJsonPrimitive("citation").getAsString())
                .pmc_Id(objectSource.get("pmc_id").isJsonNull() ? null : objectSource.getAsJsonPrimitive("pmc_id").getAsString())
                .fullJournalTitle(objectSource.get("full_journal_title").isJsonNull()
                        ? null
                        : objectSource.getAsJsonPrimitive("full_journal_title").getAsString())
                .sourceUrl(objectSource.getAsJsonPrimitive("source_url").getAsString())
                .clinicalTrials(createCivicClinicalTrials(objectSource.getAsJsonArray("clinical_trials")))
                .pubmedId(objectSource.getAsJsonPrimitive("pubmed_id").getAsString())
                .isReview(objectSource.getAsJsonPrimitive("is_review").getAsString())
                .publicationDate(createPublicationDate(objectSource.getAsJsonObject("publication_date")))
                .id(objectSource.getAsJsonPrimitive("id").getAsString())
                .build();
    }

    @NotNull
    private static List<CivicClinicalTrial> createCivicClinicalTrials(@NotNull JsonArray arrayClinicalTrial) {
        List<CivicClinicalTrial> clinicalTrialList = Lists.newArrayList();
        for (JsonElement clinicalTrial : arrayClinicalTrial) {
            Set<String> keysClinicalTrials = clinicalTrial.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_CLINICALTRIAL_SIZES.contains(keysClinicalTrials.size())) {
                LOGGER.warn("Found " + keysClinicalTrials.size() + " in civic clinial trials rather than the expected "
                        + EXPECTED_CIVIC_CLINICALTRIAL_SIZES);
                LOGGER.warn(keysClinicalTrials);
            }

            clinicalTrialList.add(ImmutableCivicClinicalTrial.builder()
                    .nct_id(clinicalTrial.getAsJsonObject().getAsJsonPrimitive("nct_id").getAsString())
                    .description(clinicalTrial.getAsJsonObject().getAsJsonPrimitive("description").getAsString())
                    .clinical_trial_url(clinicalTrial.getAsJsonObject().getAsJsonPrimitive("clinical_trial_url").getAsString())
                    .name(clinicalTrial.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return clinicalTrialList;
    }

    @NotNull
    private static CivicPublicationDate createPublicationDate(@NotNull JsonObject objectPublicationDate) {
        Set<String> keysPublicationDate = objectPublicationDate.keySet();
        if (!EXPECTED_CIVIC_PUBLICATIONS_DATE_SIZES.contains(keysPublicationDate.size())) {
            LOGGER.warn("Found " + keysPublicationDate.size() + " in civic publication date rather than the expected "
                    + EXPECTED_CIVIC_PUBLICATIONS_DATE_SIZES);
            LOGGER.warn(keysPublicationDate);
        }

        return ImmutableCivicPublicationDate.builder()
                .year(!objectPublicationDate.has("year") ? null : objectPublicationDate.getAsJsonPrimitive("year").getAsString())
                .day(!objectPublicationDate.has("day") ? null : objectPublicationDate.getAsJsonPrimitive("day").getAsString())
                .month(!objectPublicationDate.has("month") ? null : objectPublicationDate.getAsJsonPrimitive("month").getAsString())
                .build();
    }

    @NotNull
    private static CivicDisease createDiseases(@NotNull JsonObject objectDisease) {
        Set<String> keysDisease = objectDisease.keySet();
        if (!EXPECTED_CIVIC_DISEASES_SIZES.contains(keysDisease.size())) {
            LOGGER.warn("Found " + keysDisease.size() + " in civic diseases rather than the expected " + EXPECTED_CIVIC_DISEASES_SIZES);
            LOGGER.warn(keysDisease);
        }

        return ImmutableCivicDisease.builder()
                .doid(objectDisease.get("doid").isJsonNull() ? null : objectDisease.getAsJsonPrimitive("doid").getAsString())
                .url(objectDisease.getAsJsonPrimitive("url").getAsString())
                .displayName(objectDisease.getAsJsonPrimitive("display_name").getAsString())
                .id(objectDisease.getAsJsonPrimitive("id").getAsString())
                .name(objectDisease.getAsJsonPrimitive("name").getAsString())
                .build();
    }

    @NotNull
    private static List<CivicDrugs> createDrugs(@NotNull JsonArray arrayDrugs) {
        List<CivicDrugs> drugsList = Lists.newArrayList();
        for (JsonElement drug : arrayDrugs) {
            Set<String> keysDrugs = drug.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_DRUGS_SIZES.contains(keysDrugs.size())) {
                LOGGER.warn("Found " + keysDrugs.size() + " in civic drugs rather than the expected " + EXPECTED_CIVIC_DRUGS_SIZES);
                LOGGER.warn(keysDrugs);
            }

            drugsList.add(ImmutableCivicDrugs.builder()
                    .pubchemId(drug.getAsJsonObject().get("pubchem_id").isJsonNull()
                            ? null
                            : drug.getAsJsonObject().getAsJsonPrimitive("pubchem_id").getAsString())
                    .id(drug.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .name(drug.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return drugsList;
    }

    @NotNull
    private static CivicLifecycleActions createLifeCycleActions(@NotNull JsonObject objectLifeCycleActions) {
        Set<String> keysLifecycleActions = objectLifeCycleActions.keySet();
        if (!EXPECTED_CIVIC_LIFECYCLE_ACTIONS_SIZES.contains(keysLifecycleActions.size())) {
            LOGGER.warn("Found " + keysLifecycleActions.size() + " in civic lifecycle actions rather than the expected "
                    + EXPECTED_CIVIC_LIFECYCLE_ACTIONS_SIZES);
            LOGGER.warn(keysLifecycleActions);
        }

        return ImmutableCivicLifecycleActions.builder()
                .lastCommentedOn(objectLifeCycleActions.getAsJsonObject("last_commented_on") == null
                        ? null
                        : createLastCommentOn(objectLifeCycleActions.getAsJsonObject("last_commented_on")))
                .lastModified(objectLifeCycleActions.getAsJsonObject("last_modified") == null
                        ? null
                        : createLastModified(objectLifeCycleActions.getAsJsonObject("last_modified")))
                .lastReviewed(objectLifeCycleActions.getAsJsonObject("last_reviewed") == null
                        ? null
                        : createLastReviewed(objectLifeCycleActions.getAsJsonObject("last_reviewed")))
                .build();
    }

    @NotNull
    private static CivicLastCommentedOn createLastCommentOn(@NotNull JsonObject objectLastCommned) {
        Set<String> keysLastCommentedOn = objectLastCommned.keySet();
        if (!EXPECTED_CIVIC_LAST_COMMENTED_ON_SIZES.contains(keysLastCommentedOn.size())) {
            LOGGER.warn("Found " + keysLastCommentedOn.size() + " in civic last commented on rather than the expected "
                    + EXPECTED_CIVIC_LAST_COMMENTED_ON_SIZES);
            LOGGER.warn(keysLastCommentedOn);
        }

        return ImmutableCivicLastCommentedOn.builder()
                .timestamp(objectLastCommned.getAsJsonPrimitive("timestamp").getAsString())
                .user(createCivicUser(objectLastCommned.getAsJsonObject("user")))
                .build();
    }

    @NotNull
    private static CivicLastModified createLastModified(@NotNull JsonObject objectLastModified) {
        Set<String> keysLastModified = objectLastModified.keySet();
        if (!EXPECTED_CIVIC_LAST_MODIFIED_SIZES.contains(keysLastModified.size())) {
            LOGGER.warn("Found " + keysLastModified.size() + " in civic last modified rather than the expected"
                    + EXPECTED_CIVIC_LAST_MODIFIED_SIZES);
            LOGGER.warn(keysLastModified);
        }

        return ImmutableCivicLastModified.builder()
                .timestamp(objectLastModified.getAsJsonPrimitive("timestamp").getAsString())
                .user(createCivicUser(objectLastModified.getAsJsonObject("user")))
                .build();
    }

    @NotNull
    private static CivicLastReviewed createLastReviewed(@NotNull JsonObject objectLastReviewed) {
        Set<String> keysLastReviewed = objectLastReviewed.keySet();
        if (!EXPECTED_CIVIC_LAST_REVIEWED_SIZES.contains(keysLastReviewed.size())) {
            LOGGER.warn("Found " + keysLastReviewed.size() + " in civic last reviewed rather than the expected "
                    + EXPECTED_CIVIC_LAST_REVIEWED_SIZES);
            LOGGER.warn(keysLastReviewed);
        }

        return ImmutableCivicLastReviewed.builder()
                .timestamp(objectLastReviewed.getAsJsonPrimitive("timestamp").getAsString())
                .user(createCivicUser(objectLastReviewed.getAsJsonObject("user")))
                .build();
    }

    @NotNull
    private static CivicUser createCivicUser(@NotNull JsonObject objectUser) {
        Set<String> keysUser = objectUser.keySet();
        if (!EXPECTED_CIVIC_USER_SIZES.contains(keysUser.size())) {
            LOGGER.warn("Found " + keysUser.size() + " in civic user rather than the expected " + EXPECTED_CIVIC_USER_SIZES);
            LOGGER.warn(keysUser);
        }

        return ImmutableCivicUser.builder()
                .username(objectUser.getAsJsonPrimitive("username").getAsString())
                .areaOfExpertise(objectUser.get("area_of_expertise").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("area_of_expertise").getAsString())
                .organization(createOrganization(objectUser.getAsJsonObject("organization")))
                .twitterHandle(objectUser.get("twitter_handle").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("twitter_handle").getAsString())
                .name(objectUser.getAsJsonPrimitive("name").getAsString())
                .bio(objectUser.get("bio").isJsonNull() ? null : objectUser.getAsJsonPrimitive("bio").getAsString())
                .url(objectUser.get("url").isJsonNull() ? null : objectUser.getAsJsonPrimitive("url").getAsString())
                .createdAt(objectUser.getAsJsonPrimitive("created_at").getAsString())
                .avatars(createAvatars(objectUser.getAsJsonObject("avatars")))
                .acceptedLicense(objectUser.get("accepted_license").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("accepted_license").getAsString())
                .affiliation(objectUser.get("affiliation").isJsonNull() ? null : objectUser.getAsJsonPrimitive("affiliation").getAsString())
                .avatarUrl(objectUser.getAsJsonPrimitive("avatar_url").getAsString())
                .role(objectUser.getAsJsonPrimitive("role").getAsString())
                .facebookProfile(objectUser.get("facebook_profile").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("facebook_profile").getAsString())
                .linkedinProfile(objectUser.get("linkedin_profile").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("linkedin_profile").getAsString())
                .orcid(objectUser.get("orcid").isJsonNull() ? null : objectUser.getAsJsonPrimitive("orcid").getAsString())
                .displayName(objectUser.getAsJsonPrimitive("display_name").getAsString())
                .lastSeenAt(objectUser.get("last_seen_at").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("last_seen_at").getAsString())
                .featuredExpert(objectUser.getAsJsonPrimitive("featured_expert").getAsString())
                .id(objectUser.getAsJsonPrimitive("id").getAsString())
                .signupComplete(objectUser.get("signup_complete").isJsonNull()
                        ? null
                        : objectUser.getAsJsonPrimitive("signup_complete").getAsString())
                .build();
    }

    @NotNull
    private static CivicOrganization createOrganization(@NotNull JsonObject objectOrganization) {
        Set<String> keysOrganization = objectOrganization.keySet();
        if (!EXPECTED_CIVIC_ORGANIZATION_SIZES.contains(keysOrganization.size())) {
            LOGGER.warn("Found " + keysOrganization.size() + " in civic organization rather than the expected "
                    + EXPECTED_CIVIC_ORGANIZATION_SIZES);
            LOGGER.warn(keysOrganization);
        }

        return ImmutableCivicOrganization.builder()
                .url(!objectOrganization.has("url") ? null : objectOrganization.getAsJsonPrimitive("url").getAsString())
                .id(!objectOrganization.has("id") ? null : objectOrganization.getAsJsonPrimitive("id").getAsString())
                .profileImage(!objectOrganization.has("profile_image")
                        ? null
                        : createProfileImage(objectOrganization.getAsJsonObject("profile_image")))
                .description(!objectOrganization.has("description")
                        ? null
                        : objectOrganization.getAsJsonPrimitive("description").getAsString())
                .name(!objectOrganization.has("name") ? null : objectOrganization.getAsJsonPrimitive("name").getAsString())
                .build();
    }

    @NotNull
    private static CivicProfileImage createProfileImage(@NotNull JsonObject objectProfileImage) {
        Set<String> keysProfileImage = objectProfileImage.keySet();
        if (!EXPECTED_CIVIC_PROFILE_IMAGE_SIZES.contains(keysProfileImage.size())) {
            LOGGER.warn("Found " + keysProfileImage.size() + " in civic profile image rather than the expected "
                    + EXPECTED_CIVIC_PROFILE_IMAGE_SIZES);
            LOGGER.warn(keysProfileImage);
        }

        return ImmutableCivicProfileImage.builder()
                .x32(objectProfileImage.getAsJsonPrimitive("x32").getAsString())
                .x256(objectProfileImage.getAsJsonPrimitive("x256").getAsString())
                .x14(objectProfileImage.getAsJsonPrimitive("x14").getAsString())
                .x64(objectProfileImage.getAsJsonPrimitive("x64").getAsString())
                .x128(objectProfileImage.getAsJsonPrimitive("x128").getAsString())
                .build();
    }

    @NotNull
    private static CivicAvatars createAvatars(@NotNull JsonObject objectAvatars) {
        Set<String> keysAvatars = objectAvatars.keySet();
        if (!EXPECTED_CIVIC_AVATARS_SIZES.contains(keysAvatars.size())) {
            LOGGER.warn("Found " + keysAvatars.size() + " in civic avatars rather than the expected " + EXPECTED_CIVIC_AVATARS_SIZES);
            LOGGER.warn(keysAvatars);
        }

        return ImmutableCivicAvatars.builder()
                .x32(objectAvatars.getAsJsonPrimitive("x32").getAsString())
                .x14(objectAvatars.getAsJsonPrimitive("x14").getAsString())
                .x64(objectAvatars.getAsJsonPrimitive("x64").getAsString())
                .x128(objectAvatars.getAsJsonPrimitive("x128").getAsString())
                .build();
    }

    @NotNull
    private static List<CivicVariantTypes> createVariantTypes(@NotNull JsonArray arrayvariantTypes) {
        List<CivicVariantTypes> civicVariantTypesList = Lists.newArrayList();
        for (JsonElement variantTypes : arrayvariantTypes) {
            Set<String> keysVariantTypes = variantTypes.getAsJsonObject().keySet();
            if (!EXPECTED_CIVIC_VARIANTTYPES_SIZES.contains(keysVariantTypes.size())) {
                LOGGER.warn("Found " + keysVariantTypes.size() + " in civic variant types rather than the expected "
                        + EXPECTED_CIVIC_VARIANTTYPES_SIZES);
                LOGGER.warn(keysVariantTypes);
            }

            civicVariantTypesList.add(ImmutableCivicVariantTypes.builder()
                    .displayName(variantTypes.getAsJsonObject().getAsJsonPrimitive("display_name").getAsString())
                    .description(variantTypes.getAsJsonObject().getAsJsonPrimitive("description").getAsString())
                    .url(variantTypes.getAsJsonObject().getAsJsonPrimitive("url").getAsString())
                    .soId(variantTypes.getAsJsonObject().getAsJsonPrimitive("so_id").getAsString())
                    .id(variantTypes.getAsJsonObject().getAsJsonPrimitive("id").getAsString())
                    .name(variantTypes.getAsJsonObject().getAsJsonPrimitive("name").getAsString())
                    .build());
        }
        return civicVariantTypesList;
    }

}