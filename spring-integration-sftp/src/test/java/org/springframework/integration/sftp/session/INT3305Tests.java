/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.sftp.session;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.ConnectException;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.Test;

import org.springframework.integration.test.util.SocketUtils;

import com.jcraft.jsch.JSchException;

/**
 * @author Gary Russell
 * @since 3.0.2
 *
 */
public class INT3305Tests {

	/*
	 * Verify the socket is closed if the channel.connect() fails.
	 */
	@Test
	public void testConnectFailSocketOpen() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		SshServer server = SshServer.setUpDefaultServer();
		try {
			server.setPasswordAuthenticator(new PasswordAuthenticator() {

				@Override
				public boolean authenticate(String arg0, String arg1, ServerSession arg2) {
					return true;
				}
			});
			server.setPort(port);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(port);
			f.setUser("user");
			f.setPassword("pass");
			int n = 0;
			while (true) {
				try {
					f.getSession();
					fail("Expected Exception");
				}
				catch (Exception e) {
					if (e instanceof IllegalStateException && "failed to create SFTP Session".equals(e.getMessage())) {
						if (e.getCause() instanceof IllegalStateException) {
							if (e.getCause().getCause() instanceof JSchException) {
								if (e.getCause().getCause().getCause() instanceof ConnectException) {
									assertTrue("Server failed to start in 10 seconds", n++ < 100);
									Thread.sleep(100);
									continue;
								}
							}
						}
					}
					assertThat(e, instanceOf(IllegalStateException.class));
					assertThat(e.getCause(), instanceOf(IllegalStateException.class));
					assertThat(e.getCause().getMessage(), equalTo("failed to connect"));
					break;
				}
			}

			n = 0;
			while (n++ < 100 && server.getActiveSessions().size() > 0) {
				Thread.sleep(100);
			}

			assertEquals(0, server.getActiveSessions().size());
		}
		finally {
			server.stop(true);
		}
	}

}
