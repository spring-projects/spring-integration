/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpConnectionInterceptorFactoryChain {

	private TcpConnectionInterceptorFactory @Nullable [] interceptorFactories;

	public TcpConnectionInterceptorFactory @Nullable [] getInterceptorFactories() {
		return this.interceptorFactories; //NOSONAR
	}

	public void setInterceptors(TcpConnectionInterceptorFactory[] interceptorFactories) {
		this.interceptorFactories = Arrays.copyOf(interceptorFactories, interceptorFactories.length);
	}

	public void setInterceptor(TcpConnectionInterceptorFactory... interceptorFactories) {
		this.interceptorFactories = Arrays.copyOf(interceptorFactories, interceptorFactories.length);
	}

}
