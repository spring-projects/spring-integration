/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.expression.ExpressionException;
import org.springframework.integration.context.OrderlyShutdownCapable;
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
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractWebServiceInboundGateway extends MessagingGatewaySupport
		implements MessageEndpoint, OrderlyShutdownCapable {

	private final AtomicInteger activeCount = new AtomicInteger();

	private SoapHeaderMapper headerMapper = new DefaultSoapHeaderMapper();

	@Override
	public String getComponentType() {
		return "ws:inbound-gateway";
	}

	public void setHeaderMapper(SoapHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	protected SoapHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	@Override
	public void invoke(MessageContext messageContext) throws Exception { // NOSONAR - external interface
		if (!isRunning()) {
			throw new ServiceUnavailableException("503 Service Unavailable");
		}
		Assert.notNull(messageContext, "'messageContext' is required; it must not be null.");

		try {
			this.activeCount.incrementAndGet();
			this.doInvoke(messageContext);
		}
		catch (Exception e) {
			while ((e instanceof MessagingException || e instanceof ExpressionException) && // NOSONAR
					e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw e;
		}
		finally {
			this.activeCount.decrementAndGet();
		}
	}

	protected void fromSoapHeaders(MessageContext messageContext, AbstractIntegrationMessageBuilder<?> builder) {
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

	protected void toSoapHeaders(WebServiceMessage response, Message<?> replyMessage) {
		if (response instanceof SoapMessage) {
			this.headerMapper.fromHeadersToReply(
					replyMessage.getHeaders(), (SoapMessage) response);
		}
	}

	@Override
	public int beforeShutdown() {
		stop();
		return this.activeCount.get();
	}

	@Override
	public int afterShutdown() {
		return this.activeCount.get();
	}

	protected abstract void doInvoke(MessageContext messageContext) throws IOException;

}
