/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ftp.gateway;

import org.apache.commons.net.ftp.FTPFile;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;

/**
 * Outbound Gateway for performing remote file operations via FTP/FTPS.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ftp.outbound.FtpOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class FtpOutboundGateway extends org.springframework.integration.ftp.outbound.FtpOutboundGateway {

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory,
			MessageSessionCallback<FTPFile, ?> messageSessionCallback) {

		super(sessionFactory, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate,
			MessageSessionCallback<FTPFile, ?> messageSessionCallback) {

		super(remoteFileTemplate, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc.), and an expression to determine the remote path.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the remote path expression.
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory, String command, @Nullable String expression) {
		super(sessionFactory, command, expression);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc.), and an expression to determine the remote path.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the remote path expression.
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate, String command,
			@Nullable String expression) {

		super(remoteFileTemplate, command, expression);
	}

	/**
	 * Construct an instance with the supplied session factory
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the FTP Client.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @since 4.3
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory, String command) {
		this(sessionFactory, command, null);
	}

	/**
	 * Construct an instance with the supplied remote file template
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the FTP Client.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @since 4.3
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate, String command) {
		this(remoteFileTemplate, command, null);
	}

}
