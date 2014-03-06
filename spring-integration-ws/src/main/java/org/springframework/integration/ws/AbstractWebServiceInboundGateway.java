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

import java.util.Map;

import org.springframework.expression.ExpressionException;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.MessageEndpoint;
import org.springframework.ws.soap.SoapMessage;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 */
abstract public class AbstractWebServiceInboundGateway extends MessagingGatewaySupport implements MessageEndpoint {

	protected volatile SoapHeaderMapper headerMapper = new DefaultSoapHeaderMapper();

	@Override
	public String getComponentType() {
		return "ws:inbound-gateway";
	}

	public void setHeaderMapper(SoapHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	public void invoke(MessageContext messageContext) throws Exception {
		Assert.notNull(messageContext,"'messageContext' is required; it must not be null.");

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

	protected void fromSoapHeaders(MessageContext messageContext, AbstractIntegrationMessageBuilder<?> builder){
		WebServiceMessage request = messageContext.getRequest();
		String[] propertyNames = messageContext.getPropertyNames();
		if (propertyNames != null) {
			for (String propertyName : propertyNames) {
				builder.setHeader(propertyName, messageContext.getProperty(propertyName));
			}
		}
		if (request instanceof SoapMessage) {
			SoapMessage soapMessage = (SoapMessage) request;
			Map<String, ?> headers = this.headerMapper.toHeadersFromRequest(soapMessage);
			if (!CollectionUtils.isEmpty(headers)) {
				builder.copyHeaders(headers);
			}
		}
	}

	protected void toSoapHeaders(WebServiceMessage response, Message<?> replyMessage){
		if (response instanceof SoapMessage) {
			this.headerMapper.fromHeadersToReply(
					replyMessage.getHeaders(), (SoapMessage) response);
		}
	}

	abstract protected void doInvoke(MessageContext messageContext) throws Exception;
}
