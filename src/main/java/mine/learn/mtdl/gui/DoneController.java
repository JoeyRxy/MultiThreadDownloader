package mine.learn.mtdl.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DoneController implements Initializable {

    @FXML
    private Text msg;

    @FXML
    private Button openfileloc;

    @FXML
    private Button openfile;

    private final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        msg.setText((String) resources.getObject("msg"));
        File file = (File) resources.getObject("file");
        if (desktop == null) {
            Controller.alertMsg("当前操作系统可能不支持文件操作");
            openfile.setVisible(false);
            openfileloc.setVisible(false);
        } else {
            if (desktop.isSupported(Desktop.Action.OPEN))
                openfile.setOnMouseClicked(event -> {
                    try {
                        desktop.open(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            else
                openfile.setVisible(false);
            if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR))
                openfileloc.setOnMouseClicked(event -> {
                    desktop.browseFileDirectory(file);
                });
            else if(desktop.isSupported(Desktop.Action.OPEN)) 
                openfileloc.setOnMouseClicked(event->{
                    try {
                        desktop.open(file.getParentFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            else
                openfileloc.setVisible(false);
            ;
        }
    }

    public void dismiss(ActionEvent event) {
        Stage curStage = (Stage) msg.getScene().getWindow();
        curStage.close();
    }

}
