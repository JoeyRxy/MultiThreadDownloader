package mine.learn.multidownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.http.HttpHost;
import org.junit.Test;

public class AppTest {

    private static final HttpHost proxy = HttpHost.create("localhost:7890");

    @Test
    public void test() throws Exception {
        String url = "https://www.eso.org/public/archives/images/publicationtiff40k/eso1242a.tif";
        File file = new File(url.substring(url.lastIndexOf("/") + 1));
        Downloader downloader = DownloaderBuilder.create().setFile(file).setUri(url).setThreadNum(2048).build();

        downloader.registerLogListener(System.out::println);
        // downloader.registerThreadRetryLog(System.out::println);

        long start = System.currentTimeMillis();
        downloader.run();
        long end = System.currentTimeMillis();
        System.out.println("avg. speed: " + (downloader.alreadyDone()) / (end - start) + " KB/s");

        System.out.println("====== Verifing ======");
        MessageDigest md = MessageDigest.getInstance("sha-256");
        FileInputStream fis = new FileInputStream(file);

        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = fis.read(buf)) != -1)
            md.update(buf, 0, len);

        fis.close();
        String checksum = "b64451a69f2f4bf3184c1bdad5177dbe4da921a64795949cdb640ecef8391c0f";
        StringBuilder builder = new StringBuilder();
        byte[] digest = md.digest();
        for (int i = 0; i < digest.length; i++) {
            builder.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }
        String fileHash = builder.toString();
        builder = null;
        System.out.println("already done: " + downloader.alreadyDone());
        System.out.println("File size : " + file.length());
        System.out.println("File Hash: \n" + fileHash);
        System.out.println("verify res : " + fileHash.equalsIgnoreCase(checksum));
        file.delete();
    }

}