package com.hartwig.hmftools.patientdb;

import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.addDatabaseCmdLineArgs;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.databaseAccess;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.hartwig.hmftools.common.chord.ChordAnalysis;
import com.hartwig.hmftools.common.chord.ChordFileReader;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class LoadChordData {

    private static final Logger LOGGER = LogManager.getLogger(LoadChordData.class);

    private static final String SAMPLE = "sample";

    private static final String PREDICTION_FILE = "prediction_file";

    public static void main(@NotNull String[] args) throws ParseException, SQLException {
        Options options = createOptions();
        CommandLine cmd = createCommandLine(args, options);

        String predictionFile = cmd.getOptionValue(PREDICTION_FILE);
        String sample = cmd.getOptionValue(SAMPLE);

        if (Utils.anyNull(predictionFile, sample)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("patient-db - load chord data", options);
        } else {
            File fileObject = new File(predictionFile);
            if (fileObject.isFile()) {
                DatabaseAccess dbWriter = databaseAccess(cmd);

                LOGGER.info("Extracting and writing chord for {}", predictionFile);
                try {
                    ChordAnalysis chordAnalysis = ChordFileReader.read(predictionFile);
                    dbWriter.writeChord(sample, chordAnalysis);
                } catch (IOException e) {
                    LOGGER.warn("Cannot extract chord for {}", predictionFile);
                }
            } else {
                if (!fileObject.exists()) {
                    LOGGER.warn("file '{}' does not exist.", predictionFile);
                }
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("patient-db - load chord data", options);
            }
        }
    }

    @NotNull
    private static Options createOptions() {
        Options options = new Options();
        options.addOption(SAMPLE, true, "The tumor sample.");
        options.addOption(PREDICTION_FILE, true, "Path towards the chord prediction file.");
        addDatabaseCmdLineArgs(options);
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull String[] args, @NotNull Options options) throws ParseException {
        return new DefaultParser().parse(options, args);
    }
}