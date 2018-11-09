package org.jets3t.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestFileUtils {

    public static File createTempFileWithSize(
        String name, String suffix, long byteCount)
    {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(name, suffix);
            BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(tempFile));
            int offset = 0;
            while (offset < byteCount) {
                bos.write((offset++ % 256));
            }
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

}
