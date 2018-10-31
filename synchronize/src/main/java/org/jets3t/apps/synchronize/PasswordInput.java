/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 Bennett Hiles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.apps.synchronize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A utility class that gives the ability to check which java version
 * is being used to determine whether the java.io.Console class can be
 * utitlized to retrieve a password from a user's input.
 *
 * @author Bennett Hiles
 */
public class PasswordInput {

    private static final Log LOG = LogFactory.getLog(PasswordInput.class);

    public static String getPassword(String prompt) throws IOException {

        String password = null;
        String javaVers = System.getProperty("java.runtime.version");

        if (LOG.isDebugEnabled()) {
            LOG.debug("About to attempt to read password under Java " +
                      "version: "+javaVers);
        }

        // Split version string into components. String is of the form
        // MAJOR_VERSION_NUMBER . MINOR_VERSION_NUMBR . REVISION_STRING
        // e.g. 1.6.0_17-b04-248-10M3025
        String[] versionMajAndMin = javaVers.split("\\.");
        int majorVersion = Integer.parseInt(versionMajAndMin[0]);
        int minorVersion = Integer.parseInt(versionMajAndMin[1]);

        if (majorVersion > 1 || minorVersion >= 6) {
            password = getPasswordJDK16OrLater(prompt);
        } else {
            password = getPasswordBeforeJDK16(prompt);
        }

        return password;
    }

    private static String getPasswordBeforeJDK16(String prompt) throws IOException {
        System.out.print(prompt + ": ");
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        String password = inputReader.readLine();
        return password;
    }

    private static String getPasswordJDK16OrLater(String prompt) throws IOException {
        String password = null;
        try {
            Class consoleClass = Class.forName("java.io.Console");
            Method consoleReadPassword = consoleClass.getDeclaredMethod("readPassword", new Class[] {});

            Method consoleMethod = System.class.getDeclaredMethod("console", new Class[] {});;
            Object consoleObject = consoleMethod.invoke(null, new Object[] {});

            if (consoleObject == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("java.io.Console using reflection returned null, " +
                            "so that password will be fetched using the pre-JDK 1.6 version");
                }
                password = getPasswordBeforeJDK16(prompt);
            } else {
                System.out.println(prompt + " (typing will be hidden):");
                Object passwordObj = consoleReadPassword.invoke(consoleObject, new Object[] {});
                char[] passwordChars = (char[]) passwordObj;
                password = String.valueOf(passwordChars);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Encountered trouble using relection to generate java.io.Console class");
        }

        return password;
    }

}
