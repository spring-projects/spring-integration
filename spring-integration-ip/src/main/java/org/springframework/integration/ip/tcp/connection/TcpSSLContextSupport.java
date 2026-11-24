/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

/**
 * Strategy interface for the creation of an {@link SSLContext} object
 * for use with SSL/TLS sockets.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface TcpSSLContextSupport {

	/**
	 * Gets an SSLContext.
	 * @return the SSLContext.
	 * @throws GeneralSecurityException Any GeneralSecurityException.
	 * @throws IOException Any IOException.
	 */
	SSLContext getSSLContext() throws GeneralSecurityException, IOException;

}
