/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.sftp.config;

import org.springframework.integration.sftp.SftpSessionFactory;


/**
 * Provides a single place to handle this tedious chore.
 *
 * @author Josh Long
 */
public class SftpSessionUtils {
	/**
	 * This method hides the minutae required to build an #SftpSessionFactory.
	 *
	 * @param host      the host to connect to.
	 * @param usr       this is required. It is the username of the credentials being authenticated.
	 * @param pw        if password authentication is being used (as opposed to key-based authentication) then this is
	 *                  where you configure the password.
	 * @param pvKey     the file that is the private key
	 * @param pvKeyPass the passphrase used to use the key file
	 * @param port      the default (22) is used if the value here is N< 0. The value should be only be set if the port
	 *                  is non-standard (not 22)
	 * @return the SftpSessionFactory that's used to create connections and get us in the right state to start issue
	 *         commands against a remote SFTP/SSH filesystem
	 * @throws Exception thrown in case of darned near <em>anything</em>
	 */
	public static SftpSessionFactory buildSftpSessionFactory(String host, String pw, String usr, String pvKey, String pvKeyPass, int port)
			throws Exception {
		SftpSessionFactory sftpSessionFactory = new SftpSessionFactory();
		sftpSessionFactory.setPassword(pw);
		sftpSessionFactory.setPort(port);
		sftpSessionFactory.setRemoteHost(host);
		sftpSessionFactory.setUser(usr);
		sftpSessionFactory.setPrivateKey(pvKey);
		sftpSessionFactory.setPrivateKeyPassphrase(pvKeyPass);
		sftpSessionFactory.afterPropertiesSet();

		return sftpSessionFactory;
	}
}
