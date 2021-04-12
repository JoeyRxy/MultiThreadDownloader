package mine.learn.mtdl.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ProxyController {

    @FXML
    private TextField ipfield;

    @FXML
    private TextField portfield;

    @FXML
    private ComboBox<String> schemefield;

    @FXML
    private CheckBox enabledfield;

    public ProxyController() {
    }

    @FXML
    public void initialize() {
        ObservableList<String> schemeOptions = FXCollections.observableArrayList();
        schemeOptions.add("http");
        schemeOptions.add("https");
        schemefield.setValue("http");
        schemefield.setItems(schemeOptions);
        fillData();
    }

    public void confirmProxy(ActionEvent actionEvent) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("properties.json")))) {
            Main.data.put("scheme", schemefield.getValue());
            Main.data.put("ip", ipfield.getText());
            String text = portfield.getText();
            Main.data.put("port", text.equals("") ? -1 : Integer.parseInt(text));
            Main.data.put("enabled", enabledfield.isSelected());
            writer.write(Main.data.toJSONString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Window curStage = ipfield.getScene().getWindow();
        curStage.hide();
    }

    private void fillData() {
        int port = Main.data.getIntValue("port");
        if (port != 0) {
            portfield.setText(port + "");
            ipfield.setText(Main.data.getString("ip"));
            schemefield.setValue(Main.data.getString("scheme"));
            enabledfield.setSelected(Main.data.getBooleanValue("enabled"));
            return;
        }
        portfield.setText("");
        ipfield.setText("");
        schemefield.setValue("");
        enabledfield.setSelected(false);
    }

    public void resetProxy(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            fillData();
        });
    }

    public void cacelSetting(ActionEvent actionEvent) {
        Stage curStage = (Stage) ipfield.getScene().getWindow();
        curStage.close();
    }

}