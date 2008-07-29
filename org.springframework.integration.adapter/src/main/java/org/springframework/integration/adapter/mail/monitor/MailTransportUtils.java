/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.integration.adapter.mail.monitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Service;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;


/**
 * Collection of utility methods to work with Mail transports.
 *
 * @author Arjen Poutsma
 */
public abstract class MailTransportUtils {

    private static final Pattern TO_PATTERN = Pattern.compile("^([^\\?]+)");

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("subject=([^\\&]+)");

    private static final Log logger = LogFactory.getLog(MailTransportUtils.class);

    private MailTransportUtils() {
    }

    public static InternetAddress getTo(URI uri) {
        Matcher matcher = TO_PATTERN.matcher(uri.getSchemeSpecificPart());
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null) {
                    try {
                        return new InternetAddress(group);
                    }
                    catch (AddressException e) {
                        // try next group
                    }
                }
            }
        }
        return null;
    }

    public static String getSubject(URI uri) {
        Matcher matcher = SUBJECT_PATTERN.matcher(uri.getSchemeSpecificPart());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

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

    /**
     * Close the given JavaMail Folder and ignore any thrown exception. This is useful for typical <code>finally</code>
     * blocks in manual JavaMail code.
     *
     * @param folder the JavaMail Folder to close (may be <code>null</code>)
     */

    public static void closeFolder(Folder folder) {
        closeFolder(folder, false);
    }

    /**
     * Close the given JavaMail Folder and ignore any thrown exception. This is useful for typical <code>finally</code>
     * blocks in manual JavaMail code.
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
        }
    }

    /** Returns a string representation of the given {@link URLName}, where the password has been protected. */
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

    /**
     * Converts the given internet address into a <code>mailto</code> URI.
     *
     * @param to      the To: address
     * @param subject the subject, may be <code>null</code>
     * @return a mailto URI
     */
    public static URI toUri(InternetAddress to, String subject) throws URISyntaxException {
        if (StringUtils.hasLength(subject)) {
            return new URI(MailTransportConstants.MAIL_URI_SCHEME, to.getAddress() + "?subject=" + subject, null);
        }
        else {
            return new URI(MailTransportConstants.MAIL_URI_SCHEME, to.getAddress(), null);
        }
    }


}
