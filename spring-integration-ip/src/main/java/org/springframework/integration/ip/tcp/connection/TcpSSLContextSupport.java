/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
