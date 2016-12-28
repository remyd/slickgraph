package fr.caladan.slickgraph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class SLGTest {

	@Test
	public void testData() {
		SlickGraph slg = new SlickGraph();

		assertNotNull(slg.dataProperty);
		assertNull(slg.dataProperty.get());
		assertNotNull(slg.kernelBandWidthProperty);
		assertTrue(slg.kernelBandWidthProperty.get() == 5.);
		assertNotNull(slg.histogram);
		assertTrue(slg.histogram.isEmpty());
		assertTrue(slg.start == -1);
		assertTrue(slg.end == -1);
		assertTrue(slg.histogramMax == -1);
		assertNotNull(slg.vertices);
		assertTrue(slg.vertices.isEmpty());
		assertTrue(slg.origMouseX == 0);

		slg.setData(new ArrayList<Double>());
		assertNotNull(slg.dataProperty.get());
		assertTrue(slg.dataProperty.get().isEmpty());
	}
}
