package org.jets3t.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jets3t.service.io.SegmentedRepeatableFileInputStream;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.utils.ServiceUtils;

import junit.framework.TestCase;

public class TestUtilities extends TestCase {

    public void testIsEtagAlsoAnMD5Hash() {
        // Valid MD5 ETag value
        assertTrue(ServiceUtils.isEtagAlsoAnMD5Hash(
            "cb8aaa4056eb349af16b1907280dee18"));

        // Non-MD5 ETag values
        assertFalse(ServiceUtils.isEtagAlsoAnMD5Hash(
            "cb8aaa4056eb349af16b1907280dee18-1"));

        assertFalse(ServiceUtils.isEtagAlsoAnMD5Hash(
            "Xcb8aaa4056eb349af16b1907280dee1"));

        assertFalse(ServiceUtils.isEtagAlsoAnMD5Hash(
            "cb8aaa4056eb349af16b1907280dee1"));

        assertFalse(ServiceUtils.isEtagAlsoAnMD5Hash(
            "cb8aaa4056eb349af16b1907280dee18cb"));
    }

    public void testMd5ETag() throws Exception {
        StorageObject so = new S3Object("");

        // MD5 ETag values
        so.setETag("cb8aaa4056eb349af16b1907280dee18");
        assertEquals("y4qqQFbrNJrxaxkHKA3uGA==", so.getMd5HashAsBase64());
    }

    public void testNonMd5ETag() throws Exception {
        StorageObject so = new S3Object("");

        so.setETag("cb8aaa4056eb349af16b1907280dee18-1");
        assertEquals(null, so.getMd5HashAsBase64());

        so.setETag("cb8aaa4056eb349af16b1907280dee");
        assertEquals(null, so.getMd5HashAsBase64());

        so.setETag("cb8aaa4056eb349af16b1907280dee1");
        assertEquals(null, so.getMd5HashAsBase64());

        so.setETag("cb8aaa4056eb349af16b1907280dee18cb");
        assertEquals(null, so.getMd5HashAsBase64());

        so.setETag("12345");
        String hash = so.getMd5HashAsBase64();
        assertEquals(null, hash);

        so.setETag("123456-7");
        hash = so.getMd5HashAsBase64();
        assertEquals(null, hash);
    }

    public void testSegmentedRepeatableFileInputStream() throws Exception {
        File testFile = File.createTempFile("JetS3t-testSegmentedRepeatableFileInputStream", null);
        int read = -1;
        SegmentedRepeatableFileInputStream segFIS = null;

        // Create a 1 MB file for testing
        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(testFile));
        long testFileLength = 1 * 1024 * 1024;
        int offset = 0;
        while (offset < testFileLength) {
            bos.write((offset % 256));
            offset++;
        }
        bos.close();

        // Invalid segment length
        try {
            segFIS = new SegmentedRepeatableFileInputStream(testFile, 0, 0);
            fail("SegmentedRepeatableFileInputStream accepted invalid arguments");
        } catch (IllegalArgumentException e) { }

        // Segment exceeds file length
        try {
            segFIS = new SegmentedRepeatableFileInputStream(testFile, 0, testFileLength + 1);
            fail("SegmentedRepeatableFileInputStream accepted invalid arguments");
        } catch (IllegalArgumentException e) { }

        // Offset beyond file length
        try {
            segFIS = new SegmentedRepeatableFileInputStream(testFile, testFileLength + 1, 1);
            fail("SegmentedRepeatableFileInputStream accepted invalid arguments");
        } catch (IllegalArgumentException e) { }

        // Offset and segment length exceed file length
        try {
            segFIS = new SegmentedRepeatableFileInputStream(testFile, testFileLength - 1, 2);
            fail("SegmentedRepeatableFileInputStream accepted invalid arguments");
        } catch (IllegalArgumentException e) { }

        // Just right
        try {
            segFIS = new SegmentedRepeatableFileInputStream(testFile, testFileLength - 1, 1);
            segFIS.close();
        } catch (IllegalArgumentException e) {
            fail("SegmentedRepeatableFileInputStream failed to accept valid arguments");
        }

        segFIS = new SegmentedRepeatableFileInputStream(testFile, 0, 10);

        // Ensure reads stay within segment boundaries...
        long byteCount = 0;
        byte[] buffer = new byte[256];

        // Read entire segment 1 byte at a time
        segFIS.reset();
        byteCount = 0;
        while ((read = segFIS.read()) != -1) {
            byteCount++;
        }
        assertEquals("Read beyond segment length", 10, byteCount);

        // Read entire segment using buffer
        segFIS.reset();
        byteCount = 0;
        while ((read = segFIS.read(buffer, 0, buffer.length)) != -1) {
            byteCount += read;
        }
        assertEquals("Read beyond segment length", 10, byteCount);

        // Read partial segment with offset using buffer
        segFIS.reset();
        byteCount = 0;
        offset = 5;
        while ((read = segFIS.read(buffer, offset, (int) (5 - byteCount) )) != -1) {
            byteCount += read;
            offset = 0;
        }
        assertEquals("Read beyond segment length with offset", 5, byteCount);

        // Read starting beyond segment
        segFIS.reset();
        byteCount = 0;
        offset = 10;
        while ((read = segFIS.read(buffer, offset, buffer.length)) != -1) {
            byteCount += read;
        }
        assertEquals("Read beyond segment length", 0, byteCount);

        segFIS.close();

        // Check valid data read from segment...

        // Read entire file
        segFIS = new SegmentedRepeatableFileInputStream(testFile, 0, testFileLength);
        offset = 0;
        while ((read = segFIS.read()) != -1) {
            assertEquals("Read unexpected byte at offset " + offset,
                (offset % 256), read);
            offset++;
        }
        segFIS.close();

        // Read an offset segment of file
        offset = 1234;
        segFIS = new SegmentedRepeatableFileInputStream(
            testFile, offset, testFileLength - offset);
        while ((read = segFIS.read()) != -1) {
            assertEquals("Read unexpected byte at offset " + offset,
                (offset % 256), read);
            offset++;
        }
        segFIS.close();

        // Set a mark point, read from it, then reset to that point.
        segFIS = new SegmentedRepeatableFileInputStream(
            testFile, 0, testFileLength);
        int targetOffset = 12345;
        offset = 0;
        while (offset < targetOffset && (read = segFIS.read()) != -1) {
            offset++;
        }
        assertEquals("Couldn't read up to target offset", targetOffset, offset);
        assertEquals("Unexpected amount of data available",
            testFileLength - targetOffset, segFIS.available());
        segFIS.mark(0);
        while ((read = segFIS.read()) != -1) {
            assertEquals("Read unexpected byte at offset " + offset,
                (offset % 256), read);
            offset++;
        }
        segFIS.reset();
        assertEquals("Unexpected amount of data available",
            testFileLength - targetOffset, segFIS.available());
        offset = targetOffset;
        while ((read = segFIS.read()) != -1) {
            assertEquals("Read unexpected byte at offset " + offset,
                (offset % 256), read);
            offset++;
        }
        assertEquals("Didn't read expected number of bytes after reset",
            testFileLength, offset);
        segFIS.close();


        // Multiple overlapping segments...
        int[] offsets = new int[] { 0, 1000, 1080, 3500};
        long[] expectedBytesRead = new long[] { 1234, 2000, 3000, 1 };
        int i = 0;
        SegmentedRepeatableFileInputStream[] segFISs =
            new SegmentedRepeatableFileInputStream[offsets.length];
        for (i = 0; i < segFISs.length; i++) {
            segFISs[i] = new SegmentedRepeatableFileInputStream(
                testFile, offsets[i], expectedBytesRead[i]);
        };

        // Test correct bytes read, and correct segment lengths read
        for (i = 0; i < segFISs.length; i++) {
            segFIS = segFISs[i];
            offset = offsets[i];
            byteCount = 0;
            while ((read = segFIS.read()) != -1) {
                assertEquals("Read unexpected byte at offset " + offset,
                    (offset % 256), read);
                offset++;
                byteCount++;
            }
            assertEquals("Didn't read expected segment length for segment " + i,
                expectedBytesRead[i], byteCount);
            segFIS.close();
        }
    }

    public void testCaseInsensitiveObjectMetadataNames() {
        StorageObject obj = new StorageObject("SomeName");

        // Get metadata names, case-insensitive
        obj.addMetadata("My-name", "1");
        assertEquals("1", obj.getMetadata("My-name"));
        assertEquals("1", obj.getMetadata("My-Name"));
        assertEquals("1", obj.getMetadata("my-name"));

        assertNull(obj.getMetadata(null));

        // Check for presense of metadata, case-insensitive
        assertTrue(obj.containsMetadata("My-name"));
        assertTrue(obj.containsMetadata("My-Name"));
        assertTrue(obj.containsMetadata("my-name"));

        // New item with same case-insensitive name replaces old value
        obj.addMetadata("My-Name", "2");
        assertEquals("2", obj.getMetadata("My-name"));
        assertEquals("2", obj.getMetadata("My-Name"));
        assertEquals("2", obj.getMetadata("my-name"));

        // Null metadata names are allowed (though a bad idea...)
        obj.addMetadata(null, "3");
        assertEquals("3", obj.getMetadata(null));
        obj.addMetadata(null, "4");
        assertEquals("4", obj.getMetadata(null));

        // Last add operation with matching case-insensitive name wins
        obj.addMetadata("CaseInsensitive", "5");
        obj.addMetadata("Caseinsensitive", "6");
        obj.addMetadata("caseinsensitive", "7");
        obj.addMetadata("CASEINSENSITIVE", "8");
        assertEquals("8", obj.getMetadata("CaseInsensitive"));

        // Remove item is also case-insensitive
        assertEquals(3, obj.getMetadataMap().size()); // Items added so far
        obj.removeMetadata("my-namE");
        obj.removeMetadata(null);
        obj.removeMetadata("CASEinsensitive");

        // Add all
        Map<String, Object> newMetadata = new HashMap<String, Object>();
        newMetadata.put("FIRST", "1st");
        newMetadata.put("second", "2nd");
        newMetadata.put("thIrd", "3rd");
        obj.addAllMetadata(newMetadata);
        assertEquals("1st", obj.getMetadata("first"));
        assertEquals("2nd", obj.getMetadata("SECOND"));
        assertEquals("3rd", obj.getMetadata("THiRD"));

        // Replace all
        newMetadata = new HashMap<String, Object>();
        newMetadata.put("one", "1st");
        newMetadata.put("TWO", "2nd");
        newMetadata.put("THRee", "3rd");
        obj.replaceAllMetadata(newMetadata);
        assertEquals(3, obj.getMetadataMap().size());
        assertEquals("1st", obj.getMetadata("ONE"));
        assertEquals("2nd", obj.getMetadata("two"));
        assertEquals("3rd", obj.getMetadata("thrEE"));
    }

}
