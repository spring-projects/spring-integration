/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.mail.support;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Utilities for handling mail messages.
 *
 * @author Gary Russell
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
		Map<String, Object> headers = new HashMap<String, Object>();
		try {
			headers.put(MailHeaders.FROM, convertToString(source.getFrom()));
			headers.put(MailHeaders.BCC, convertToStringArray(source.getRecipients(RecipientType.BCC)));
			headers.put(MailHeaders.CC, convertToStringArray(source.getRecipients(RecipientType.CC)));
			headers.put(MailHeaders.TO, convertToStringArray(source.getRecipients(RecipientType.TO)));
			headers.put(MailHeaders.REPLY_TO, convertToString(source.getReplyTo()));
			headers.put(MailHeaders.SUBJECT, source.getSubject());
			return headers;
		}
		catch (Exception e) {
			throw new MessagingException("conversion of MailMessage headers failed", e);
		}
	}

	private static String convertToString(Address[] addresses) {
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		Assert.state(addresses.length == 1, "expected a single value but received an Array");
		return addresses[0].toString();
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
