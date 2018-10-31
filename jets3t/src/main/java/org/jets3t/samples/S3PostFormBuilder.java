package org.jets3t.samples;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class S3PostFormBuilder {

    public static void main(String[] args) throws Exception {
        String aws_access_key = "YOUR AWS ACCESS KEY GOES HERE";
        String aws_secret_key = "YOUR AWS SECRET KEY GOES HERE";

        String policy_document =
          "{\"expiration\": \"2009-01-01T00:00:00Z\"," +
            "\"conditions\": [" +
              "{\"bucket\": \"s3-bucket\"}," +
              "[\"starts-with\", \"$key\", \"uploads/\"]," +
              "{\"acl\": \"private\"}," +
              "{\"success_action_redirect\": \"http://localhost/\"}," +
              "[\"starts-with\", \"$Content-Type\", \"\"]," +
              "[\"content-length-range\", 0, 1048576]" +
            "]" +
          "}";

        // Calculate policy and signature values from the given policy document and AWS credentials.
        String policy = new String(
            Base64.encodeBase64(policy_document.getBytes("UTF-8")), "ASCII");

        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(
            aws_secret_key.getBytes("UTF-8"), "HmacSHA1"));
        String signature = new String(
            Base64.encodeBase64(hmac.doFinal(policy.getBytes("UTF-8"))), "ASCII");

        // Build an S3 POST HTML document
        String html_document =
            "<html>\n" +
                "<head>\n" +
                "  <title>S3 POST Form</title>\n" +
                "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "</head>\n" +

                "<body>\n" +
                "  <form action=\"https://s3-bucket.s3.amazonaws.com/\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "    <input type=\"hidden\" name=\"key\" value=\"uploads/${filename}\">\n" +
                "    <input type=\"hidden\" name=\"AWSAccessKeyId\" value=\"" + aws_access_key + "\">\n" +
                "    <input type=\"hidden\" name=\"acl\" value=\"private\">\n" +
                "    <input type=\"hidden\" name=\"success_action_redirect\" value=\"http://localhost/\">\n" +
                "    <input type=\"hidden\" name=\"policy\" value=\"" + policy + "\">\n" +
                "    <input type=\"hidden\" name=\"signature\" value=\"" + signature + "\">\n" +
                "    <input type=\"hidden\" name=\"Content-Type\" value=\"image/jpeg\">\n" +


                "    File to upload to S3:\n" +
                "    <input name=\"file\" type=\"file\">\n" +
                "    <br>\n" +
                "    <input type=\"submit\" value=\"Upload File to S3\">\n" +
                "  </form>\n" +
                "</body>\n" +
            "</html>";

        System.out.print(html_document);
    }

}
