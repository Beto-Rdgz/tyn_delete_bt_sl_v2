package com.liverpool.imageValidator.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class Util {

    public static String makeUrl(String url, List<String> lisInventory) {
        for (String values : lisInventory) {
            url += values + ",";

        }
        StringBuilder str = new StringBuilder(url);
        str.deleteCharAt(url.length() - 1);
        return str.toString();
    }

    public static String encode(String value) {
        byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(stringBytes);

    }

    public static String decode(String value) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(value);
        return new String(decodedBytes);
    }
}
