package fr.caladan.slickgraph;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/**
 * Timeseries model to visualize
 */
public class Timeseries {

	/** Name of the timeseries */
	protected SimpleStringProperty nameProperty;
	public SimpleStringProperty nameProperty() {
		return nameProperty;
	}
	public String getName() {
		return nameProperty.get();
	}
	public void setName(String name) {
		nameProperty.set(name);
	}

	/** Color to use for rendering */
	protected SimpleObjectProperty<Color> colorProperty;
	public SimpleObjectProperty<Color> colorProperty() {
		return colorProperty;
	}
	public Color getColor() {
		return colorProperty.get();
	}
	public void setColor(Color color) {
		colorProperty.set(color);
	}

	/** Indicates whether it is selected or not */
	protected SimpleBooleanProperty selectedProperty;
	public SimpleBooleanProperty selectedProperty() {
		return selectedProperty;
	}
	public boolean isSelected() {
		return selectedProperty.get();
	}
	public void setSelected(boolean selected) {
		selectedProperty.set(selected);
	}

	/** List of timestamps that constitutes the timeseries */
	protected ObservableList<Double> data;
	public ObservableList<Double> getData() {
		return data;
	}
	public void setData(List<Double> data) throws Exception {
		if (data == null) {
			this.data.clear();
			return;
		}

		// check if the timeseries is valid: timestamps are strictly growing
		int i = 0;
		boolean isValid = true;
		while (i < data.size() - 1 && isValid) {
			isValid = data.get(i) < data.get(i + 1);
			i++;
		}
		if (!isValid) {
			throw new Exception("Timeseries is not valid: timestamps should be strictly increasing");
		}

		this.data.clear();
		this.data.addAll(data);
	}

	/**
	 * Initializes all the attributes
	 *
	 * @param name Name of the timeseries
	 * @param color Color used for rendering
	 * @param data List of timestamps contained in the timeseries. Must be sorted
	 */
	public Timeseries(String name, Color color, List<Double> data) {
		nameProperty = new SimpleStringProperty(name);
		colorProperty = new SimpleObjectProperty<Color>(color);
		selectedProperty = new SimpleBooleanProperty(false);
		this.data = FXCollections.observableArrayList();
		try {
			setData(data);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Constructor that initializes the color to black by default
	 *
	 * @param name Name of the timeseries
	 * @param data List of timestamps contained in the timeseries. Must be sorted
	 * @throws Exception If the data is not valid (timestamps should be strictly increasing)
	 */
	public Timeseries(String name, List<Double> data) {
		this(name, Color.BLACK, data);
	}

	/**
	 * Constructor that initializes the timeseries with no data and black color by default
	 *
	 * @param name Name of the timeseries
	 */
	public Timeseries(String name) {
		this(name, Color.BLACK, null);
	}

	/**
	 * Constructor that initializes the timeseries with data only, put an empty name and black color by default
	 *
	 * @param data List of timestamps contained in the timeseries. Must be sorted
	 */
	public Timeseries(List<Double> data) {
		this("", Color.BLACK, data);
	}

	/**
	 * Public default constructor - initializes a timeseries with an empty name, black color and no data
	 */
	public Timeseries() {
		this("", Color.BLACK, null);
	}

}
