/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.transformer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import jakarta.mail.Multipart;
import jakarta.mail.Part;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.util.Assert;

/**
 * Transforms a Message payload of type {@link jakarta.mail.Message} to a String. If the
 * mail message's content is a String, it will be the payload of the result Message. If
 * the content is a Part or Multipart, a String will be created from an output stream of
 * bytes using the provided charset (or UTF-8 by default).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MailToStringTransformer extends AbstractMailMessageTransformer<String> {

	private String charset = "UTF-8";

	/**
	 * Specify the name of the Charset to use when converting from bytes.
	 * The default is UTF-8.
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), () -> "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	@Override
	protected AbstractIntegrationMessageBuilder<String> doTransform(jakarta.mail.Message mailMessage) {
		try {
			String payload;
			Object content = mailMessage.getContent();
			if (content instanceof String value) {
				payload = value;
			}
			else if (content instanceof Multipart multipart) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				multipart.writeTo(outputStream);
				payload = outputStream.toString(this.charset);
			}
			else if (content instanceof Part part) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				part.writeTo(outputStream);
				payload = outputStream.toString(this.charset);
			}
			else {
				throw new IllegalArgumentException("failed to transform contentType ["
						+ mailMessage.getContentType() + "] to String.");
			}

			return getMessageBuilderFactory().withPayload(payload);
		}
		catch (Exception ex) {
			throw new MessageTransformationException("Cannot transform mail message", ex);
		}

	}

}
