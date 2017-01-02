package fr.caladan.slickgraph;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Test;

public class SLGTest extends Application {

	private List<Double> data;

	public SLGTest() {
		data = new ArrayList<Double>();
		for (int i = 0; i < 100; i++) {
			data.add((double) i);
		}
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

		/* assertNotNull(slg.dataProperty);
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

		try {
			slg.setData(new ArrayList<Double>());
		} catch (Exception e) {}
		assertNotNull(slg.dataProperty.get());
		assertTrue(slg.dataProperty.get().isEmpty()); */
	}

	@Test
	public void testHistogram() {
		SlickGraph slg = new SlickGraph();

		/* try {
			slg.setData(data);
		} catch (Exception e) {}

		slg.scaledWidth = 100;
		slg.scaledHeight = 10;
		slg.buildHistogram();
		assertTrue(slg.histogram.size() == Math.floor(6. * slg.kernelBandWidthProperty.get()) + 100);

		slg.computeConvolution();
		assertTrue(slg.smoothedHistogram.size() == Math.floor(6. * slg.kernelBandWidthProperty.get()) + 100); */
	}

	@Test
	public void testKernel() {
		/* SlickGraph slg = new SlickGraph();
		List<Double> kernel = slg.gaussianKernel();
		try {
			slg.setData(data);
		} catch (Exception e) {}

		assertTrue(kernel.size() == slg.kernelBandWidthProperty.get() * 6 + 1);
		for (int i = 0; i < slg.kernelBandWidthProperty.get() * 3; i++) {
			assertTrue(kernel.get(i).doubleValue() == kernel.get(kernel.size() - i - 1).doubleValue());
		} */
	}

}
