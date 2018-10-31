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
package org.jets3t.service.security;

import java.util.Date;

/**
 * Represent OAuth 2.0 access and refresh tokens, and an optional expiry date
 * based on the expiry timeout an OAuth end-point may return.
 *
 * @author jmurty
 */
public class OAuth2Tokens {
    protected final String accessToken;
    protected final String refreshToken;
    protected Date expiry;

    /**
     * Store token data including the expiry date of the access token.
     *
     * @param accessToken
     * @param refreshToken
     * @param expiry
     */
    public OAuth2Tokens(String accessToken, String refreshToken, Date expiry) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiry = expiry;

        if (refreshToken == null) {
            throw new IllegalArgumentException(
                "Null refresh tokens not permitted when constructing "
                + this.getClass().getName());
        }
        if (accessToken == null) {
            this.expireAccessToken();
        }
    }

    /**
     * Store token data without the expiry date of the access token.
     *
     * @param accessToken
     * @param refreshToken
     */
    public OAuth2Tokens(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public Date getExpiry() {
        return this.expiry;
    }

    /**
     * Forcibly expire the access token by setting the expiry
     * timestamp to the epoch.
     */
    public void expireAccessToken() {
        this.expiry = new Date(0);
    }

    /**
     * @return
     * true if the access token as expired according to the expiry date
     * provided when this class was created, false otherwise. Note that
     * this method will always return true if no expiry date was provided
     * (i.e. {@link #getExpiry()} is null) since the expiry time is unknown.
     */
    public boolean isAccessTokenExpired() {
        if (getExpiry() != null) {
            return getExpiry().before(new Date());
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName()
            + " [accessToken=" + getAccessToken()
            + ", refreshToken=" + getRefreshToken()
            + ", expiry=" + getExpiry()
            + ", isExpired? " + isAccessTokenExpired() + "]";
    }

    /**
     * Calculate a date timestamp a given number of seconds in the future.
     * This is convenient for calculating the expiry time for OAuth access
     * tokens when you are only given the "expires_in" timeout value by
     * the OAuth service.
     *
     * @param expiresInSeconds
     * how many seconds in the future the result should be. May be null,
     * in which case the current date/time is returned.
     *
     * @return
     * a Date at the current time, or a given number of seconds in the future.
     */
    public static Date calculateExpiry(Number expiresInSeconds) {
        long expiresInMsec = 0;
        if (expiresInSeconds != null) {
            expiresInMsec = expiresInSeconds.longValue() * 1000;
        }
        return new Date(System.currentTimeMillis() + expiresInMsec);
    }

}
