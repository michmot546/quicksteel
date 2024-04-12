package ippp4s4.quicksteel;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MainController implements Initializable {

    @FXML
    public CheckBox dTime;
    @FXML
    public VBox dTimeMenu;
    @FXML
    public ComboBox<Integer> vatCount;
    @FXML
    public Button generateBtn;
    @FXML
    public LineChart chart;
    @FXML
    public NumberAxis timeAxis;

    ArrayList<ArrayList<Double>> data = new ArrayList<>();
    File uploadedFile;

    public void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Plik z pomiarami (*.csv)", "*.csv");
        chooser.getExtensionFilters().add(extFilter);
        uploadedFile = chooser.showOpenDialog(new Stage());
    }

    public void hideDTimeMenu() {
        var isOpen = dTime.isSelected();
        dTimeMenu.setDisable(!isOpen);
    }

    public void filterData() throws IOException {
        if (uploadedFile == null || !uploadedFile.exists())
            return;
        data.clear();
        for (var vat = 0; vat < vatCount.getValue(); vat++) {
            data.add(new ArrayList<>());
        }
        try {
            Scanner myReader = new Scanner(new BufferedReader(new FileReader(uploadedFile)));
            var i = 0;
            while (myReader.hasNextLine()) {
                List<String> line = Arrays.stream(myReader.nextLine().split(";")).toList();
                i++;
                if (i == 1 || i == 2)
                    continue;
                line = line.subList(2, vatCount.getValue() + 2);
                ArrayList<Double> numericDataLine = new ArrayList<>();
                line.forEach(mes -> numericDataLine.add(Double.valueOf(mes.replace(',', '.'))));
                for (var vat = 0; vat < vatCount.getValue(); vat++) {
                    data.get(vat).add(numericDataLine.get(vat));
                }
            }
            myReader.close();
        } catch (NumberFormatException e) {
            System.out.println("Data format error");
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        CalculateDimensionlessConcetration();
        plotData();
    }

    private void CalculateDimensionlessConcetration() {
        int numThreads = data.size();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        // Process each inner array asynchronously
        for (ArrayList<Double> innerArray : data) {
            executorService.submit(() -> processInnerArray(innerArray));
        }

        // Shutdown the executor service after all tasks are submitted
        executorService.shutdown();
    }

    private void processInnerArray(ArrayList<Double> dataArray) {
        var c0 = dataArray.get(0);
        var cinf = dataArray.get(dataArray.size() - 1);
        for (var i = 0; i < dataArray.size(); i++) {
            var ct = dataArray.get(i);
            dataArray.set(i, (ct - c0) / (cinf - c0));
        }
    }

    private void plotData() {
        chart.getData().clear();
        createHelpSeries();

        AtomicReference<BigDecimal> time = new AtomicReference<>(BigDecimal.ZERO);
        for (var vat = 0; vat < vatCount.getValue(); vat++) {
            Series series = new Series();
            series.setName("Kadź " + (vat + 1));
            time.set(BigDecimal.ZERO);
            data.get(vat).forEach(mes -> {
                BigDecimal increment = new BigDecimal("0.3");
                time.set(time.get().add(increment));
                var point = new Data<Number, Number>(time.get(), mes);
                point.setNode(createSmallNode());
                series.getData().add(point);
            });
            chart.getData().add(series);
        }

        timeAxis.setUpperBound(Math.floor(time.get().doubleValue() + timeAxis.getTickUnit()));
    }

    private void createHelpSeries(){
        Series series = new Series();
        var point = new Data<>(-1, 0.8);
        series.getData().addAll(point, new Data<>(200f, 0.8f));
        series.setName("0.8");
        chart.getData().add(series);

        Series series2 = new Series();
        var point2 = new Data<>(-1, 0.2);
        series2.getData().addAll(point2, new Data<>(200f, 0.2f));
        series2.setName("0.2");
        chart.getData().add(series2);
    }

    private Node createSmallNode(){
        var pane = new Pane();
        pane.setShape(new Circle(2.0));
        pane.setScaleShape(false);
        return pane;
    }

    private Node createDataNode(ObjectProperty<Double> value) {
        var label = new Label();
        label.textProperty().bind(value.asString("%,.1f"));

        var pane = new Pane(label);
        pane.setShape(new Circle(6.0));
        pane.setScaleShape(false);

        label.translateXProperty().bind(label.widthProperty().divide(0.4));
        label.translateYProperty().bind(label.heightProperty().divide(-1.3));

        return pane;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        vatCount.getItems().setAll(1, 2, 3, 4, 5, 6);
        vatCount.setValue(4);
    }
}
