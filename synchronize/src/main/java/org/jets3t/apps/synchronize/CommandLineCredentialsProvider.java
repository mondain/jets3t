/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;


/**
 * Prompts for the user to enter HTTP Proxy authentication credentials via the
 * command line.
 *
 * @author James Murty
 */
public class CommandLineCredentialsProvider implements CredentialsProvider {

    private final CredentialsProvider mCredentialProvider;



    public CommandLineCredentialsProvider(){
        mCredentialProvider = new BasicCredentialsProvider();
    }

    public void setCredentials(AuthScope authscope, Credentials credentials){
        mCredentialProvider.setCredentials(authscope, credentials);
    }

    /**
     * Clear credentials.
     */
    public void clear(){
        mCredentialProvider.clear();
    }


    /**
     * Implementation method for the CredentialsProvider interface.
     * <p>
     * Based on sample code:
     * <a href="http://svn.apache.org/viewvc/jakarta/commons/proper/httpclient/trunk/src/examples/InteractiveAuthenticationExample.java?view=markup">InteractiveAuthenticationExample</a>
     *
     */
    //public Credentials getCredentials(AuthScheme authscheme, String host, int port, boolean proxy) throws CredentialsNotAvailableException {
    public Credentials getCredentials(AuthScope scope){
        if (scope == null || scope.getScheme() == null) {
            return null;
        }
        Credentials credentials = mCredentialProvider.getCredentials(scope);
        if (credentials!=null){
            return credentials;
        }
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

            // authscheme instanceof NTLMScheme
            if (scope.getScheme().equals("ntlm")) {
                System.out.println("Proxy Authentication Required -- " +
                    "Host " + scope.getHost() + ":" + scope.getPort() + " requires Windows authentication");
                System.out.print("Username: ");
                String username = inputReader.readLine();
                System.out.print("Password: ");
                String password = inputReader.readLine();
                System.out.print("Domain: ");
                String domain = inputReader.readLine();
                credentials = new NTCredentials(username, password, scope.getHost(), domain);
            } else if (scope.getScheme().equals("basic")
                    || scope.getScheme().equals("digest")) {
                //if (authscheme instanceof RFC2617Scheme) {
                System.out.println("Proxy Authentication Required -- " +
                    "Host " + scope.getHost() + ":" + scope.getPort() + " requires authentication for the realm: " + scope.getRealm());
                System.out.print("Username: ");
                String username = inputReader.readLine();
                System.out.print("Password: ");
                String password = inputReader.readLine();

                credentials = new UsernamePasswordCredentials(username, password);
            } else {
                throw new IllegalArgumentException("Unsupported authentication scheme: " +
                    scope.getScheme());
            }
            if (credentials != null){
                mCredentialProvider.setCredentials(scope, credentials);
            }
            return credentials;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
