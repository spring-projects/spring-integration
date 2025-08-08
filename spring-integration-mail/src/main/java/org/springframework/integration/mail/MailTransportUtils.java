/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.mail;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Service;
import jakarta.mail.URLName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Collection of utility methods to work with Mail transports.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class MailTransportUtils {

	private static final Log LOGGER = LogFactory.getLog(MailTransportUtils.class);

	/**
	 * Close the given JavaMail Service and ignore any thrown exception. This is useful for typical
	 * <code>finally</code>
	 * blocks in manual JavaMail code.
	 * @param service the JavaMail Service to close (may be <code>null</code>)
	 * @see jakarta.mail.Transport
	 * @see jakarta.mail.Store
	 */
	public static void closeService(Service service) {
		if (service != null) {
			try {
				service.close();
			}
			catch (MessagingException ex) {
				LOGGER.debug("Could not close JavaMail Service", ex);
			}
		}
	}

	/**
	 * Close the given JavaMail Folder and ignore any thrown exception. This is
	 * useful for typical <code>finally</code> blocks in manual JavaMail code.
	 * @param folder  the JavaMail Folder to close (may be <code>null</code>)
	 * @param expunge whether all deleted messages should be expunged from the folder
	 */
	public static void closeFolder(Folder folder, boolean expunge) {
		if (folder != null && folder.isOpen()) {
			try {
				folder.close(expunge);
			}
			catch (MessagingException ex) {
				LOGGER.debug("Could not close JavaMail Folder", ex);
			}
		}
	}

	/**
	 * Returns a string representation of the given {@link URLName}, where the
	 * password has been protected.
	 * @param name The URL name.
	 * @return The result with password protection.
	 */
	public static String toPasswordProtectedString(URLName name) { // NOSONAR
		String protocol = name.getProtocol();
		String username = name.getUsername();
		String password = name.getPassword();
		String host = name.getHost();
		int port = name.getPort();
		String file = name.getFile();
		String ref = name.getRef();
		StringBuilder tempURL = new StringBuilder();
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
				tempURL.append(':').append(port);
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
