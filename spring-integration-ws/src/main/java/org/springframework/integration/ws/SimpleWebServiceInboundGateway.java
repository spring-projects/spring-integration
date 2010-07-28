/*
 * Copyright 2002-2010 the original author or authors.
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
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.expression.ExpressionException;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.MessageEndpoint;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
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
		try {
			this.doInvoke(messageContext);
		}
		catch (Exception e) {
			while ((e instanceof MessagingException || e instanceof ExpressionException) &&
					e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw e;
		}
	}

	private void doInvoke(MessageContext messageContext) throws Exception {
		Assert.notNull(messageContext,"'messageContext' is required; it must not be null.");
		WebServiceMessage request = messageContext.getRequest();
		Assert.notNull(request, "Invalid message context: request was null.");
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
			SoapHeader soapHeader = soapMessage.getSoapHeader();
			if (soapHeader != null) {
				Iterator<?> attributeIter = soapHeader.getAllAttributes();
				while (attributeIter.hasNext()) {
					QName name = (QName) attributeIter.next();
					builder.setHeader(name.toString(), soapHeader.getAttributeValue(name));
				}
				Iterator<?> elementIter = soapHeader.examineAllHeaderElements();
				while (elementIter.hasNext()) {
					Object element = elementIter.next();
					if (element instanceof SoapHeaderElement) {
						QName name = ((SoapHeaderElement) element).getName();
						builder.setHeader(name.toString(), element);
					}
				}
			}
		}
		Message<?> replyMessage = this.sendAndReceiveMessage(builder.build());
		if (replyMessage != null && replyMessage.getPayload() != null) {
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
		}
	}

	private class TransformerSupportDelegate extends TransformerObjectSupport {
		void transformSourceToResult(Source source, Result result) throws TransformerException {
			this.transform(source, result);
		}
	}

}
