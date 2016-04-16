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

package org.springframework.integration.mail.transformer;

import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.BeanUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;

/**
 * Transform a MimeMessage to a {@link DetatchedMimeMessage} which has no references
 * to Session, Store etc and may be serialized by, for example, Kryo.
 *
 * @author Gary Russell
 * @since 4.2.6
 *
 */
public class MimeToDetatchedTransformer extends AbstractMailMessageTransformer<DetatchedMimeMessage> {

	@Override
	protected AbstractIntegrationMessageBuilder<DetatchedMimeMessage> doTransform(javax.mail.Message mailMessage)
			throws Exception {
		Assert.isInstanceOf(MimeMessage.class, mailMessage, "Payload must be a MimeMessage, was:"
				+ mailMessage.getClass());
		MimeMessage mime = (MimeMessage) mailMessage;
		DetatchedMimeMessage detatched = DetatchedMimeMessage.newInstance();
		BeanUtils.copyProperties(mime, detatched);
		@SuppressWarnings("rawtypes")
		Enumeration allHeaders = mime.getAllHeaders();
		while (allHeaders.hasMoreElements()) {
			Header header = (Header) allHeaders.nextElement();
			detatched.addHeader(header.getName(), header.getValue());
		}
		detatched.setContent(mime.getContent(), mime.getContentType());
		return getMessageBuilderFactory().withPayload(detatched);
	}

}
