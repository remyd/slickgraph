package fr.caladan.slickgraph;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * Time cursor on Slick Graph
 */
public class TimeCursor extends Group {

	/** Vertical line for the cursor */
	protected Line cursorLine;
	public Line getCursorLine() {
		return cursorLine;
	}

	/** Label for the tooltip */
	protected Label tooltip;
	public void setTooltipText(String text) {
		tooltip.setText(text);
	}

	/** Circle on the curve of the graph */
	protected Circle pointer;

	/** Public default constructor - initialize the JavaFX components of the node */
	public TimeCursor() {
		super();

		// cursor line
		cursorLine = new Line();
		cursorLine.setStrokeWidth(1.);
		getChildren().add(cursorLine);

		// tooltip
		tooltip = new Label();
		tooltip.setLayoutY(0.);
		tooltip.setMinWidth(100.);
		tooltip.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
		tooltip.setTextFill(Color.WHITE);
		getChildren().add(tooltip);

		// pointer
		pointer = new Circle(3., Color.BLACK);
		pointer.layoutXProperty().bind(cursorLine.startXProperty());
		getChildren().add(pointer);
	}

	/**
	 * Set the position of the cursor
	 *
	 * @param x Horizontal position in pixels
	 * @param y Vertical position in pixels
	 */
	public void setPosition(double x, double y) {
		cursorLine.setStartX(x);
		cursorLine.setEndX(x);
		tooltip.setLayoutX(x + (x < getParent().getBoundsInLocal().getWidth() - tooltip.getBoundsInLocal().getWidth() - 50 ? 0 : -tooltip.getBoundsInLocal().getWidth()));
		pointer.setLayoutY(y);
	}

}
