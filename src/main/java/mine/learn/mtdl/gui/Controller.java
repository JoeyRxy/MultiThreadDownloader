package mine.learn.mtdl.gui;

import java.awt.Desktop;
// import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mine.learn.multidownload.Downloader;
import mine.learn.multidownload.DownloaderBuilder;
import mine.learn.multidownload.util.Log;
import mine.learn.multidownload.util.ThreadLog;

public class Controller {

    /**
     *
     */
    private static final Image LOGO_IMAGE = new Image("logo.png");

    @FXML
    private TextField fileName;

    @FXML
    private TextArea cookie;

    @FXML
    private TextField referer;

    @FXML
    private TextField url;

    @FXML
    private TextField proxy;

    @FXML
    private Button start;

    @FXML
    private ComboBox<Integer> threadNum;

    @FXML
    private ProgressBar progress;

    @FXML
    private VBox output;

    @FXML
    private Label speed;

    @FXML
    private Label activethreads;

    @FXML
    private Label totalthreads;

    @FXML
    private Label contentlengthfx;

    @FXML
    private Label alreadyfx;

    @FXML
    private ScrollPane myscroll;

    @FXML
    private Label remainTime;

    private Downloader downloader;

    private boolean isStart = false;

    private long contentLength;

    public Controller() {
    }

    private String parseFileName(String url) {
        int i = url.lastIndexOf("?");
        String fileName = url.substring(url.lastIndexOf("/") + 1, i == -1 ? url.length() : i);
        File file = new File(fileName);
        int ext = 1;
        String newFileName;
        while (file.exists() && file.length() > 0) {
            int mainIdx = fileName.lastIndexOf(".");
            if (mainIdx == -1)
                newFileName = fileName + ext;
            else
                newFileName = fileName.substring(0, mainIdx) + ext + fileName.substring(mainIdx);
            file = new File(newFileName);
            ext++;
        }
        return file.getName();
    }

    @FXML
    public void initialize() {
        ObservableList<Integer> threadNumOptions = FXCollections.observableArrayList();
        threadNumOptions.add(4);
        threadNumOptions.add(8);
        threadNumOptions.add(16);
        threadNumOptions.add(32);
        threadNumOptions.add(48);
        threadNumOptions.add(64);
        threadNumOptions.add(128);
        threadNumOptions.add(256);
        threadNumOptions.add(384);
        threadNumOptions.add(512);
        threadNumOptions.add(1024);
        threadNumOptions.add(2046);
        threadNumOptions.add(4096);
        threadNumOptions.add(4096 << 1);
        threadNumOptions.add(4096 << 2);
        threadNum.setValue(Runtime.getRuntime().availableProcessors() << 2);
        threadNum.setItems(threadNumOptions);
        url.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            String fileNameParsed = parseFileName(newValue);
            Platform.runLater(() -> {
                fileName.setText(fileNameParsed);
            });
        });
        checksum.focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue.booleanValue()) {
                    String str = checksum.getText();
                    if (str.length() == 0)
                        return;
                    if (!str.matches("[0-9a-fA-F]{64}")) {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setOnCloseRequest(event -> {
                            Platform.runLater(() -> {
                                checksum.requestFocus();
                            });
                        });
                        alert.setTitle("???????????????");
                        alert.setHeaderText(null);
                        alert.setContentText("?????????SHA-256?????????????????????64??????16????????????????????????????????????");
                        alert.showAndWait();
                    }
                }
            }

        });
        int port = Main.data.getIntValue("port");
        if (port != 0) {
            proxy.setText(Main.data.getString("scheme") + "://" + Main.data.getString("ip") + ":" + port);
            proxyDisabled = !Main.data.getBooleanValue("enabled");
            Platform.runLater(() -> {
                if (!proxyDisabled) {
                    proxy.setStyle("-fx-opacity:1");
                    proxyonoff.setText("?????????");
                } else {
                    proxy.setStyle("-fx-opacity:0.5");
                    proxyonoff.setText("??????");
                }
            });
        }
        progress.setStyle("-fx-accent: #ff5100;");
        parentnode.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                startAndPause(null);
        });
    }

    @FXML
    private VBox parentnode;

    Pattern requiredCheck = Pattern.compile(" *");

    private Status status = Status.UnStart;

    private static final Image ERROR_IMAGE = new Image("error.png");

    public static void alertMsg(String msg) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("??????");
        ButtonType type = new ButtonType("?????????", ButtonData.OK_DONE);
        dialog.setContentText(msg);
        dialog.getDialogPane().getButtonTypes().add(type);
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(ERROR_IMAGE);
        dialog.showAndWait();
    }

    public void startAndPause(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            progress.setProgress(0);
            progress.setStyle("-fx-accent: #ff5100;");
        });
        String fileNameStr = fileName.getText();
        if (!isStart && requiredCheck.matcher(fileNameStr).matches()) {
            Platform.runLater(() -> {
                alertMsg("?????????????????????");
                start.setText("??????");
            });
            return;
        }
        String urlStr = url.getText();
        if (!isStart && requiredCheck.matcher(urlStr).matches()) {
            Platform.runLater(() -> {
                alertMsg("URL????????????");
                start.setText("??????");
            });
            return;
        }
        if (status == Status.UnStart) {// ??????????????????
            Platform.runLater(new StartChangeRunnable("??????"));
            String defaultRoot = Main.data.getString("defaultRoot");
            File file;
            if (defaultRoot == null || defaultRoot.equals("")) {
                file = new File(fileNameStr);
            } else {
                file = new File(defaultRoot, fileNameStr);
            }
            if (file.exists() && file.length() > 0) {
                alertMsg("?????? " + file.getName() + " ???????????????????????????????????????");
                Platform.runLater(() -> {
                    start.setText("??????");
                });
                return;
            }
            Set<Header> headers = new HashSet<>();
            String cookieStr = cookie.getText();
            if (!cookieStr.equals("")) {
                headers.add(new BasicHeader("Cookie", cookieStr));
            }
            HttpHost _proxy = null;
            String proxyStr = proxy.getText();
            if (!proxyStr.equals("")) {
                try {
                    _proxy = getProxy(proxyStr);
                } catch (MalformedURLException e) {
                    Platform.runLater(() -> {
                        Text _txt = new Text("????????????????????????????????????");
                        _txt.setFill(Color.RED);
                        output.getChildren().add(_txt);
                        myscroll.setVvalue(1.0);
                    });
                }
            }
            String refStr = referer.getText();
            if (!refStr.equals("")) {
                headers.add(new BasicHeader("referer", refStr));
            }
            tmpRoot = new File(file.getParentFile(), "tmp");
            try {
                downloader = DownloaderBuilder.create().setFile(file).setTempFileRoot(tmpRoot).setUri(urlStr)
                        .setThreadNum(threadNum.getValue()).setHeaders(headers).setProxy(proxyDisabled ? null : _proxy)
                        .build();
            } catch (Exception e1) {
                alertMsg(e1.getMessage());
                start.setText("??????");
                return;
            }
            contentLength = downloader.getContentLength();
            Platform.runLater(() -> {
                contentlengthfx.setText(contentLength > 0 ? String.format("%.2f KB", contentLength / 1024.) : "-- KB");
            });
            downloader.registerLogListener(e -> {
                Platform.runLater(new NormalLogRunnable(e));
            });
            downloader.registerThreadRetryLog(e -> {
                Platform.runLater(new FailedLogRunnable(e));
            });
            Platform.runLater(() -> {
                threadNum.setDisable(true);
                url.setDisable(true);
                checksum.setDisable(true);
                fileName.setDisable(true);
                cookie.setDisable(true);
                referer.setDisable(true);
                proxy.setDisable(true);
                proxyonoff.setDisable(true);
            });
            if (downloader != null) {
                DownloadContainer container = new DownloadContainer(downloader);
                container.start();
                status = Status.Running;
            }
        } else if (status == Status.Paused) {// ??????????????????
            Platform.runLater(new StartChangeRunnable("??????"));
            status = Status.Running;
            if (downloader != null)
                downloader.resumeAll();
        } else {// ??????????????????
            Platform.runLater(new StartChangeRunnable("??????"));
            status = Status.Paused;
            if (downloader != null)
                downloader.pauseAll();
        }
    }

    public void cancelOpt(ActionEvent actionEvent) {
        isStart = false;
        if (downloader != null && tmpRoot.exists()) {
            for (File tmpFiles : tmpRoot.listFiles()) {
                tmpFiles.deleteOnExit();
            }
            tmpRoot.deleteOnExit();
        }
        System.exit(0);
    }

    private HttpHost getProxy(String value) throws MalformedURLException {
        URL url = new URL(value);
        return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
    }

    public void showProxySetting(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("proxysetting.fxml"));
            Stage stage = new Stage();
            stage.setOnHidden((event) -> {
                Platform.runLater(() -> {
                    int port = Main.data.getIntValue("port");
                    if (port != -1) {
                        proxy.setText(Main.data.getString("scheme") + "://" + Main.data.getString("ip") + ":" + port);
                        proxyDisabled = !Main.data.getBooleanValue("enabled");
                        Platform.runLater(() -> {
                            if (!proxyDisabled) {
                                proxy.setStyle("-fx-opacity:1");
                                proxyonoff.setText("?????????");
                            } else {
                                proxy.setStyle("-fx-opacity:0.5");
                                proxyonoff.setText("??????");
                            }
                        });
                    }
                });
            });
            stage.setTitle("??????????????????");
            stage.setScene(new Scene(root));
            stage.getIcons().add(LOGO_IMAGE);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String aboutString = "== VERSION :5.0 ==\n\n???????????????Oracle??????????????????????????????????????????????????????OpenJDK????????????Oracle?????????\n\n1. ???????????????????????????????????????????????????????????????????????????????????????\n????????????????????????????????????\n?????????????????????????????????????????????2. \n????????????????????????Get\n3. ??????HLS???????????????????????????m3u8?????????\n???????????????????????????????????????m3u8???????????????????????????4. ???????????????\n??????shadowsocks??????trojan-Qt5??????????????????\n????????????????????????????????????????????????????????????????????????????????????????????????\n????????????????????????????????????????????????????????????????????????\n\t1. ????????????????????????????????????????????????????????????????????????????????? 64KB ???????????????64KB?????????????????????\n\t2. ????????????????????????????????????????????????????????????Range???????????????\n\t???????????????????????????????????????????????????????????????????????????????????????\n\t?????????????????????????????????????????????????????????????????????\n\n???????????????????????????????????????????????????????????????????????????????????????\n- ?????????????????????\n\t????????????????????????????????????properties.json??????????????????????????????\n- ?????????????????????????????????\n\t????????????????????????????????????????????????????????????????????????????????????????????????????????? -> ??????????????????????????????\n\n????????????????????????????????????????????????????????????????????????????????????????????????\n?????????????????????????????????????????????????????????????????????????????????????????????????????????\n\n\n\tRxy";

    public void showAbout(ActionEvent actionEvent) {
        Dialog<String> dialog = new Dialog<String>();
        dialog.setTitle("??????");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(LOGO_IMAGE);
        ButtonType type = new ButtonType("?????????", ButtonData.OK_DONE);
        dialog.setContentText(aboutString);
        dialog.getDialogPane().getButtonTypes().add(type);
        dialog.showAndWait();
        // Parent root =
        // FXMLLoader.load(getClass().getClassLoader().getResource("about.fxml"));
        // Stage stage = new Stage();
        // stage.setTitle("??????");
        // stage.setScene(new Scene(root));
        // stage.getIcons().add(new Image("logo.png"));
        // stage.show();
    }

    public void setDefaultRoot(ActionEvent event) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getClassLoader().getResource("fileRoot.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Stage stage = new Stage();
        stage.setTitle("??????????????????");
        stage.setScene(new Scene(root));
        stage.getIcons().add(LOGO_IMAGE);
        stage.show();
    }

    public void showDonePane(String msg, File file) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getClassLoader().getResource("done.fxml"),
                    new MineResourceBundle(msg, file));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Stage stage = new Stage();
        stage.setTitle("????????????");
        stage.setScene(new Scene(root));
        stage.getIcons().add(LOGO_IMAGE);
        stage.show();
    }

    static class MineResourceBundle extends ResourceBundle {

        private Map<String, Object> map;

        MineResourceBundle(String msg, File file) {
            map = new HashMap<>();
            map.put("msg", msg);
            map.put("file", file);
        }

        @Override
        protected Object handleGetObject(String key) {
            return map.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return new Vector<>(map.keySet()).elements();
        }

    }

    private boolean proxyDisabled;

    @FXML
    private Button proxyonoff;

    public void disableProxy(ActionEvent event) {
        if (!proxyDisabled) {
            proxyDisabled = true;
            proxy.setStyle("-fx-opacity:0.5");
            Platform.runLater(() -> {
                proxyonoff.setText("??????");
            });
        } else {
            proxyDisabled = false;
            proxy.setStyle("-fx-opacity:1");
            Platform.runLater(() -> {
                proxyonoff.setText("?????????");
            });
        }
    }

    private enum Status {
        Running, Paused, UnStart
    }

    private class DownloadContainer extends Thread {
        Downloader downloader;

        DownloadContainer(Downloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public void run() {
            try {
                downloader.run();
                Platform.runLater(() -> {
                    File file = downloader.getFile();

                    speed.setText("0 KB/s");
                    progress.setProgress(1);
                    progress.setStyle("-fx-accent: #3cff00;");
                    alreadyfx.setText(String.format("%.2f KB", downloader.getFile().length() / 1024.));
                    activethreads.setText("0");

                    Text txt = new Text("????????????????????????" + file.getAbsolutePath());
                    txt.setFill(Color.GREEN);
                    txt.setFont(new Font(txt.getFont().getSize() * 2));
                    output.getChildren().add(txt);
                    //
                    showDonePane("????????????" + url.getText(), file);

                    if (checksum.getText().length() > 0 && !check(file))
                        alertMsg("???????????????checksum????????????????????????");

                    reset();

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    myscroll.setVvalue(1.0);
                });
            } catch (IOException | IllegalBlockSizeException | BadPaddingException | InterruptedException e1) {
                alertMsg(e1.getMessage());
                reset();
                myscroll.setVvalue(1.0);
            }
        }
    }

    private boolean check(File file) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("sha-256");

            FileInputStream fis = new FileInputStream(file);

            byte[] buf = new byte[1024 * 1024];
            int len;
            while ((len = fis.read(buf)) != -1)
                md.update(buf, 0, len);

            fis.close();
            StringBuilder builder = new StringBuilder();
            byte[] digest = md.digest();
            for (int i = 0; i < digest.length; i++)
                builder.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));

            String fileHash = builder.toString();
            return fileHash.equalsIgnoreCase(checksum.getText());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

    private File tmpRoot;

    private void reset() {
        Platform.runLater(() -> {
            fileName.setDisable(false);
            fileName.setText("");
            cookie.setDisable(false);
            referer.setDisable(false);
            url.setDisable(false);
            url.setText("");
            url.requestFocus();
            proxy.setDisable(false);
            threadNum.setDisable(false);
            proxyonoff.setDisable(false);
            start.setText("??????");
            checksum.setDisable(false);
        });
        status = Status.UnStart;
    }

    private class NormalLogRunnable implements Runnable {

        private Log e;

        NormalLogRunnable(Log e) {
            this.e = e;
        }

        @Override
        public void run() {
            progress.setProgress(((double) e.getAlreadyDone() / contentLength));
            speed.setText(String.format("%.2f KB/s", e.getSpeed() / 1024.));
            activethreads.setText(e.getActiveThreadCount() + "");
            totalthreads.setText(e.getTotalExecutedTreadCount() + "");
            alreadyfx.setText(String.format("%.2f KB", e.getAlreadyDone() / 1024.));
            remainTime.setText(
                    contentLength > 0 ? secondToTime((long) ((contentLength - e.getAlreadyDone()) / e.getSpeed()))
                            : "-");
        }

    }

    private class FailedLogRunnable implements Runnable {
        private ThreadLog e;

        FailedLogRunnable(ThreadLog e) {
            this.e = e;
        }

        @Override
        public void run() {
            Text txt = new Text(String.format("?????? %d: ?????????/?????????: %.2f KB/%.2f KB at %s", e.getId(),
                    (e.getIdx() - e.getStart()) / 1024., (e.getEnd() - e.getStart()) / 1024., new Date().toString()));
            txt.setFill(Color.RED);
            output.getChildren().add(txt);
            myscroll.setVvalue(1.0);
        }

    }

    private class StartChangeRunnable implements Runnable {
        private String str;

        StartChangeRunnable(String str) {
            this.str = str;
        }

        @Override
        public void run() {
            start.setText(str);
        }

    }

    private String secondToTime(long second) {
        if (second <= 0)
            return "00:00:00";
        long days = second / 86400;
        second = second % 86400;
        long hours = second / 3600;
        second = second % 3600;
        long minutes = second / 60;
        second = second % 60;
        if (0 < days)
            return String.format("%d d %02d:%02d:%02d", days, hours, minutes, second);
        else
            return String.format("%02d:%02d:%02d", hours, minutes, second);
    }

    @FXML
    private TextField checksum;

}
