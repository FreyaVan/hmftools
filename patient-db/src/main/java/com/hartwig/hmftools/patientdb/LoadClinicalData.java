package com.hartwig.hmftools.patientdb;

import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.DB_URL;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.addDatabaseCmdLineArgs;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.databaseAccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.doid.DiseaseOntology;
import com.hartwig.hmftools.common.doid.DoidNode;
import com.hartwig.hmftools.common.ecrf.EcrfModel;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfPatient;
import com.hartwig.hmftools.common.ecrf.datamodel.ValidationFinding;
import com.hartwig.hmftools.common.ecrf.formstatus.FormStatusModel;
import com.hartwig.hmftools.common.ecrf.formstatus.FormStatusReader;
import com.hartwig.hmftools.common.lims.Lims;
import com.hartwig.hmftools.common.lims.LimsFactory;
import com.hartwig.hmftools.common.lims.LimsStudy;
import com.hartwig.hmftools.patientdb.context.RunContext;
import com.hartwig.hmftools.patientdb.curators.BiopsySiteCurator;
import com.hartwig.hmftools.patientdb.curators.PrimaryTumorCurator;
import com.hartwig.hmftools.patientdb.curators.TreatmentCurator;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;
import com.hartwig.hmftools.patientdb.data.Patient;
import com.hartwig.hmftools.patientdb.data.SampleData;
import com.hartwig.hmftools.patientdb.readers.ColoPatientReader;
import com.hartwig.hmftools.patientdb.readers.CorePatientReader;
import com.hartwig.hmftools.patientdb.readers.EcrfPatientReader;
import com.hartwig.hmftools.patientdb.readers.LimsSampleReader;
import com.hartwig.hmftools.patientdb.readers.RunsFolderReader;
import com.hartwig.hmftools.patientdb.readers.WidePatientReader;
import com.hartwig.hmftools.patientdb.readers.cpct.CpctPatientReader;
import com.hartwig.hmftools.patientdb.readers.cpct.CpctUtil;
import com.hartwig.hmftools.patientdb.readers.drup.DrupPatientReader;
import com.hartwig.hmftools.patientdb.readers.wide.ImmutableWideEcrfModel;
import com.hartwig.hmftools.patientdb.readers.wide.WideAvlTreatmentData;
import com.hartwig.hmftools.patientdb.readers.wide.WideBiopsyData;
import com.hartwig.hmftools.patientdb.readers.wide.WideEcrfFileReader;
import com.hartwig.hmftools.patientdb.readers.wide.WideEcrfModel;
import com.hartwig.hmftools.patientdb.readers.wide.WideFiveDays;
import com.hartwig.hmftools.patientdb.readers.wide.WidePreAvlTreatmentData;
import com.hartwig.hmftools.patientdb.readers.wide.WideResponseData;
import com.hartwig.hmftools.patientdb.validators.CurationValidator;
import com.hartwig.hmftools.patientdb.validators.PatientValidator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LoadClinicalData {

    private static final Logger LOGGER = LogManager.getLogger(LoadClinicalData.class);
    private static final String VERSION = LoadClinicalData.class.getPackage().getImplementationVersion();
    private static final String PIPELINE_VERSION = "pipeline_version_file";

    private static final String RUNS_DIRECTORY = "runs_dir";

    private static final String CPCT_ECRF_FILE = "cpct_ecrf";
    private static final String CPCT_FORM_STATUS_CSV = "cpct_form_status_csv";
    private static final String DRUP_ECRF_FILE = "drup_ecrf";

    private static final String DO_LOAD_CLINICAL_DATA = "do_load_clinical_data";
    private static final String DO_LOAD_RAW_ECRF = "do_load_raw_ecrf";

    private static final String CURATED_PRIMARY_TUMOR_TSV = "curated_primary_tumor_tsv";

    private static final String DO_PROCESS_WIDE_CLINICAL_DATA = "do_process_wide_clinical_data";
    private static final String WIDE_PRE_AVL_TREATMENT_CSV = "wide_pre_avl_treatment_csv";
    private static final String WIDE_BIOPSY_CSV = "wide_biopsy_csv";
    private static final String WIDE_AVL_TREATMENT_CSV = "wide_avl_treatment_csv";
    private static final String WIDE_RESPONSE_CSV = "wide_response_csv";
    private static final String WIDE_FIVE_DAYS_CSV = "wide_five_days_csv";

    private static final String LIMS_DIRECTORY = "lims_dir";

    private static final String DOID_JSON = "doid_json";
    private static final String TUMOR_LOCATION_MAPPING_TSV = "tumor_location_mapping_tsv";
    private static final String TREATMENT_MAPPING_CSV = "treatment_mapping_csv";
    private static final String BIOPSY_MAPPING_CSV = "biopsy_mapping_csv";

    public static void main(@NotNull String[] args) throws ParseException, IOException, XMLStreamException, SQLException {
        LOGGER.info("Running Clinical Patient DB v{}", VERSION);
        Options options = createOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);

        if (!checkInputs(cmd)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("patient-db", options);
            System.exit(1);
        }

        List<DoidNode> doidNodes = DiseaseOntology.readDoidOwlEntryFromDoidJson(cmd.getOptionValue(DOID_JSON)).nodes();
        PrimaryTumorCurator primaryTumorCurator = new PrimaryTumorCurator(cmd.getOptionValue(TUMOR_LOCATION_MAPPING_TSV), doidNodes);
        BiopsySiteCurator biopsySiteCurator = new BiopsySiteCurator(cmd.getOptionValue(BIOPSY_MAPPING_CSV));
        TreatmentCurator treatmentCurator = new TreatmentCurator(cmd.getOptionValue(TREATMENT_MAPPING_CSV));

        LOGGER.info("Loading sequence runs from {}", cmd.getOptionValue(RUNS_DIRECTORY));
        List<RunContext> runContexts = loadRunContexts(cmd.getOptionValue(RUNS_DIRECTORY), cmd.getOptionValue(PIPELINE_VERSION));
        Map<String, List<String>> sequencedSamplesPerPatient = extractSequencedSamplesFromRunContexts(runContexts);
        Map<String, String> sampleToSetNameMap = extractSampleToSetNameMap(runContexts);
        Set<String> sequencedPatientIds = sequencedSamplesPerPatient.keySet();

        LOGGER.info(" Loaded sequence runs for {} patient IDs ({} samples)",
                sequencedPatientIds.size(),
                toUniqueSampleIds(sequencedSamplesPerPatient).size());

        LOGGER.info("Loading sample data from LIMS in {}", cmd.getOptionValue(LIMS_DIRECTORY));
        Lims lims = LimsFactory.fromLimsDirectory(cmd.getOptionValue(LIMS_DIRECTORY));
        Map<String, List<SampleData>> sampleDataPerPatient =
                extractAllSamplesFromLims(lims, sampleToSetNameMap, sequencedSamplesPerPatient);
        LOGGER.info(" Loaded samples for {} patient IDs ({} samples)",
                sampleDataPerPatient.keySet().size(),
                countValues(sampleDataPerPatient));

        EcrfModels ecrfModels = loadEcrfModels(cmd);

        Map<String, Patient> patients =
                loadAndInterpretPatients(sampleDataPerPatient, ecrfModels, primaryTumorCurator, biopsySiteCurator, treatmentCurator);

        LOGGER.info("Writing curated primary tumors");
        DumpPrimaryTumorData.writeCuratedPrimaryTumorsToTSV(cmd.getOptionValue(CURATED_PRIMARY_TUMOR_TSV), patients.values());

        if (cmd.hasOption(DO_LOAD_CLINICAL_DATA)) {
            LOGGER.info("Connecting to database {}", cmd.getOptionValue(DB_URL));
            DatabaseAccess dbWriter = databaseAccess(cmd);

            if (cmd.hasOption(DO_LOAD_RAW_ECRF)) {
                writeRawEcrf(dbWriter, sequencedPatientIds, ecrfModels);
            }

            writeClinicalData(dbWriter, lims, sequencedPatientIds, sampleDataPerPatient, patients);

            dbWriter.writeValidationFindings(CurationValidator.validatePrimaryTumorCurator(primaryTumorCurator));
            dbWriter.writeValidationFindings(CurationValidator.validateTreatmentCurator(treatmentCurator));
        }

        LOGGER.info("Complete");
    }

    @NotNull
    private static List<RunContext> loadRunContexts(@NotNull String runsDirectory, @NotNull String pipelineVersionFile) throws IOException {
        List<RunContext> runContexts = RunsFolderReader.extractRunContexts(new File(runsDirectory), pipelineVersionFile);
        LOGGER.info(" Loaded run contexts from {} ({} sets)", runsDirectory, runContexts.size());

        return runContexts;
    }

    @NotNull
    private static Map<String, String> extractSampleToSetNameMap(@NotNull List<RunContext> runContexts) {
        Map<String, String> sampleToSetNameMap = Maps.newHashMap();
        for (RunContext runContext : runContexts) {
            if (sampleToSetNameMap.containsKey(runContext.tumorSample())) {
                LOGGER.warn("Duplicate sample ID found in run contexts: {}", runContext.tumorSample());
            }
            sampleToSetNameMap.put(runContext.tumorSample(), runContext.setName());
        }
        return sampleToSetNameMap;
    }

    @NotNull
    private static Map<String, List<String>> extractSequencedSamplesFromRunContexts(@NotNull List<RunContext> runContexts) {
        Map<String, List<String>> sequencedSamplesPerPatient = Maps.newHashMap();
        for (RunContext runContext : runContexts) {
            String patientId = Utils.extractPatientIdentifier(runContext.setName());
            List<String> currentSampleIds = sequencedSamplesPerPatient.get(patientId);
            if (currentSampleIds == null) {
                currentSampleIds = Lists.newArrayList(runContext.tumorSample());
            } else {
                currentSampleIds.add(runContext.tumorSample());
            }
            sequencedSamplesPerPatient.put(patientId, currentSampleIds);
        }

        return sequencedSamplesPerPatient;
    }

    @NotNull
    private static Map<String, List<SampleData>> extractAllSamplesFromLims(@NotNull Lims lims,
            @NotNull Map<String, String> sampleToSetNameMap, @NotNull Map<String, List<String>> sequencedSamplesPerPatient) {
        LimsSampleReader sampleReader = new LimsSampleReader(lims, sampleToSetNameMap, toUniqueSampleIds(sequencedSamplesPerPatient));

        Map<String, List<SampleData>> samplesPerPatient = Maps.newHashMap();
        for (String sampleBarcode : lims.sampleBarcodes()) {
            String sampleId = lims.sampleId(sampleBarcode);
            LimsStudy study = LimsStudy.fromSampleId(sampleId);

            if (study != LimsStudy.NON_CANCER_STUDY) {
                String patientId = lims.patientId(sampleBarcode);
                SampleData sampleData = sampleReader.read(sampleBarcode, sampleId);

                if (sampleData != null) {
                    List<SampleData> currentSamples = samplesPerPatient.get(patientId);
                    if (currentSamples == null) {
                        currentSamples = Lists.newArrayList(sampleData);
                    } else if (!sampleIdExistsInSampleDataList(currentSamples, sampleId)) {
                        // TODO If a single sample exists in LIMS with multiple barcodes we should pick "the most relevant one".
                        // Currently just picking a random one - sampleId has to be unique in this list.
                        currentSamples.add(sampleData);
                    }
                    samplesPerPatient.put(patientId, currentSamples);
                }
            }
        }

        // Some samples may be missing from LIMS simply because they are old and we did not collect information back in those days.
        for (Map.Entry<String, List<String>> sequencedPatientEntry : sequencedSamplesPerPatient.entrySet()) {
            List<SampleData> samples = samplesPerPatient.get(sequencedPatientEntry.getKey());
            if (samples == null) {
                samples = Lists.newArrayList();
            }
            for (String sampleId : sequencedPatientEntry.getValue()) {
                if (!sampleIdExistsInSampleDataList(samples, sampleId)) {
                    LOGGER.info(" Creating sample data for {}. This sample is not found in LIMS even though it has been sequenced!",
                            sampleId);
                    SampleData sampleData = sampleReader.readSequencedSampleWithoutBarcode(sampleId);
                    if (sampleData != null) {
                        samples.add(sampleData);
                    }
                }
            }
            samplesPerPatient.put(sequencedPatientEntry.getKey(), samples);
        }

        return samplesPerPatient;
    }

    private static boolean sampleIdExistsInSampleDataList(@NotNull List<SampleData> samples, @NotNull String sampleId) {
        for (SampleData sample : samples) {
            if (sample.sampleId().equals(sampleId)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private static EcrfModels loadEcrfModels(@NotNull CommandLine cmd) throws IOException, XMLStreamException {
        EcrfModel cpctEcrfModel = buildCpctEcrfModel(cmd);
        EcrfModel drupEcrfModel = buildDrupEcrfModel(cmd);
        WideEcrfModel wideEcrfModel = buildWideEcrfModel(cmd);

        return ImmutableEcrfModels.builder().cpctModel(cpctEcrfModel).drupModel(drupEcrfModel).wideModel(wideEcrfModel).build();
    }

    @NotNull
    private static EcrfModel buildCpctEcrfModel(@NotNull CommandLine cmd) throws IOException, XMLStreamException {
        String cpctEcrfFilePath = cmd.getOptionValue(CPCT_ECRF_FILE);
        String cpctFormStatusCsv = cmd.getOptionValue(CPCT_FORM_STATUS_CSV);
        LOGGER.info("Loading CPCT eCRF from {}", cpctEcrfFilePath);
        FormStatusModel cpctFormStatusModel = FormStatusReader.buildModelFromCsv(cpctFormStatusCsv);
        EcrfModel cpctEcrfModel = EcrfModel.loadFromXMLWithFormStates(cpctEcrfFilePath, cpctFormStatusModel);
        LOGGER.info(" Finished loading CPCT eCRF. Read {} patients", cpctEcrfModel.patientCount());

        return cpctEcrfModel;
    }

    @NotNull
    private static EcrfModel buildDrupEcrfModel(@NotNull CommandLine cmd) throws FileNotFoundException, XMLStreamException {
        String drupEcrfFilePath = cmd.getOptionValue(DRUP_ECRF_FILE);
        LOGGER.info("Loading DRUP eCRF from {}", drupEcrfFilePath);
        EcrfModel drupEcrfModel = EcrfModel.loadFromXMLNoFormStates(drupEcrfFilePath);
        LOGGER.info(" Finished loading DRUP eCRF. Read {} patients", drupEcrfModel.patientCount());

        return drupEcrfModel;
    }

    @NotNull
    private static WideEcrfModel buildWideEcrfModel(@NotNull CommandLine cmd) throws IOException {
        WideEcrfModel wideEcrfModel;

        if (cmd.hasOption(DO_PROCESS_WIDE_CLINICAL_DATA)) {
            LOGGER.info("Loading WIDE eCRF");

            String preAvlTreatmentCsv = cmd.getOptionValue(WIDE_PRE_AVL_TREATMENT_CSV);
            List<WidePreAvlTreatmentData> preAvlTreatments = WideEcrfFileReader.readPreAvlTreatments(preAvlTreatmentCsv);
            LOGGER.info(" Loaded {} WIDE pre-AVL-treatments from {}", preAvlTreatments.size(), preAvlTreatmentCsv);

            String biopsyCsv = cmd.getOptionValue(WIDE_BIOPSY_CSV);
            List<WideBiopsyData> biopsies = WideEcrfFileReader.readBiopsies(biopsyCsv);
            LOGGER.info(" Loaded {} WIDE biopsies from {}", biopsies.size(), biopsyCsv);

            String avlTreatmentCsv = cmd.getOptionValue(WIDE_AVL_TREATMENT_CSV);
            List<WideAvlTreatmentData> avlTreatments = WideEcrfFileReader.readAvlTreatments(avlTreatmentCsv);
            LOGGER.info(" Loaded {} WIDE AVL treatments from {}", avlTreatments.size(), avlTreatmentCsv);

            String wideResponseCsv = cmd.getOptionValue(WIDE_RESPONSE_CSV);
            List<WideResponseData> responses = WideEcrfFileReader.readResponses(wideResponseCsv);
            LOGGER.info(" Loaded {} WIDE responses from {}", responses.size(), wideResponseCsv);

            String fiveDaysCsv = cmd.getOptionValue(WIDE_FIVE_DAYS_CSV);
            List<WideFiveDays> fiveDays = WideEcrfFileReader.readFiveDays(fiveDaysCsv);
            LOGGER.info(" Loaded {} WIDE five days entries from {}", fiveDays.size(), fiveDaysCsv);

            wideEcrfModel = ImmutableWideEcrfModel.builder()
                    .preAvlTreatments(preAvlTreatments)
                    .biopsies(biopsies)
                    .avlTreatments(avlTreatments)
                    .responses(responses)
                    .fiveDays(fiveDays)
                    .build();
        } else {
            LOGGER.info("Skipping the loading of WIDE eCRF");
            wideEcrfModel = ImmutableWideEcrfModel.builder()
                    .preAvlTreatments(Lists.newArrayList())
                    .biopsies(Lists.newArrayList())
                    .avlTreatments(Lists.newArrayList())
                    .responses(Lists.newArrayList())
                    .fiveDays(Lists.newArrayList())
                    .build();
        }

        return wideEcrfModel;
    }

    private static void writeRawEcrf(@NotNull DatabaseAccess dbWriter, @NotNull Set<String> sequencedPatients,
            @NotNull EcrfModels ecrfModels) {
        EcrfModel cpctEcrfModel = ecrfModels.cpctModel();
        LOGGER.info("Writing raw cpct ecrf data for {} patients", cpctEcrfModel.patientCount());
        dbWriter.clearCpctEcrf();
        dbWriter.writeCpctEcrf(cpctEcrfModel, sequencedPatients);
        LOGGER.info(" Finished writing raw cpct ecrf data for {} patients", cpctEcrfModel.patientCount());

        EcrfModel drupEcrfModel = ecrfModels.drupModel();
        LOGGER.info("Writing raw drup ecrf data for {} patients", drupEcrfModel.patientCount());
        dbWriter.clearDrupEcrf();
        dbWriter.writeDrupEcrf(drupEcrfModel, sequencedPatients);
        LOGGER.info(" Finished writing raw drup ecrf data for {} patients", drupEcrfModel.patientCount());
    }

    private static void writeClinicalData(@NotNull DatabaseAccess dbAccess, @NotNull Lims lims, @NotNull Set<String> sequencedPatientIds,
            @NotNull Map<String, List<SampleData>> sampleDataPerPatient, @NotNull Map<String, Patient> patients) {
        LOGGER.info("Clearing interpreted clinical tables in database");
        dbAccess.clearClinicalTables();

        int missingPatients = 0;
        int missingSamples = 0;
        LOGGER.info("Writing clinical data for {} sequenced patients", sequencedPatientIds.size());
        for (String patientId : sequencedPatientIds) {
            Patient patient = patients.get(patientId);
            if (patient == null) {
                LOGGER.warn("No clinical data found for patient {}", patientId);
                missingPatients++;
                List<SampleData> sequencedSamples = sequencedOnly(sampleDataPerPatient.get(patientId));
                missingSamples += sequencedSamples.size();
                dbAccess.writeSampleClinicalData(patientId, lims.isBlacklisted(patientId), sequencedSamples);
            } else if (patient.sequencedBiopsies().isEmpty()) {
                LOGGER.warn("No sequenced biopsies found for sequenced patient: {}! Skipping writing to db", patientId);
            } else {
                dbAccess.writeFullClinicalData(patient, lims.isBlacklisted(patientId));
                List<ValidationFinding> findings = PatientValidator.validatePatient(patient);

                dbAccess.writeValidationFindings(findings);
                dbAccess.writeValidationFindings(patient.matchFindings());
            }
        }

        if (missingPatients > 0) {
            LOGGER.warn("Could not load {} patients ({} samples)!", missingPatients, missingSamples);
        }
    }

    @NotNull
    private static Map<String, Patient> loadAndInterpretPatients(@NotNull Map<String, List<SampleData>> sampleDataPerPatient,
            @NotNull EcrfModels ecrfModels, @NotNull PrimaryTumorCurator primaryTumorCurator,
            @NotNull BiopsySiteCurator biopsySiteCurator, @NotNull TreatmentCurator treatmentCurator) {
        EcrfModel cpctEcrfModel = ecrfModels.cpctModel();
        LOGGER.info("Interpreting and curating data for {} CPCT patients", cpctEcrfModel.patientCount());
        EcrfPatientReader cpctPatientReader = new CpctPatientReader(primaryTumorCurator,
                CpctUtil.extractHospitalMap(cpctEcrfModel),
                biopsySiteCurator,
                treatmentCurator);

        Map<String, Patient> cpctPatients = readEcrfPatients(cpctPatientReader, cpctEcrfModel.patients(), sampleDataPerPatient);
        LOGGER.info(" Finished curation of {} CPCT patients", cpctPatients.size());

        EcrfModel drupEcrfModel = ecrfModels.drupModel();
        LOGGER.info("Interpreting and curating data for {} DRUP patients", drupEcrfModel.patientCount());
        EcrfPatientReader drupPatientReader = new DrupPatientReader(primaryTumorCurator, biopsySiteCurator);

        Map<String, Patient> drupPatients = readEcrfPatients(drupPatientReader, drupEcrfModel.patients(), sampleDataPerPatient);
        LOGGER.info(" Finished curation of {} DRUP patients", drupPatients.size());

        LOGGER.info("Interpreting and curating data for WIDE patients");
        Map<String, Patient> widePatients =
                readWidePatients(ecrfModels.wideModel(), sampleDataPerPatient, primaryTumorCurator, treatmentCurator);
        LOGGER.info(" Finished curation of {} WIDE patients", widePatients.size());

        LOGGER.info("Interpreting and curating data for CORE patients");
        Map<String, Patient> corePatients = readCorePatients(sampleDataPerPatient, primaryTumorCurator);
        LOGGER.info(" Finished curation of {} CORE patients", corePatients.size());

        Map<String, Patient> mergedPatients = Maps.newHashMap();
        mergedPatients.putAll(cpctPatients);
        mergedPatients.putAll(drupPatients);
        mergedPatients.putAll(widePatients);
        mergedPatients.putAll(corePatients);
        mergedPatients.putAll(readColoPatients());
        return mergedPatients;
    }

    @NotNull
    private static Map<String, Patient> readEcrfPatients(@NotNull EcrfPatientReader reader, @NotNull Iterable<EcrfPatient> patients,
            @NotNull Map<String, List<SampleData>> sampleDataPerPatient) {
        Map<String, Patient> patientMap = Maps.newHashMap();
        for (EcrfPatient ecrfPatient : patients) {
            List<SampleData> sequencedSamples = sequencedOnly(sampleDataPerPatient.get(ecrfPatient.patientId()));
            Patient patient = reader.read(ecrfPatient, sequencedSamples);
            patientMap.put(patient.patientIdentifier(), patient);
        }
        return patientMap;
    }

    @NotNull
    private static Map<String, Patient> readWidePatients(@NotNull WideEcrfModel wideEcrfModel,
            @NotNull Map<String, List<SampleData>> sampleDataPerPatient, @NotNull PrimaryTumorCurator primaryTumorCurator,
            @NotNull TreatmentCurator treatmentCurator) {
        Map<String, Patient> patientMap = Maps.newHashMap();

        WidePatientReader widePatientReader = new WidePatientReader(wideEcrfModel, primaryTumorCurator, treatmentCurator);
        for (Map.Entry<String, List<SampleData>> entry : sampleDataPerPatient.entrySet()) {
            List<SampleData> samples = entry.getValue();

            assert samples != null;
            List<SampleData> tumorSamples = extractTumorSamples(samples);
            if (!tumorSamples.isEmpty()) {
                LimsStudy study = LimsStudy.fromSampleId(tumorSamples.get(0).sampleId());

                if (study == LimsStudy.WIDE) {
                    String patientId = entry.getKey();
                    Patient widePatient =
                            widePatientReader.read(patientId, tumorSamples.get(0).limsPrimaryTumor(), sequencedOnly(tumorSamples));
                    patientMap.put(patientId, widePatient);
                }
            }
        }
        return patientMap;
    }

    @NotNull
    private static Map<String, Patient> readCorePatients(@NotNull Map<String, List<SampleData>> sampleDataPerPatient,
            @NotNull PrimaryTumorCurator primaryTumorCurator) {
        Map<String, Patient> patientMap = Maps.newHashMap();
        CorePatientReader corePatientReader = new CorePatientReader(primaryTumorCurator);

        for (Map.Entry<String, List<SampleData>> entry : sampleDataPerPatient.entrySet()) {
            List<SampleData> samples = entry.getValue();

            assert samples != null;
            List<SampleData> tumorSamples = extractTumorSamples(samples);
            if (!tumorSamples.isEmpty()) {
                LimsStudy study = LimsStudy.fromSampleId(tumorSamples.get(0).sampleId());

                if (study == LimsStudy.CORE) {
                    String patientId = entry.getKey();
                    Patient corePatient =
                            corePatientReader.read(patientId, tumorSamples.get(0).limsPrimaryTumor(), sequencedOnly(tumorSamples));
                    patientMap.put(patientId, corePatient);
                }
            }
        }

        return patientMap;
    }

    @NotNull
    private static List<SampleData> extractTumorSamples(@NotNull Iterable<SampleData> samples) {
        List<SampleData> tumorSamples = Lists.newArrayList();

        for (SampleData sample : samples) {
            LimsStudy study = LimsStudy.fromSampleId(sample.sampleId());
            if (study != LimsStudy.NON_CANCER_STUDY) {
                if (sample.sampleId().substring(12).contains("T")) {
                    tumorSamples.add(sample);
                }
            }
        }
        return tumorSamples;
    }

    @NotNull
    private static Map<String, Patient> readColoPatients() {
        Map<String, Patient> patientMap = Maps.newHashMap();
        ColoPatientReader coloPatientReader = new ColoPatientReader();
        LOGGER.info("Creating patient representation for COLO829");
        Patient colo829Patient = coloPatientReader.read("COLO829T");

        patientMap.put(colo829Patient.patientIdentifier(), colo829Patient);
        return patientMap;
    }

    private static <V, K> int countValues(@NotNull Map<V, List<K>> map) {
        int count = 0;
        for (Map.Entry<V, List<K>> entry : map.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    @NotNull
    private static Set<String> toUniqueSampleIds(@NotNull Map<String, List<String>> samplesPerPatient) {
        Set<String> uniqueSampleIds = Sets.newHashSet();
        for (Map.Entry<String, List<String>> entry : samplesPerPatient.entrySet()) {
            uniqueSampleIds.addAll(entry.getValue());
        }
        return uniqueSampleIds;
    }

    @NotNull
    private static List<SampleData> sequencedOnly(@Nullable Iterable<SampleData> samples) {
        if (samples == null) {
            return Lists.newArrayList();
        }

        List<SampleData> sequencedSamples = Lists.newArrayList();
        for (SampleData sample : samples) {
            if (sample.sequenced()) {
                sequencedSamples.add(sample);
            }
        }
        return sequencedSamples;
    }

    private static boolean checkInputs(@NotNull CommandLine cmd) {
        String runsDirectory = cmd.getOptionValue(RUNS_DIRECTORY);

        boolean allParamsPresent = !Utils.anyNull(runsDirectory,
                cmd.getOptionValue(CPCT_ECRF_FILE),
                cmd.getOptionValue(CPCT_FORM_STATUS_CSV),
                cmd.getOptionValue(DRUP_ECRF_FILE),
                cmd.getOptionValue(LIMS_DIRECTORY),
                cmd.getOptionValue(TREATMENT_MAPPING_CSV),
                cmd.getOptionValue(BIOPSY_MAPPING_CSV),
                cmd.getOptionValue(TUMOR_LOCATION_MAPPING_TSV),
                cmd.getOptionValue(CURATED_PRIMARY_TUMOR_TSV),
                cmd.getOptionValue(DOID_JSON));

        if (cmd.hasOption(DO_LOAD_CLINICAL_DATA)) {
            allParamsPresent = allParamsPresent && DatabaseAccess.hasDatabaseConfig(cmd);
        }

        if (cmd.hasOption(DO_PROCESS_WIDE_CLINICAL_DATA)) {
            allParamsPresent = allParamsPresent && !Utils.anyNull(cmd.getOptionValue(WIDE_AVL_TREATMENT_CSV),
                    cmd.getOptionValue(WIDE_PRE_AVL_TREATMENT_CSV),
                    cmd.getOptionValue(WIDE_BIOPSY_CSV),
                    cmd.getOptionValue(WIDE_RESPONSE_CSV),
                    cmd.getOptionValue(WIDE_FIVE_DAYS_CSV));
        }

        boolean validRunDirectories = true;
        if (allParamsPresent) {
            File runDirectoryDb = new File(runsDirectory);

            if (!runDirectoryDb.exists() || !runDirectoryDb.isDirectory()) {
                validRunDirectories = false;
                LOGGER.warn("HMF database run directory '{}' does not exist or is not a directory", runDirectoryDb);
            }
        }

        return validRunDirectories && allParamsPresent;
    }

    @NotNull
    private static Options createOptions() {
        Options options = new Options();
        options.addOption(RUNS_DIRECTORY,
                true,
                "Path towards the folder containing patient runs that are considered part of HMF database.");

        options.addOption(CPCT_ECRF_FILE, true, "Path towards the cpct ecrf file.");
        options.addOption(CPCT_FORM_STATUS_CSV, true, "Path towards the cpct form status csv file.");
        options.addOption(DRUP_ECRF_FILE, true, "Path towards the drup ecrf file.");
        options.addOption(DO_LOAD_RAW_ECRF, false, "If set, writes raw ecrf data to database");

        options.addOption(DO_LOAD_CLINICAL_DATA, false, "If set, clinical data will be loaded into the database");
        options.addOption(CURATED_PRIMARY_TUMOR_TSV, true, "Path towards to the TSV of curated primary tumor.");

        options.addOption(DO_PROCESS_WIDE_CLINICAL_DATA,
                false,
                "if set, creates clinical timeline for wide patients and persists to database");
        options.addOption(WIDE_AVL_TREATMENT_CSV, true, "Path towards the wide avl treatment csv");
        options.addOption(WIDE_PRE_AVL_TREATMENT_CSV, true, "Path towards the wide pre avl treatment csv.");
        options.addOption(WIDE_BIOPSY_CSV, true, "Path towards the wide biopsy csv.");
        options.addOption(WIDE_RESPONSE_CSV, true, "Path towards the wide response csv.");
        options.addOption(WIDE_FIVE_DAYS_CSV, true, "Path towards the wide five days csv.");

        options.addOption(LIMS_DIRECTORY, true, "Path towards the LIMS directory.");

        options.addOption(DOID_JSON, true, "Path towards to the json file of the doid ID of primary tumors.");
        options.addOption(TUMOR_LOCATION_MAPPING_TSV, true, "Path towards to the TSV of mapping the tumor location.");
        options.addOption(TREATMENT_MAPPING_CSV, true, "Path towards to the CSV of mapping the treatments.");
        options.addOption(BIOPSY_MAPPING_CSV, true, "Path towards to the CSV of mapping of biopsies.");

        options.addOption(PIPELINE_VERSION, true, "Path towards the pipeline version");

        addDatabaseCmdLineArgs(options);
        return options;
    }
}
