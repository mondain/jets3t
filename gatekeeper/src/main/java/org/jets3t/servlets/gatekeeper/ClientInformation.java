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
package org.jets3t.servlets.gatekeeper;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Stores information about the HTTP client that submitted a request to the Gatekeeper.
 * <p>
 * The information available about a client will depend on the server and client configuration,
 * such as whether the client is identified with an existing HttpSession or Principal. It must
 * be assumed that much of the information stored in this class will have a null value in many
 * cases.
 * <p>
 * All information in this class is sourced from equivalent methods in
 * {@link javax.servlet.http.HttpServletRequest}.
 *
 * @author James Murty
 */
public class ClientInformation {
    private String remoteAddress = null;
    private String remoteHost = null;
    private String remoteUser = null;
    private int remotePort = -1;
    private HttpSession session = null;
    private Principal userPrincipal = null;
    private String userAgent = null;
    private HttpServletRequest httpServletRequest = null;

    public ClientInformation(String remoteAddress, String remoteHost, String remoteUser,
        int remotePort, HttpSession session, Principal userPrincipal, String userAgent,
        HttpServletRequest httpServletRequest)
    {
        this.remoteAddress = remoteAddress;
        this.remoteHost = remoteHost;
        this.remoteUser = remoteUser;
        this.remotePort = remotePort;
        this.session = session;
        this.userPrincipal = userPrincipal;
        this.userAgent = userAgent;
        this.httpServletRequest = httpServletRequest;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public HttpSession getSession() {
        return session;
    }

    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return
     * the original servlet request, in case the specific information captured in this
     * class is not sufficient.
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

}
