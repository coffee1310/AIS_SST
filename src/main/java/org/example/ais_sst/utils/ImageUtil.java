package org.example.ais_sst.utils;

import java.util.Base64;

public class ImageUtil {

    public static String encodeToBase64(byte[] image) {
        if (image == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(image);
    }

    public static byte[] decodeFromBase64(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            return null;
        }
        // Если строка содержит префикс data:image/png;base64,
        // нужно удалить его перед декодированием
        if (base64Image.contains(",")) {
            base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
        }
        return Base64.getDecoder().decode(base64Image);
    }
}