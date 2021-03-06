package com.hartwig.hmftools.serve.actionability.hotspot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.hartwig.hmftools.serve.actionability.ActionabilityTestUtil;

import org.junit.Test;

public class ActionableHotspotFileTest {

    @Test
    public void canReadFromFileAndConvert() throws IOException {
        String actionableHotspotTsv = ActionableHotspotFile.actionableHotspotTsvPath(ActionabilityTestUtil.SERVE_ACTIONABILITY_DIR);
        List<ActionableHotspot> actionableHotspots = ActionableHotspotFile.read(actionableHotspotTsv);

        assertEquals(1, actionableHotspots.size());

        List<String> lines = ActionableHotspotFile.toLines(actionableHotspots);
        List<ActionableHotspot> regeneratedHotspots = ActionableHotspotFile.fromLines(lines);
        List<String> regeneratedLines = ActionableHotspotFile.toLines(regeneratedHotspots);
        assertEquals(lines.size(), regeneratedLines.size());

        for (int i = 0; i < lines.size(); i++) {
            assertEquals(lines.get(i), regeneratedLines.get(i));
        }
    }
}