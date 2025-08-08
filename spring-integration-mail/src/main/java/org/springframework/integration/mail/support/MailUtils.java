/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.mail.support;

import java.util.HashMap;
import java.util.Map;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.StringUtils;

/**
 * Utilities for handling mail messages.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public final class MailUtils {

	private MailUtils() {
		// empty
	}

	/**
	 * Map the message headers to a Map using {@link MailHeaders} keys; specifically
	 * maps the address headers and the subject.
	 * @param source the message.
	 * @return the map.
	 */
	public static Map<String, Object> extractStandardHeaders(Message source) {
		Map<String, Object> headers = new HashMap<>();
		try {
			headers.put(MailHeaders.FROM, StringUtils.arrayToCommaDelimitedString(source.getFrom()));
			headers.put(MailHeaders.BCC, convertToStringArray(source.getRecipients(RecipientType.BCC)));
			headers.put(MailHeaders.CC, convertToStringArray(source.getRecipients(RecipientType.CC)));
			headers.put(MailHeaders.TO, convertToStringArray(source.getRecipients(RecipientType.TO)));
			headers.put(MailHeaders.REPLY_TO, StringUtils.arrayToCommaDelimitedString(source.getReplyTo()));
			headers.put(MailHeaders.SUBJECT, source.getSubject());
			return headers;
		}
		catch (Exception ex) {
			throw new MessagingException("conversion of MailMessage headers failed", ex);
		}
	}

	private static String[] convertToStringArray(Address[] addresses) {
		if (addresses != null) {
			String[] addressStrings = new String[addresses.length];
			for (int i = 0; i < addresses.length; i++) {
				addressStrings[i] = addresses[i].toString();
			}
			return addressStrings;
		}
		return new String[0];
	}

}
