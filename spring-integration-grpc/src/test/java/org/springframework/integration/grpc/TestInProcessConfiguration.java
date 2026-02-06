/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.grpc;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * @author Artem Bilan
 *
 * @since 7.1
 */
@Configuration(proxyBeanMethods = false)
public class TestInProcessConfiguration implements DisposableBean {

	final String serverName = InProcessServerBuilder.generateName();

	final InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(this.serverName);

	volatile Server server;

	@Bean
	ManagedChannel grpcChannel() {
		return InProcessChannelBuilder.forName(this.serverName).directExecutor().build();
	}

	@Bean
	BeanPostProcessor bindGrpcServicesPostProcessor() {
		return new BeanPostProcessor() {

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof BindableService bindableService) {
					TestInProcessConfiguration.this.serverBuilder.addService(bindableService);
				}
				return bean;
			}

		};
	}

	@EventListener(ContextRefreshedEvent.class)
	void startServer() throws IOException {
		this.server = this.serverBuilder.build().start();
	}

	@Override
	public void destroy() {
		this.server.shutdownNow();
	}

}
