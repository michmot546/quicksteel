package ippp4s4.quicksteel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.xml.transform.Source;
import java.io.*;
import java.net.URL;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;

public class MainController implements Initializable {

    @FXML
    public CheckBox dTime;
    @FXML
    public VBox dTimeMenu;
    @FXML
    public ComboBox<Integer> vatCount;
    @FXML
    public Button generateBtn;
    List<Double[]> data = new ArrayList<>();
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
            Scanner myReader = new Scanner(uploadedFile);
            System.out.println(uploadedFile.length());
            System.out.println(myReader.hasNext());
            while (myReader.hasNext()) {
                String data = myReader.next();
                System.out.println(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        vatCount.getItems().setAll(1,2,3,4,5,6);
        vatCount.setValue(4);
    }
}
