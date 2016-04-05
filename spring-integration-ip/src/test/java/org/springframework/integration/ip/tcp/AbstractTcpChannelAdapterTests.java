/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptorFactory;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class AbstractTcpChannelAdapterTests {

	private static final ApplicationEventPublisher NOOP_PUBLISHER = new ApplicationEventPublisher() {

		@Override
		public void publishEvent(ApplicationEvent event) {
		}

		@Override
		public void publishEvent(Object event) {

		}

	};

	protected HelloWorldInterceptorFactory newInterceptorFactory() {
		HelloWorldInterceptorFactory factory = new HelloWorldInterceptorFactory();
		factory.setApplicationEventPublisher(NOOP_PUBLISHER);
		return factory;
	}

	protected void noopPublisher(AbstractConnectionFactory connectionFactory) {
		connectionFactory.setApplicationEventPublisher(NOOP_PUBLISHER);
	}


}
