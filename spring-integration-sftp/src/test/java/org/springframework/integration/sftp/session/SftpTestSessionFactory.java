/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
