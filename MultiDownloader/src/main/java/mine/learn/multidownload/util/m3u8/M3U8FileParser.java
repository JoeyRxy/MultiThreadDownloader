package mine.learn.multidownload.util.m3u8;

import java.net.URI;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8FileParser {
    private final static Pattern uriPattern = Pattern.compile(
            "^(https?://)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
    private final static Pattern extinfPattern = Pattern.compile("EXTINF:([0-9.]*).*?\\n(.*?)\\n");

    private final static Pattern keyPattern = Pattern
            .compile("EXT-X-KEY *: *METHOD= *AES-128 *, *URI *= *\"(?<uri>.*?)\" *(, *IV *= *(?<iv>(0x)?\\d{32}))?\n?");

    public static M3U8Info parse(String text, String baseURI) {
        if (baseURI.charAt(baseURI.length() - 1) != '/')
            baseURI += "/";
        M3U8Info m3u8Info = new M3U8Info();
        String[] parts = text.split("#");
        int mediaSequence = -1;
        EXTKEY key = new EXTKEY();
        boolean encrypted = false;
        for (String part : parts) {
            if (part.startsWith("EXT-X-KEY")) {
                encrypted = true;
                Matcher matcher = keyPattern.matcher(part);
                if (!matcher.matches())
                    throw new IllegalArgumentException("M3U8的加密参数不符合要求：" + part);
                key.setMethod("AES-128");
                String uri = matcher.group("uri");
                if (!uriPattern.matcher(uri).matches() || !uri.startsWith("https://"))
                    uri = baseURI + uri;
                key.setUri(URI.create(uri));
                String iv = matcher.group("iv");
                if (iv != null)
                    key.setIv(iv);
            } else if (part.startsWith("EXTINF")) {
                Matcher matcher = extinfPattern.matcher(part);
                if (!matcher.matches()) {
                    System.err.println(part);
                    return null;
                }
                String uri = matcher.group(2);
                if (!uri.startsWith("https://") && uriPattern.matcher(uri).matches())
                    uri = baseURI + uri;
                m3u8Info.add(new EXTINF(Float.parseFloat(matcher.group(1)), URI.create(uri)));
            } else if (part.startsWith("EXT-X-MEDIA-SEQUENCE")) {
                part = part.substring(part.indexOf(":") + 1);
                if (part.charAt(part.length() - 1) == '\n')
                    part = part.substring(0, part.length() - 1);
                mediaSequence = Integer.parseInt(part);
            }
        }
        if (encrypted) {
            if (key.getIv() == null) {
                if (mediaSequence == -1)
                    throw new IllegalArgumentException("M3U8加密错误");
                key.setIv("0x" + paddingHexString(mediaSequence));
            }
            m3u8Info.setKey(key);
        }
        return m3u8Info;
    }

    private static String paddingHexString(int decimal) {
        String hexString = Integer.toHexString(decimal);
        byte[] b = new byte[32 - hexString.length()];
        Arrays.fill(b, (byte) 48);
        return new String(b) + hexString;
    }

}