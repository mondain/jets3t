package org.jets3t.service.utils;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;


public class ServiceUtilsTest extends TestCase {

    public void testParseIso8601Date() throws Exception {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // Parse default expected date string: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        calendar.setTime(ServiceUtils.parseIso8601Date("2015-11-08T14:39:23.123Z"));
        assertEquals(0, calendar.get(Calendar.ZONE_OFFSET));
        assertEquals(2015, calendar.get(Calendar.YEAR));
        assertEquals(10, calendar.get(Calendar.MONTH));
        assertEquals(8, calendar.get(Calendar.DATE));
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(39, calendar.get(Calendar.MINUTE));
        assertEquals(23, calendar.get(Calendar.SECOND));
        assertEquals(123, calendar.get(Calendar.MILLISECOND));

        // Parse fallback date string without MS: "yyyy-MM-dd'T'HH:mm:ss'Z'"
        calendar.setTime(ServiceUtils.parseIso8601Date("2015-11-08T14:39:23Z"));
        assertEquals(0, calendar.get(Calendar.ZONE_OFFSET));
        assertEquals(2015, calendar.get(Calendar.YEAR));
        assertEquals(10, calendar.get(Calendar.MONTH));
        assertEquals(8, calendar.get(Calendar.DATE));
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(39, calendar.get(Calendar.MINUTE));
        assertEquals(23, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));

        // Parse alternative date string used by Eucalyptus Walrus service
        // that is no in the UTC timezone: "yyyy-MM-dd'T'HH:mm:ss"
        calendar = Calendar.getInstance();  // Reset to local time zone
        calendar.setTime(ServiceUtils.parseIso8601Date("2015-11-08T14:39:23"));
        assertEquals(2015, calendar.get(Calendar.YEAR));
        assertEquals(10, calendar.get(Calendar.MONTH));
        assertEquals(8, calendar.get(Calendar.DATE));
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(39, calendar.get(Calendar.MINUTE));
        assertEquals(23, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

}
