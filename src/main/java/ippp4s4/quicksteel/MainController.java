package ippp4s4.quicksteel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    public CheckBox dTime;
    @FXML
    public VBox dTimeMenu;
    @FXML
    public ComboBox<Integer> vatCount;
    @FXML
    public Button generateBtn;
    List<List<Double>> data = new ArrayList<>();
    File uploadedFile;

    public void openFile(){
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Plik z pomiarami (*.csv)", "*.csv");
        chooser.getExtensionFilters().add(extFilter);
        uploadedFile = chooser.showOpenDialog(new Stage());
    }

    public void hideDTimeMenu(){
        var isOpen = dTime.isSelected();
        dTimeMenu.setDisable(!isOpen);
    }

    public void filterData() throws IOException {
        if(uploadedFile == null || !uploadedFile.exists())
            return;
        try {
            Scanner myReader = new Scanner(new BufferedReader(new FileReader(uploadedFile)));
            var i = 0;
            while (myReader.hasNextLine()) {
                List<String> line = Arrays.stream(myReader.nextLine().split(";")).toList();
                i++;
                if(i==1 || i ==2)
                    continue;
                line = line.subList(2, 8);
                List<Double> numericDataLine = new ArrayList<>();
                line.forEach(mes -> numericDataLine.add(Double.valueOf(mes.replace(',','.'))));
                data.add(numericDataLine);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void plotData(){
        
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        vatCount.getItems().setAll(1,2,3,4,5,6);
        vatCount.setValue(4);
    }
}
