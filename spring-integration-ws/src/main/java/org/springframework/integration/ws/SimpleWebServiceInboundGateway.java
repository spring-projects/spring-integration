/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ws;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 1.0.2
 */
public class SimpleWebServiceInboundGateway extends AbstractWebServiceInboundGateway {

	private final TransformerSupportDelegate transformerSupportDelegate = new TransformerSupportDelegate();

	private boolean extractPayload = true;

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void doInvoke(MessageContext messageContext) {
		WebServiceMessage request = messageContext.getRequest();
		Assert.notNull(request, "Invalid message context: request was null.");

		AbstractIntegrationMessageBuilder<?> builder = getMessageBuilderFactory().withPayload(
				(this.extractPayload) ? request.getPayloadSource() : request);

		fromSoapHeaders(messageContext, builder);

		Message<?> replyMessage = sendAndReceiveMessage(builder.build());

		if (replyMessage != null) {
			Object replyPayload = replyMessage.getPayload();
			Source responseSource = null;

			if (replyPayload instanceof WebServiceMessage) {
				messageContext.setResponse((WebServiceMessage) replyPayload);
			}
			else {
				if (replyPayload instanceof Source) {
					responseSource = (Source) replyPayload;
				}
				else if (replyPayload instanceof Document) {
					responseSource = new DOMSource((Document) replyPayload);
				}
				else if (replyPayload instanceof String) {
					responseSource = new StringSource((String) replyPayload);
				}
				else {
					throw new IllegalArgumentException("The reply Message payload must be a ["
							+ Source.class.getName() + "], [" + Document.class.getName()
							+ "], [java.lang.String] or [" + WebServiceMessage.class.getName() + "]. " +
							"The actual type was [" + replyPayload.getClass().getName() + "]");
				}
				WebServiceMessage response = messageContext.getResponse();
				try {
					this.transformerSupportDelegate.transformSourceToResult(responseSource, response.getPayloadResult());
				}
				catch (TransformerException e) {
					throw new IllegalStateException(e);
				}

				toSoapHeaders(response, replyMessage);
			}
		}
	}

	private static class TransformerSupportDelegate extends TransformerObjectSupport {

		TransformerSupportDelegate() {
		}

		void transformSourceToResult(Source source, Result result) throws TransformerException {
			transform(source, result);
		}

	}

}
