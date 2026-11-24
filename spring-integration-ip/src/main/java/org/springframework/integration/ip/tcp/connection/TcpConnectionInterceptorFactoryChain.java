/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
