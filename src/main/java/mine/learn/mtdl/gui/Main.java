package mine.learn.mtdl.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    public static JSONObject data = null;
    static {
        try (InputStream in = new FileInputStream(new File("properties.json"))) {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            data = JSONObject.parseObject(builder.toString());
            builder = null;
        } catch (IOException e) {
            data = new JSONObject();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("downloader.fxml"));
        primaryStage.setTitle("多线程下载器 5.0 by Rxy");
        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image("logo.png"));
        primaryStage.setOnCloseRequest((event) -> {
            Platform.exit();
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
