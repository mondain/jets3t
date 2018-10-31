/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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
package org.jets3t.service.utils.oauth;

/**
 * OAuth constants across providers/services.
 */
public class OAuthConstants {

    /**
     * Constant values applicable to the Google Storage service.
     */
    public static class GSOAuth2_10 {
        /**
         * Redirect URI for native (i.e. non-web based) applications.
         */
        public static final String NATIVE_APPLICATION_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

        public static class GrantTypes {
            public static final String Authorization = "authorization_code";
            public static final String RefreshToken = "refresh_token";
        }

        public static class ResponseTypes {
            public static final String Code = "code";
            public static final String Token = "token";
        }

        public static class Endpoints {
            public static final String Authorization = "https://accounts.google.com/o/oauth2/auth";
            public static final String Token = "https://accounts.google.com/o/oauth2/token";
        }

        public static class Scopes {
            public static final OAuthScope ReadOnly = new OAuthScope("https://www.googleapis.com/auth/devstorage.read_only");
            public static final OAuthScope ReadWrite = new OAuthScope("https://www.googleapis.com/auth/devstorage.read_write");
            public static final OAuthScope FullControl = new OAuthScope("https://www.googleapis.com/auth/devstorage.full_control");
        }
    }

}
