/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail.support;

import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import jakarta.mail.Header;
import jakarta.mail.internet.MimeMessage;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Maps an inbound {@link MimeMessage} to a {@link Map}.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class DefaultMailHeaderMapper implements HeaderMapper<MimeMessage> {

	@Override
	public void fromHeaders(MessageHeaders headers, MimeMessage target) {
		throw new UnsupportedOperationException("Mapping to a mail message is not currently supported");
	}

	@Override
	public Map<String, Object> toHeaders(MimeMessage source) {
		Map<String, Object> headers = MailUtils.extractStandardHeaders(source);
		try {
			Enumeration<?> allHeaders = source.getAllHeaders();
			MultiValueMap<String, String> rawHeaders = new LinkedMultiValueMap<String, String>();
			while (allHeaders.hasMoreElements()) {
				Object headerInstance = allHeaders.nextElement();
				if (headerInstance instanceof Header) {
					Header header = (Header) headerInstance;
					rawHeaders.add(header.getName(), header.getValue());
				}
			}
			headers.put(MailHeaders.RAW_HEADERS, rawHeaders);
			headers.put(MailHeaders.FLAGS, source.getFlags());
			int lineCount = source.getLineCount();
			if (lineCount > 0) {
				headers.put(MailHeaders.LINE_COUNT, lineCount);
			}
			Date receivedDate = source.getReceivedDate();
			if (receivedDate != null) {
				headers.put(MailHeaders.RECEIVED_DATE, receivedDate);
			}
			int size = source.getSize();
			if (size > 0) {
				headers.put(MailHeaders.SIZE, size);
			}
			headers.put(MailHeaders.EXPUNGED, source.isExpunged());
			headers.put(MailHeaders.CONTENT_TYPE, source.getContentType());
		}
		catch (Exception e) {
			throw new MessagingException("Failed to map message headers", e);
		}
		return headers;
	}

}
