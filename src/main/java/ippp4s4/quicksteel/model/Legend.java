package ippp4s4.quicksteel.model;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;

public class Legend extends HBox {

    public Legend() {
        // Initialize the custom legend component
        setSpacing(5); // Adjust spacing between legend items
    }

    public void addLegendItem(LegendItem item) {
        CheckBox checkBox = new CheckBox(item.getSeriesName());
        checkBox.setSelected(item.isVisible());
        checkBox.setTextFill((Paint) item.getSeriesColor());

        checkBox.setOnAction(event -> {
            item.setVisible(checkBox.isSelected());
            // Update visibility of corresponding series in the chart
            // (Implementation required based on your chart setup)

        });

        getChildren().add(checkBox);
    }

    public void clear() {
        getChildren().clear();
    }
}
