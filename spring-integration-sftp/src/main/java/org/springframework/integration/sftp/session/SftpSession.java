/*
 * Copyright 2002-2010 the original author or authors.
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

import com.jcraft.jsch.ChannelSftp;

/**
 * There are many ways to create a {@link SftpSession} just as there are many ways to SSH into a remote system.
 * You may use a username and password, you may use a username and private key, you may use a username and a private key with a password, etc.
 * <p/>
 * This object represents the connection to the remote server, and to use it you must provide it with all the components you'd normally provide an
 * incantation of the <code>ssh</code> command.
 *
 * @author Josh Long
 * @author Mario Gray
 * @since 2.0
 */
public interface SftpSession {

	ChannelSftp getChannel();

	void connect();

	void disconnect();

}
