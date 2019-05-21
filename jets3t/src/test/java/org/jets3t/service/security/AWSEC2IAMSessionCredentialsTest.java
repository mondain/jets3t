package org.jets3t.service.security;

import java.io.IOException;
import java.text.ParseException;

import org.jets3t.service.utils.ServiceUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import junit.framework.TestCase;

public class AWSEC2IAMSessionCredentialsTest extends TestCase {

    public void testParseEC2InstanceDataSuccess() throws JsonProcessingException, IOException, ParseException {
        String iamRoleData = "{\n" + "  \"Code\" : \"Success\",\n" + "  \"LastUpdated\" : \"2012-04-26T16:39:16Z\",\n" + "  \"Type\" : \"AWS-HMAC\",\n" + "  \"AccessKeyId\" : \"ABCDEFGHIJKLMNOP\",\n" + "  \"SecretAccessKey\" : \"afsdjkafsjdklajfdksa;jfkd;afjdks\",\n" + "  \"Token\" : \"TokeNtOkEn\",\n" + "  \"Expiration\" : \"2015-05-29T13:32:52Z\"\n" + "}";

        AWSEC2IAMSessionCredentials credentials = AWSEC2IAMSessionCredentials.parseEC2InstanceData(iamRoleData, "thisIsARole", false);

        assertEquals("ABCDEFGHIJKLMNOP", credentials.getAccessKey());
        assertEquals("afsdjkafsjdklajfdksa;jfkd;afjdks", credentials.getSecretKey());
        assertEquals("TokeNtOkEn", credentials.getSessionToken());
        assertEquals(ServiceUtils.parseIso8601Date("2015-05-29T13:32:52Z"), credentials.getExpiration());
        assertEquals("thisIsARole", credentials.getRoleName());
        assertFalse(credentials.isAutomaticRefreshEnabled());
    }

    public void testParseEC2InstanceDataError() throws JsonProcessingException, IOException, ParseException {
        String iamRoleData = "{\n" + "  \"Code\" : \"UhOhNoes\",\n" + "  \"LastUpdated\" : \"2012-04-26T16:39:16Z\",\n" + "  \"Type\" : \"AWS-HMAC\",\n" + "  \"AccessKeyId\" : \"ABCDEFGHIJKLMNOP\",\n" + "  \"SecretAccessKey\" : \"afsdjkafsjdklajfdksa;jfkd;afjdks\",\n" + "  \"Token\" : \"TokeNtOkEn\",\n" + "  \"Expiration\" : \"2015-05-29T13:32:52Z\"\n" + "}";

        try {
            AWSEC2IAMSessionCredentials.parseEC2InstanceData(iamRoleData, "thisIsARole", false);
            fail("Expected failure");
        } catch (RuntimeException ex) {
            assertEquals("Status 'Code' != 'Success'", ex.getMessage());
        }
    }

}
