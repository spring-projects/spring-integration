/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.gateway;

import org.apache.sshd.sftp.client.SftpClient;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;

/**
 * Outbound Gateway for performing remote file operations via SFTP.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.sftp.outbound.SftpOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SftpOutboundGateway extends org.springframework.integration.sftp.outbound.SftpOutboundGateway {

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public SftpOutboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			MessageSessionCallback<SftpClient.DirEntry, ?> messageSessionCallback) {

		super(sessionFactory, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public SftpOutboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			MessageSessionCallback<SftpClient.DirEntry, ?> messageSessionCallback) {

		super(remoteFileTemplate, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc.), and an expression to determine the remote path.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the remote path expression.
	 */
	public SftpOutboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory, String command,
			@Nullable String expression) {

		super(sessionFactory, command, expression);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc.), and an expression to determine the remote path.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the remote path expression.
	 */
	public SftpOutboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate, String command,
			@Nullable String expression) {

		super(remoteFileTemplate, command, expression);
	}

}
