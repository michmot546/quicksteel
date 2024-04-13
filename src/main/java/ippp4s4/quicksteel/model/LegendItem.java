package ippp4s4.quicksteel.model;

import javafx.scene.Node;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class LegendItem {
    private final Series<Double, Double> series;
    private final Paint seriesColor;
    private boolean visible;

    public LegendItem(Series<Double,Double> series, Paint seriesColor, boolean visible) {
        this.series = series;
        this.seriesColor = seriesColor;
        this.visible = visible;
    }

    // Getters and setters
    public String getSeriesName() {
        return series.getName();
    }

    public Paint getSeriesColor() {
        return seriesColor;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.series.getNode().setVisible(visible);
    }
}
