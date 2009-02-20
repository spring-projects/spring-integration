/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.MessageEndpoint;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * @author Mark Fisher
 * @since 1.0.2
 */
public class SimpleWebServiceInboundGateway extends SimpleMessagingGateway implements MessageEndpoint {

	private final TransformerSupportDelegate transformerSupportDelegate = new TransformerSupportDelegate();

	private volatile boolean extractPayload = true;


	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void invoke(MessageContext messageContext) throws Exception {
		WebServiceMessage request = messageContext.getRequest();
		MessageBuilder<?> builder = MessageBuilder.withPayload(
				(this.extractPayload) ? request.getPayloadSource() : request);
		String[] propertyNames = messageContext.getPropertyNames();
		if (propertyNames != null) {
			for (String propertyName : propertyNames) {
				builder.setHeader(propertyName, messageContext.getProperty(propertyName));
			}
		}
		if (request instanceof SoapMessage) {
			SoapMessage soapMessage = (SoapMessage) request;
			Iterator<?> iter = soapMessage.getSoapHeader().getAllAttributes();
			while (iter.hasNext()) {
				QName name = (QName) iter.next();
				builder.setHeader(name.toString(), soapMessage.getSoapHeader().getAttributeValue(name));
			}
		}
		Message<?> replyMessage = this.sendAndReceiveMessage(builder.build());
		if (replyMessage != null && replyMessage.getPayload() != null) {
			Object replyPayload = replyMessage.getPayload();
			Source responseSource = null;
			if (replyPayload instanceof Source) {
				responseSource = (Source) replyPayload;
			}
			else if (replyPayload instanceof String) {
				responseSource = new StringSource((String) replyPayload);
			}
			else {
				throw new IllegalArgumentException("The reply Message payload must be a ["
						+ Source.class.getName() + "] or [java.lang.String]");
			}
			WebServiceMessage response = messageContext.getResponse();
			this.transformerSupportDelegate.transformSourceToResult(responseSource, response.getPayloadResult());
		}
	}

	private class TransformerSupportDelegate extends TransformerObjectSupport {
		void transformSourceToResult(Source source, Result result) throws TransformerException {
			this.transform(source, result);
		}
	}

}
