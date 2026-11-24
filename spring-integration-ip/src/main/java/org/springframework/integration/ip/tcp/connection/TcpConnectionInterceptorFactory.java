/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

