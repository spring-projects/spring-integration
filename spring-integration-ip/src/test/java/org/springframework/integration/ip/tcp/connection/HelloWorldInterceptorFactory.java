/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
