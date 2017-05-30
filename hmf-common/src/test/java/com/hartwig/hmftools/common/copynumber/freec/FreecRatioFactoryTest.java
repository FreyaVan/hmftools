package com.hartwig.hmftools.common.copynumber.freec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.io.Resources;
import com.hartwig.hmftools.common.exception.HartwigException;

import org.junit.Test;

public class FreecRatioFactoryTest {

    private static final double EPSILON = 1e-10;
    private static final String BASE_PATH = Resources.getResource("copynumber").getPath() + File.separator + "ratio";
    private static final String SAMPLE = "sample";

    @Test
    public void canConvertLine() throws HartwigException {
        final String line = "1\t1000\t0.3\t0.28\t2\t-1\t2\t-\t-1";

        final FreecRatio ratio = FreecRatioFactory.fromRatioLine(line);
        assertEquals("1", ratio.chromosome());
        assertEquals(1000, ratio.position());
        assertEquals(0.3, ratio.ratio(), EPSILON);
        assertEquals(0.28, ratio.medianRatio(), EPSILON);
        assertEquals(2, ratio.estimatedBAF(), EPSILON);
    }

    @Test
    public void canLoadNormalFile() throws IOException, HartwigException {
        final List<FreecRatio> ratios = FreecRatioFactory.loadNormalRatios(BASE_PATH, SAMPLE);
        assertEquals(2, ratios.size());
        assertEquals(-1, ratios.get(0).ratio(), EPSILON);
        assertEquals(0.3, ratios.get(1).ratio(), EPSILON);
    }

    @Test
    public void canLoadTumorFile() throws IOException, HartwigException {
        final List<FreecRatio> ratios = FreecRatioFactory.loadTumorRatios(BASE_PATH, SAMPLE);
        assertEquals(1, ratios.size());
        assertEquals(1, ratios.get(0).ratio(), EPSILON);
    }
}
