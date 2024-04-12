package ippp4s4.quicksteel;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
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
    public LineChart<Double, Double> chart;
    @FXML
    public NumberAxis timeAxis;
    @FXML
    public NumberAxis conductivityAxis;
    @FXML
    public TextField vFluid;
    @FXML
    public TextField qFluid;
    @FXML
    public TextField avgTime;


    ArrayList<ArrayList<Double>> data = new ArrayList<>();
    File uploadedFile;

    public void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Plik z pomiarami (*.csv)", "*.csv");
        chooser.getExtensionFilters().add(extFilter);
        uploadedFile = chooser.showOpenDialog(new Stage());

        ButtonBlockSet();
    }

    public void ButtonBlockSet(){
        this.generateBtn.setDisable(!conditionsForBtnUnlockMet());
    }

    private double calculateTheoreticalAvgTime(double v, double q){
        return v/q;
    }

    private boolean conditionsForBtnUnlockMet(){
        boolean conditionWithDTime = false;
        if(dTime.isSelected()){
            if(vFluid.getText().isEmpty() || qFluid.getText().isEmpty()){
                conditionWithDTime = false;
            }
            else {
                conditionWithDTime = true;
                var v = Double.valueOf(vFluid.getText());
                var q = Double.valueOf(qFluid.getText());
                if(v == 0 || q == 0)
                    avgTime.setText("");
                else
                    avgTime.setText(calculateTheoreticalAvgTime(v, q) + "");
            }
        }
        else{
            conditionWithDTime = true;
        }

        return this.uploadedFile != null && conditionWithDTime;
    }

    public void hideDTimeMenu() {
        var isOpen = dTime.isSelected();
        dTimeMenu.setDisable(!isOpen);
        generateBtn.setDisable(!conditionsForBtnUnlockMet());
    }

    public void filterData() throws IOException {
        if(!conditionsForBtnUnlockMet() || !uploadedFile.exists()){
            generateBtn.setDisable(true);
            return;
        }
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
            Series<Double, Double> series = new Series<>();
            series.setName("Wylew " + (vat + 1));
            time.set(BigDecimal.ZERO);
            data.get(vat).forEach(mes -> {
                BigDecimal increment = new BigDecimal("0.3");
                time.set(time.get().add(increment));
                var point = new Data<Double, Double>(time.get().doubleValue(), mes);
                point.setNode(createSmallNode());
                series.getData().add(point);
            });
            chart.getData().add(series);
        }
        conductivityAxis.setLabel("Stężenie bezwymiarowe");
        timeAxis.setUpperBound(Math.floor(time.get().doubleValue() + timeAxis.getTickUnit()));
        if(dTime.isSelected()){
            timeAxis.setLabel("Czas bezwymiarowy");
        }
        else{
            timeAxis.setLabel("Czas [s]");
        }
    }

    private void createHelpSeries(){
        Series<Double, Double> series = new Series<>();
        var point = new Data<Double, Double>(-1.0, 0.8);
        series.getData().addAll(point, new Data<Double, Double>(200.0, 0.8));
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

        vFluid.textProperty().addListener(new CharacterValidator(vFluid));
        vFluid.focusedProperty().addListener(new NumberValidator(vFluid));

        qFluid.textProperty().addListener(new CharacterValidator(qFluid));
        qFluid.focusedProperty().addListener(new NumberValidator(qFluid));
    }

    private class CharacterValidator implements ChangeListener<String>{
        private TextField field;
        public CharacterValidator(TextField textField){
            this.field = textField;
        }
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
            if (!newValue.matches("^[0-9.]+$") && !newValue.isEmpty()) {
                field.setText(oldValue);
            }
        }
    }
    private class NumberValidator implements ChangeListener<Boolean>{
        private TextField field;
        public NumberValidator(TextField textField){
            this.field = textField;
        }
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldPropertyValue, Boolean newPropertyValue) {
            //TextField is out of focus
            if (!newPropertyValue)
            {
                var textToValidate = field.getText().trim();

                if(textToValidate.equals("0") || textToValidate.isEmpty()){
                    field.clear();
                    return;
                }

                if(textToValidate.length() > 10)
                    textToValidate = textToValidate.substring(0, 10);

                textToValidate = removeMoreThanOneOccurence(textToValidate, '.');

                if(textToValidate.endsWith(".")){
                    textToValidate = textToValidate.replace(".", "");
                }

                if(textToValidate.startsWith(".")){
                    textToValidate = "0" + textToValidate;
                }

                textToValidate = new BigDecimal(textToValidate).stripTrailingZeros().toString();

                field.setText(textToValidate);
                ButtonBlockSet();
            }
        }
    }
    // Method to count occurrences of a character in a string
    private String removeMoreThanOneOccurence(String str, char targetChar) {
        // Split the input string into two parts at the first dot
        String[] parts = str.split("\\.", 2); // Limit the split to 2 parts
        // If there was a dot in the input
        if (parts.length > 1) {
            // Concatenate the first part (before the dot) with the second part (after the dot)
            String result = parts[0] + "." + parts[1].replaceAll("\\.", "");
            return result;
        } else {
            // No dot found, use the original input
            return str;
        }
    }
}
