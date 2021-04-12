package mine.learn.mtdl.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class FileRootController {
    @FXML
    private TextField fileRoot;

    @FXML
    public void initialize() {
        String txt = new File("").getAbsolutePath();
        String tmp = Main.data.getString("defaultRoot");
        if (!(tmp == null || "".equals(tmp))) {
            txt = tmp;
        }
        fileRoot.setText(txt);
    }

    public void chooseFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("默认下载文件夹");
        File directory = chooser.showDialog(new Stage());
        fileRoot.setText(directory.getAbsolutePath());
    }

    public void confirm(ActionEvent event) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("properties.json")));
        String text = fileRoot.getText();
        File directory = new File(text);
        if (!directory.exists())
            directory.mkdirs();
        Main.data.put("defaultRoot", text);
        writer.write(Main.data.toJSONString());
        writer.close();
        Stage curStage = (Stage) fileRoot.getScene().getWindow();
        curStage.close();
    }
}