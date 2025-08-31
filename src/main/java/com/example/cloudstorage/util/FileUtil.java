package com.example.cloudstorage.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtil {
    public static String generateUniqueFilename(String originalFilename) {
        return System.currentTimeMillis() + "_" + originalFilename;
    }

    public static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
}
