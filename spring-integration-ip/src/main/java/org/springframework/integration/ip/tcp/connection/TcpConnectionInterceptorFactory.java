/*
 * Copyright © 2001 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2001-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

/**
 * Interface for TCP connection interceptor factories.
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public interface TcpConnectionInterceptorFactory {

	/**
	 * Called for each new connection;
	 * a new interceptor must be returned on each call.
	 *
	 * @return the TcpInterceptor
	 */
	TcpConnectionInterceptorSupport getInterceptor();

}

