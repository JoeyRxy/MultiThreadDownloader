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
                        alert.setTitle("错误的格式");
                        alert.setHeaderText(null);
                        alert.setContentText("应满足SHA-256格式的签名（长64位的16进制字符串），没有可不填");
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
                    proxyonoff.setText("不使用");
                } else {
                    proxy.setStyle("-fx-opacity:0.5");
                    proxyonoff.setText("使用");
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
        dialog.setTitle("警告");
        ButtonType type = new ButtonType("知道了", ButtonData.OK_DONE);
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
                alertMsg("文件名不能为空");
                start.setText("开始");
            });
            return;
        }
        String urlStr = url.getText();
        if (!isStart && requiredCheck.matcher(urlStr).matches()) {
            Platform.runLater(() -> {
                alertMsg("URL不能为空");
                start.setText("开始");
            });
            return;
        }
        if (status == Status.UnStart) {// 未开始，开始
            Platform.runLater(new StartChangeRunnable("暂停"));
            String defaultRoot = Main.data.getString("defaultRoot");
            File file;
            if (defaultRoot == null || defaultRoot.equals("")) {
                file = new File(fileNameStr);
            } else {
                file = new File(defaultRoot, fileNameStr);
            }
            if (file.exists() && file.length() > 0) {
                alertMsg("文件 " + file.getName() + " 已存在且有内容，下载未开始");
                Platform.runLater(() -> {
                    start.setText("开始");
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
                        Text _txt = new Text("代理格式错误，不使用代理");
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
                start.setText("开始");
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
        } else if (status == Status.Paused) {// 暂停中，继续
            Platform.runLater(new StartChangeRunnable("暂停"));
            status = Status.Running;
            if (downloader != null)
                downloader.resumeAll();
        } else {// 运行中，暂停
            Platform.runLater(new StartChangeRunnable("继续"));
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
                                proxyonoff.setText("不使用");
                            } else {
                                proxy.setStyle("-fx-opacity:0.5");
                                proxyonoff.setText("使用");
                            }
                        });
                    }
                });
            });
            stage.setTitle("设置默认代理");
            stage.setScene(new Scene(root));
            stage.getIcons().add(LOGO_IMAGE);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String aboutString = "== VERSION :5.0 ==\n\n注意，由于Oracle对加密模块的保护政策，该版本需运行在OpenJDK或其他非Oracle环境下\n\n1. 支持动态线程开启，即每个爬取线程时可动态再次开启多个线程。\n后期速度不至于下降过快。\n不过大多数情况下其实差不多……2. \n使用的请求方式是Get\n3. 支持HLS部分协议，可以下载m3u8文件。\n支持的加密标签。不支持双层m3u8（大多数都是单层）4. 关于代理：\n对于shadowsocks或者trojan-Qt5之类的软件，\n在每个服务器属性中都提供了本地地址、端口等信息，即为所需的代理。\n对于在自己服务器上搭建的代理，输入相应配置即可。\n\t1. 关于线程数：线程数如果过多，导致每个线程的下载大小小于 64KB 时，会取以64KB每线程的线程数\n\t2. 对于不可进行多线程下载的文件：（不能使用Range头的情况）\n\t会使用单线程下载，这种情况下，下载进度条和长度等信息无效。\n\t（大多数情况是代理等网络问题，所以会提前结束）\n\n应用中的一些常用设置可进行自定义，不必每次都进行相应操作：\n- 关于默认代理：\n\t会在同文件路径下生成一个properties.json文件，记录代理情况。\n- 关于默认文件下载位置：\n\t在应用的“保存为”栏目里表示的是文件的名字，而文件的存储位置，由“设置 -> 默认下载位置”设定。\n\n本应用处于开发阶段，很多异常都只进行了抛出而没有弹窗警告等信息，\n如网络失败等情况没有提示，甚至会提示下载完成，但实际上文件并没有下载。\n\n\n\tRxy";

    public void showAbout(ActionEvent actionEvent) {
        Dialog<String> dialog = new Dialog<String>();
        dialog.setTitle("关于");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(LOGO_IMAGE);
        ButtonType type = new ButtonType("知道了", ButtonData.OK_DONE);
        dialog.setContentText(aboutString);
        dialog.getDialogPane().getButtonTypes().add(type);
        dialog.showAndWait();
        // Parent root =
        // FXMLLoader.load(getClass().getClassLoader().getResource("about.fxml"));
        // Stage stage = new Stage();
        // stage.setTitle("关于");
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
        stage.setTitle("默认下载位置");
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
        stage.setTitle("下载完成");
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
                proxyonoff.setText("使用");
            });
        } else {
            proxyDisabled = false;
            proxy.setStyle("-fx-opacity:1");
            Platform.runLater(() -> {
                proxyonoff.setText("不使用");
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

                    Text txt = new Text("下载完成！位于：" + file.getAbsolutePath());
                    txt.setFill(Color.GREEN);
                    txt.setFont(new Font(txt.getFont().getSize() * 2));
                    output.getChildren().add(txt);
                    //
                    showDonePane("原链接：" + url.getText(), file);

                    if (checksum.getText().length() > 0 && !check(file))
                        alertMsg("校验不符和checksum，文件可能已破坏");

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
            start.setText("开始");
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
            Text txt = new Text(String.format("线程 %d: 已下载/总大小: %.2f KB/%.2f KB at %s", e.getId(),
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
