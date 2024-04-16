package ippp4s4.quicksteel;

import com.google.common.io.Files;
import ippp4s4.quicksteel.model.Legend;
import ippp4s4.quicksteel.model.LegendItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    @FXML
    public Legend legend;

    private List<Color> chartColors = Arrays.asList(Color.PURPLE, Color.GREEN, Color.BLUE, Color.ORANGE, Color.RED, Color.PINK);

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
        var allSeries = new ArrayList<Series<Double, Double>>();
        AtomicReference<BigDecimal> time = new AtomicReference<>(BigDecimal.ZERO);
        for (var vat = 0; vat < vatCount.getValue(); vat++) {
            Series<Double, Double> series = new Series<>();
            series.setName("Wylew " + (vat + 1));
            time.set(BigDecimal.ZERO);
            data.get(vat).forEach(mes -> {
                time.set(time.get().add(new BigDecimal("0.3")));
                var point = new Data<Double, Double>(time.get().doubleValue(), mes);
                point.setNode(createSmallNode(0));
                series.getData().add(point);
            });
            allSeries.add(series);
        }
        conductivityAxis.setLabel("Stężenie bezwymiarowe");

        var processTime = Math.floor(time.get().doubleValue() + timeAxis.getTickUnit());
        if(dTime.isSelected()){
            var dimentionlessTimeStep = 0.3d / Double.parseDouble(avgTime.getText());
            for(var doubleSeries : allSeries){
                var increment = 0d;
                for(var point : doubleSeries.getData()){
                    point.setXValue(increment);
                    increment += dimentionlessTimeStep;
                }
            }
            timeAxis.setUpperBound(dimentionlessTimeStep * allSeries.get(0).getData().size());
            timeAxis.setTickUnit(0.2);
        }
        else {
            timeAxis.setUpperBound(processTime);
            timeAxis.setTickUnit(5d);
        }

        createHorizontalHelpSeries(0d, processTime, 0.8d, "0.8");
        createHorizontalHelpSeries(0d, processTime, 0.2d, "0.2");
        Set<Node> hrNodes = chart.lookupAll(".series0");
        for (Node n : hrNodes) {
            n.setStyle("-fx-stroke: darkred;");
        }
        hrNodes = chart.lookupAll(".series1");
        for (Node n : hrNodes) {
            n.setStyle("-fx-stroke: darkred;");
        }

        legend.clear();
        for(var i = 0; i < allSeries.size(); i++) {
            chart.getData().add(allSeries.get(i));
            legend.addLegendItem(new LegendItem(chart.getData().get(chart.getData().size() - 1), chartColors.get(i), true));
            chart.applyCss();
            Set<Node> nodes = chart.lookupAll(".series" + (i + 2));
            String rgb = String.format("%d, %d, %d",
                    (int) (chartColors.get(i).getRed() * 255),
                    (int) (chartColors.get(i).getGreen() * 255),
                    (int) (chartColors.get(i).getBlue() * 255));
            for (Node n : nodes) {
                n.setStyle("-fx-stroke: rgba(" + rgb + ", 1.0);");
            }
        }

        if(dTime.isSelected()){
            timeAxis.setLabel("Czas bezwymiarowy");
        }
        else{
            timeAxis.setLabel("Czas [s]");
        }
    }
    public void saveAsExcel(){
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dane wykresu");

        ObservableList<Series<Double, Double>> chartData = chart.getData();

        //cleanup
        List<String> seriesToRemove = Arrays.asList("0.2", "0.8");
        chartData.removeIf(series -> seriesToRemove.contains(series.getName()));

        // Make row with x-axis values
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(chart.getXAxis().getLabel());
        int rowNum = 1;
        for (Data<Double, Double> data : chartData.get(0).getData()) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }
            row.createCell(0).setCellValue(data.getXValue());
            rowNum++;
        }

        // Make rows with y-axis values
        int colNum = 1;
        for (Series<Double, Double> series : chartData) {
            headerRow.createCell(colNum).setCellValue(series.getName());
            rowNum = 1;
            for (Data<Double, Double> data : series.getData()) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    row = sheet.createRow(rowNum);
                }
                row.createCell(colNum).setCellValue(data.getYValue());
                rowNum++;
            }
            colNum++;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz jako arkusz kalkulacyjny");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arkusz kalkulacyjny", "*.xlsx"));
        fileChooser.setInitialFileName("Wylew.xlsx");

        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                System.out.println("Pilk Excela utworzony!");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error :)");
            } finally {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void saveAsCSV() {
        char rowDelimiter = ';';
        ObservableList<Series<Double, Double>> chartData = chart.getData();

        //cleanup
        List<String> seriesToRemove = Arrays.asList("0.2", "0.8");
        chartData.removeIf(series -> seriesToRemove.contains(series.getName()));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz jako CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("Wylew.csv");

        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Make header row with series names
                writer.write(chartData.get(0).getName() + rowDelimiter); // x-axis name
                for (Series<Double, Double> series : chartData) {
                    writer.write(series.getName() + rowDelimiter); // y-axis names
                }
                writer.newLine();

                // Make rows with x-axis and y-axis values
                int maxDataPoints = chartData.stream().mapToInt(series -> series.getData().size()).max().orElse(0);
                for (int i = 0; i < maxDataPoints; i++) {
                    // x-axis values
                    Data<Double, Double> xData = chartData.get(0).getData().get(i);
                    writer.write(xData.getXValue() + Character.toString(rowDelimiter));

                    // y-axis values
                    for (Series<Double, Double> series : chartData) {
                        Data<Double, Double> yData = series.getData().get(i);
                        writer.write(yData.getYValue() + Character.toString(rowDelimiter));
                    }
                    writer.newLine();
                }

                System.out.println("Pilk CSV utworzony!");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error :)");
            }
        }
    }

    public void saveAsPng() {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter for text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Pliki graficzne (*.png, *.jpg)", "*.png", "*.jpg");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName("wykres.png");

        //Show save file dialog
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            WritableImage image = chart.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), Files.getFileExtension(file.getName()), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void createHorizontalHelpSeries(double start, double end, double y, String name){
        Series<Double, Double> series = new Series<>();
        var point = new Data<Double, Double>(start, y);
        var point2 = new Data<Double, Double>(end, y);
        point.setNode(createSmallNode(1));
        point2.setNode(createSmallNode(1));
        series.getData().addAll(point, point2);
        series.setName(name);
        chart.getData().add(series);
    }

    private Node createSmallNode(double size){
        var pane = new Pane();
        pane.setShape(new Circle(size));
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
