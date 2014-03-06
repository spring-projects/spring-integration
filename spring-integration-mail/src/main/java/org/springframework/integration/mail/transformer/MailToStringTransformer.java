/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.mail.transformer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import javax.mail.Multipart;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;

/**
 * Transforms a Message payload of type {@link javax.mail.Message} to a String.
 * If the mail message's content is a String, it will be the payload of the
 * result Message. If the content is a Multipart, a String will be created from
 * an output stream of bytes using the provided charset (or UTF-8 by default).
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MailToStringTransformer extends AbstractMailMessageTransformer<String> {

	private volatile String charset = "UTF-8";


	/**
	 * Specify the name of the Charset to use when converting from bytes.
	 * The default is UTF-8.
	 *
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	@Override
	protected AbstractIntegrationMessageBuilder<String> doTransform(javax.mail.Message mailMessage) throws Exception {
		Object content = mailMessage.getContent();
		if (content instanceof String) {
			return this.getMessageBuilderFactory().withPayload((String) content);
		}
		if (content instanceof Multipart) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			((Multipart) content).writeTo(outputStream);
			return this.getMessageBuilderFactory().withPayload(
					new String(outputStream.toByteArray(), this.charset));
		}
		throw new IllegalArgumentException("failed to transform contentType ["
				+ mailMessage.getContentType() +"] to String.");
	}

}
