package com.hartwig.hmftools.patientreporter.cfreport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.patientreporter.AnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.PatientReport;
import com.hartwig.hmftools.patientreporter.ReportWriter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.CircosChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.DetailsAndDisclaimerChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.ExplanationChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.GenomicAlterationsChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.QCFailChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.ReportChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.SummaryChapter;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.TherapyDetailsChapterOffLabel;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.TherapyDetailsChapterOnLabel;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.TumorCharacteristicsChapter;
import com.hartwig.hmftools.patientreporter.qcfail.QCFailReport;
import com.hartwig.hmftools.protect.GenomicAnalysis;
import com.hartwig.hmftools.protect.variants.germline.GermlineReportingModel;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.property.AreaBreakType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CFReportWriter implements ReportWriter {

    private static final Logger LOGGER = LogManager.getLogger(CFReportWriter.class);

    private final boolean writeToFile;
    @NotNull
    private final GermlineReportingModel germlineReportingModel;

    @NotNull
    public static CFReportWriter createProductionReportWriterNoGermline() {
        return new CFReportWriter(true, new GermlineReportingModel(Lists.newArrayList()));
    }

    @NotNull
    public static CFReportWriter createProductionReportWriter(@NotNull GermlineReportingModel germlineReportingModel) {
        return new CFReportWriter(true, germlineReportingModel);
    }

    @VisibleForTesting
    CFReportWriter(final boolean writeToFile, @NotNull final GermlineReportingModel germlineReportingModel) {
        this.writeToFile = writeToFile;
        this.germlineReportingModel = germlineReportingModel;
    }

    @Override
    public void writeAnalysedPatientReport(@NotNull AnalysedPatientReport report, @NotNull String outputFilePath) throws IOException {
        GenomicAnalysis analysis = report.genomicAnalysis();
        ReportChapter[] chapters = new ReportChapter[] { new SummaryChapter(report), new TherapyDetailsChapterOnLabel(analysis),
                new TherapyDetailsChapterOffLabel(analysis),
                new GenomicAlterationsChapter(analysis, report.sampleReport(), germlineReportingModel),
                new TumorCharacteristicsChapter(analysis), new CircosChapter(report), new ExplanationChapter(),
                new DetailsAndDisclaimerChapter(report) };

        writeReport(report, chapters, outputFilePath);
    }

    @Override
    public void writeQCFailReport(@NotNull QCFailReport report, @NotNull String outputFilePath) throws IOException {
        writeReport(report, new ReportChapter[] { new QCFailChapter(report) }, outputFilePath);
    }

    private void writeReport(@NotNull PatientReport patientReport, @NotNull ReportChapter[] chapters, @NotNull String outputFilePath)
            throws IOException {
        Document doc = initializeReport(outputFilePath, writeToFile);
        PdfDocument pdfDocument = doc.getPdfDocument();

        PageEventHandler pageEventHandler = new PageEventHandler(patientReport);
        pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, pageEventHandler);

        for (int i = 0; i < chapters.length; i++) {
            ReportChapter chapter = chapters[i];

            pageEventHandler.chapterTitle(chapter.name());
            pageEventHandler.resetChapterPageCounter();
            pageEventHandler.sidebarType(!chapter.isFullWidth(), chapter.hasCompleteSidebar());

            if (i > 0) {
                doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
            chapter.render(doc);
        }

        pageEventHandler.writeDynamicTextParts(doc.getPdfDocument());

        doc.close();
        pdfDocument.close();

        if (writeToFile) {
            LOGGER.info("Created patient report at {}", outputFilePath);
        } else {
            LOGGER.info("Successfully generated in-memory patient report");
        }
    }

    @NotNull
    private static Document initializeReport(@NotNull String outputFilePath, boolean writeToFile) throws IOException {
        PdfWriter writer;
        if (writeToFile) {
            if (Files.exists(new File(outputFilePath).toPath())) {
                throw new IOException("Could not write " + outputFilePath + " as it already exists.");
            }

            writer = new PdfWriter(outputFilePath);
        } else {
            // Write output to output stream where it is effectively ignored.
            writer = new PdfWriter(new ByteArrayOutputStream());
        }

        PdfDocument pdf = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4);
        pdf.getDocumentInfo().setTitle(ReportResources.METADATA_TITLE);
        pdf.getDocumentInfo().setAuthor(ReportResources.METADATA_AUTHOR);

        Document document = new Document(pdf);
        document.setMargins(ReportResources.PAGE_MARGIN_TOP,
                ReportResources.PAGE_MARGIN_RIGHT,
                ReportResources.PAGE_MARGIN_BOTTOM,
                ReportResources.PAGE_MARGIN_LEFT);

        return document;
    }
}
