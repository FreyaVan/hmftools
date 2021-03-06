package com.hartwig.hmftools.patientreporter.cfreport.chapters;

import com.hartwig.hmftools.patientreporter.cfreport.ReportResources;
import com.itextpdf.layout.Document;

import org.jetbrains.annotations.NotNull;

public interface ReportChapter {

    @NotNull
    String name();

    void render(@NotNull Document reportDocument);

    default boolean isFullWidth() {
        return true;
    }

    default boolean hasCompleteSidebar() {
        return false;
    }

    default float contentWidth() {
        return isFullWidth()
                ? ReportResources.CONTENT_WIDTH_WIDE
                : ReportResources.CONTENT_WIDTH_NARROW;
    }
}
