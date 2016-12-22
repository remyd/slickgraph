package fr.caladan.slickgraph;

import javafx.scene.paint.Color;

/**
 * 2D vertex for rendering in JavaFX canvas
 *
 * @author "Rémy Dautriche <remy.dautriche@caladan.fr>"
 */
public class Vertex {

	public double x;
	public double y;
	public Color color;

	public Vertex(double x, double y) {
		this.x = x;
		this.y = y;
		color = Color.BLACK;
	}

	public Vertex(double x, double y, Color c) {
		this(x, y);
		this.color = c;
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + "]";
	}
}