/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.rmi;

import java.io.Serializable;

import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * An outbound Messaging Gateway for RMI-based remoting.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RmiOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final RequestReplyExchanger proxy;

	private final RmiProxyFactoryBeanConfigurer configurer;

	/**
	 * Construct an instance with a `RequestReplyExchanger` built from the
	 * default {@link RmiProxyFactoryBean}.
	 * @param url the url.
	 */
	public RmiOutboundGateway(String url) {
		this(url, null);
	}

	/**
	 * Construct an instance with a `RequestReplyExchanger` built from the
	 * default {@link RmiProxyFactoryBean} which can be modified by the
	 * configurer.
	 * @param url the url.
	 * @param configurer the {@link RmiProxyFactoryBeanConfigurer}.
	 * @since 4.3.2
	 */
	public RmiOutboundGateway(String url, RmiProxyFactoryBeanConfigurer configurer) {
		this.configurer = configurer;
		this.proxy = createProxy(url);
	}


	public void setReplyChannel(MessageChannel replyChannel) {
		setOutputChannel(replyChannel);
	}

	@Override
	public String getComponentType() {
		return "rmi:outbound-gateway";
	}

	@Override
	public final Object handleRequestMessage(Message<?> message) {
		if (!(message.getPayload() instanceof Serializable)) {
			throw new MessageHandlingException(message,
					this.getComponentName() + " expects a Serializable payload type " +
					"but encountered [" + message.getPayload().getClass().getName() + "]");
		}
		Message<?> requestMessage = this.getMessageBuilderFactory().withPayload(message.getPayload())
				.copyHeaders(message.getHeaders()).build();
		try {
			Message<?> reply = this.proxy.exchange(requestMessage);
			if (reply != null) {
				reply = getMessageBuilderFactory()
						.fromMessage(reply)
						.copyHeadersIfAbsent(message.getHeaders())
						.build();
			}
			return reply;
		}
		catch (MessagingException e) {
			throw new MessageHandlingException(message, e);
		}
		catch (RemoteAccessException e) {
			throw new MessageHandlingException(message, "Remote failure in RmiOutboundGateway: " +
					this.getComponentName(), e);
		}
	}

	private RequestReplyExchanger createProxy(String url) {
		RmiProxyFactoryBean proxyFactory = new RmiProxyFactoryBean();
		proxyFactory.setServiceInterface(RequestReplyExchanger.class);
		proxyFactory.setServiceUrl(url);
		proxyFactory.setLookupStubOnStartup(false);
		proxyFactory.setRefreshStubOnConnectFailure(true);
		if (this.configurer != null) {
			this.configurer.configure(proxyFactory);
		}
		proxyFactory.afterPropertiesSet();
		return (RequestReplyExchanger) proxyFactory.getObject();
	}

	/**
	 * Allows configuration of the proxy factory bean before the RMI proxy is created.
	 * @since 4.3.2
	 */
	public interface RmiProxyFactoryBeanConfigurer {

		/**
		 * Perform additional configuration of the factory bean before the
		 * {@code RequestReplyExchanger} is created.
		 * @param factoryBean the factory bean.
		 */
		void configure(RmiProxyFactoryBean factoryBean);

	}

}
