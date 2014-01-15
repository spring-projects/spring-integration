/*
 * Copyright 2007-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Service;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Collection of utility methods to work with Mail transports.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public abstract class MailTransportUtils {

    private static final Log logger = LogFactory.getLog(MailTransportUtils.class);


    /**
     * Close the given JavaMail Service and ignore any thrown exception. This is useful for typical <code>finally</code>
     * blocks in manual JavaMail code.
     *
     * @param service the JavaMail Service to close (may be <code>null</code>)
     * @see Transport
     * @see Store
     */
    public static void closeService(Service service) {
        if (service != null) {
            try {
                service.close();
            }
            catch (MessagingException ex) {
                logger.debug("Could not close JavaMail Service", ex);
            }
        }
    }

//    /**
//     * Close the given JavaMail Folder and ignore any thrown exception. This is
//     * useful for typical <code>finally</code> blocks in manual JavaMail code.
//     *
//     * @param folder the JavaMail Folder to close (may be <code>null</code>)
//     */
//
//    public static void closeFolder(Folder folder) {
//        closeFolder(folder, false);
//    }

    /**
     * Close the given JavaMail Folder and ignore any thrown exception. This is
     * useful for typical <code>finally</code> blocks in manual JavaMail code.
     *
     * @param folder  the JavaMail Folder to close (may be <code>null</code>)
     * @param expunge whether all deleted messages should be expunged from the folder
     */
    public static void closeFolder(Folder folder, boolean expunge) {
        if (folder != null && folder.isOpen()) {
            try {
            	folder.close(expunge);
            }
            catch (MessagingException ex) {
                logger.debug("Could not close JavaMail Folder", ex);
            }
            catch (NullPointerException ex) {
                // JavaMail prior to 1.4.1 may throw this
                logger.debug("Could not close JavaMail Folder", ex);
            }
        }
    }

    /**
     * Returns a string representation of the given {@link URLName}, where the
     * password has been protected.
     *
     * @param name The URL name.
     * @return The result with password protection.
     */
    public static String toPasswordProtectedString(URLName name) {
        String protocol = name.getProtocol();
        String username = name.getUsername();
        String password = name.getPassword();
        String host = name.getHost();
        int port = name.getPort();
        String file = name.getFile();
        String ref = name.getRef();
        StringBuffer tempURL = new StringBuffer();
        if (protocol != null) {
            tempURL.append(protocol).append(':');
        }
        if (StringUtils.hasLength(username) || StringUtils.hasLength(host)) {
            tempURL.append("//");
            if (StringUtils.hasLength(username)) {
                tempURL.append(username);
                if (StringUtils.hasLength(password)) {
                    tempURL.append(":*****");
                }
                tempURL.append("@");
            }
            if (StringUtils.hasLength(host)) {
                tempURL.append(host);
            }
            if (port != -1) {
                tempURL.append(':').append(Integer.toString(port));
            }
            if (StringUtils.hasLength(file)) {
                tempURL.append('/');
            }
        }
        if (StringUtils.hasLength(file)) {
            tempURL.append(file);
        }
        if (StringUtils.hasLength(ref)) {
            tempURL.append('#').append(ref);
        }
        return tempURL.toString();
    }

}
