/*
 * Copyright 2002-2011 the original author or authors.
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
 * @since 1.0.2
 */
public class SimpleWebServiceInboundGateway extends AbstractWebServiceInboundGateway {

	private final TransformerSupportDelegate transformerSupportDelegate = new TransformerSupportDelegate();

	private volatile boolean extractPayload = true;

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void doInvoke(MessageContext messageContext) throws Exception {

		WebServiceMessage request = messageContext.getRequest();
		Assert.notNull(request, "Invalid message context: request was null.");

		AbstractIntegrationMessageBuilder<?> builder = this.getMessageBuilderFactory().withPayload(
				(this.extractPayload) ? request.getPayloadSource() : request);

		this.fromSoapHeaders(messageContext, builder);

		Message<?> replyMessage = this.sendAndReceiveMessage(builder.build());

		if (replyMessage != null) {
			Object replyPayload = replyMessage.getPayload();
			Source responseSource = null;
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
						+ "], or [java.lang.String]. The actual type was ["
						+ replyPayload.getClass().getName() + "]");
			}
			WebServiceMessage response = messageContext.getResponse();
			this.transformerSupportDelegate.transformSourceToResult(responseSource, response.getPayloadResult());

			this.toSoapHeaders(response, replyMessage);

		}
	}


	private static class TransformerSupportDelegate extends TransformerObjectSupport {
		void transformSourceToResult(Source source, Result result) throws TransformerException {
			this.transform(source, result);
		}
	}

}
