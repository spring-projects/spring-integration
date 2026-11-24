/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
