/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.sftp.session;

import org.apache.sshd.sftp.client.SftpClient;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class SftpTestSessionFactory {

	private SftpTestSessionFactory() {
		super();
	}

	public static SftpSession createSftpSession(SftpClient sftpClient) {
		SftpSession sftpSession = spy(new SftpSession(sftpClient));
		willReturn("mock.sftp.host:22")
				.given(sftpSession)
				.getHostPort();
		return sftpSession;
	}

}
