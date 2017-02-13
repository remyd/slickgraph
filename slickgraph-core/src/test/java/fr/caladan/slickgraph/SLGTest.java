package fr.caladan.slickgraph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Test;

public class SLGTest extends Application {

	private List<Timeseries> ts;

	public SLGTest() {
		List<Double> data = new ArrayList<Double>();
		for (int i = 0; i < 100; i++) {
			data.add((double) i);
		}

		ts = new ArrayList<Timeseries>();
		ts.add(new Timeseries(data));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// nothing to do, we only need the jfx thread
	}

	@BeforeClass
	public static void initJavaFX() {
		Thread t = new Thread(() -> Application.launch(SLGTest.class, new String[0]));
		t.setDaemon(true);
		t.start();
	}

	@Test
	public void testInitialization() {
		SlickGraph slg = new SlickGraph();

		assertNotNull(slg.canvas);
		assertNotNull(slg.timeseries);
		assertTrue(slg.timeseries.isEmpty());
		assertNotNull(slg.kernelBandWidthProperty);
		assertTrue(slg.kernelBandWidthProperty.get() == 5.);
		assertNotNull(slg.mapHistograms);
		assertTrue(slg.mapHistograms.isEmpty());
		assertNotNull(slg.mapSmoothedHistogram);
		assertTrue(slg.mapSmoothedHistogram.isEmpty());
		assertNotNull(slg.mapVertices);
		assertTrue(slg.mapVertices.isEmpty());
		assertTrue(slg.start == -1);
		assertTrue(slg.end == -1);

		slg.setTimeseries(new ArrayList<Timeseries>());
		assertNotNull(slg.getTimeseries());
		assertTrue(slg.getTimeseries().isEmpty());
	}

	@Test
	public void testHistogram() {
		SlickGraph slg = new SlickGraph();
		slg.setTimeseries(ts);

		// slg.scaledWidth = 100;
		// slg.scaledHeight = 10;
		// slg.buildHistogram(ts.get(0));
		// assertTrue(slg.mapHistograms.get(ts.get(0)).size() == Math.floor(6. * slg.kernelBandWidthProperty.get()) + 100);

		// slg.computeConvolution(ts.get(0));
		// assertTrue(slg.mapSmoothedHistogram.get(ts.get(0)).size() == Math.floor(6. * slg.kernelBandWidthProperty.get()) + 100);
	}

	@Test
	public void testKernel() {
		SlickGraph slg = new SlickGraph();
		List<Double> kernel = StatisticKernel.gaussian(slg.getKernelBandWidth());
		slg.setTimeseries(ts);

		assertTrue(kernel.size() == slg.kernelBandWidthProperty.get() * 6 + 1);
		for (int i = 0; i < slg.kernelBandWidthProperty.get() * 3; i++) {
			assertTrue(kernel.get(i).doubleValue() == kernel.get(kernel.size() - i - 1).doubleValue());
		}
	}

}
