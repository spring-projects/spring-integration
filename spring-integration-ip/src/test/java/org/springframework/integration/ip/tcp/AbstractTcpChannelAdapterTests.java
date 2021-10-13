/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptorFactory;

/**
 * @author Gary Russell
 * @author Mário Dias
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class AbstractTcpChannelAdapterTests {

	private static final ApplicationEventPublisher NOOP_PUBLISHER = event -> { };

	protected HelloWorldInterceptorFactory newInterceptorFactory() {
		return newInterceptorFactory(NOOP_PUBLISHER);
	}

	protected HelloWorldInterceptorFactory newInterceptorFactory(ApplicationEventPublisher applicationEventPublisher) {
		HelloWorldInterceptorFactory factory = new HelloWorldInterceptorFactory();
		factory.setApplicationEventPublisher(applicationEventPublisher);
		return factory;
	}

	protected void noopPublisher(AbstractConnectionFactory connectionFactory) {
		connectionFactory.setApplicationEventPublisher(NOOP_PUBLISHER);
	}

}
