/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import org.apache.commons.net.ftp.FTPClient;

/**
 * Default implementation of FTP SessionFactory.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @since 2.0
 */
public class DefaultFtpSessionFactory extends AbstractFtpSessionFactory<FTPClient> {

	@Override
	protected FTPClient createClientInstance() {
		return new FTPClient();
	}

}
