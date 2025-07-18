/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class HelloWorldInterceptorFactory implements
		TcpConnectionInterceptorFactory, ApplicationEventPublisherAware {

	private String hello = "Hello";

	private String world = "world!";

	private volatile ApplicationEventPublisher applicationEventPublisher;

	public HelloWorldInterceptorFactory() {
	}

	/**
	 * @param hello
	 * @param world
	 */
	public HelloWorldInterceptorFactory(String hello, String world) {
		this.hello = hello;
		this.world = world;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public TcpConnectionInterceptorSupport getInterceptor() {
		return new HelloWorldInterceptor(this.hello, this.world, this.applicationEventPublisher);
	}

}
