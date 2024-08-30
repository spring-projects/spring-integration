/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.sftp.dsl;

import java.io.File;
import java.util.Comparator;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.lang.Nullable;

/**
 * The factory for SFTP components.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Deepak Gunasekaran
 *
 * @since 5.0
 */
public final class Sftp {

	/**
	 * An {@link SftpInboundChannelAdapterSpec} factory for an inbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @return the spec.
	 */
	public static SftpInboundChannelAdapterSpec inboundAdapter(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		return inboundAdapter(sessionFactory, null);
	}

	/**
	 * An {@link SftpInboundChannelAdapterSpec} factory for an inbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param receptionOrderComparator the comparator.
	 * @return the spec.
	 */
	public static SftpInboundChannelAdapterSpec inboundAdapter(SessionFactory<SftpClient.DirEntry> sessionFactory,
			@Nullable Comparator<File> receptionOrderComparator) {

		return new SftpInboundChannelAdapterSpec(sessionFactory, receptionOrderComparator);
	}

	/**
	 * An {@link SftpStreamingInboundChannelAdapterSpec} factory for an inbound channel
	 * adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @return the spec.
	 */
	public static SftpStreamingInboundChannelAdapterSpec inboundStreamingAdapter(
			RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate) {

		return inboundStreamingAdapter(remoteFileTemplate, null);
	}

	/**
	 * An {@link SftpStreamingInboundChannelAdapterSpec} factory for an inbound channel
	 * adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @param receptionOrderComparator the comparator.
	 * @return the spec.
	 */
	public static SftpStreamingInboundChannelAdapterSpec inboundStreamingAdapter(
			RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			@Nullable Comparator<SftpClient.DirEntry> receptionOrderComparator) {

		return new SftpStreamingInboundChannelAdapterSpec(remoteFileTemplate, receptionOrderComparator);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @return the spec.
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		return new SftpMessageHandlerSpec(sessionFactory);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SessionFactory<SftpClient.DirEntry> sessionFactory,
			FileExistsMode fileExistsMode) {

		return outboundAdapter(new SftpRemoteFileTemplate(sessionFactory), fileExistsMode);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sftpRemoteFileTemplate the remote file template.
	 * @return the spec.
	 * @since 5.4
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SftpRemoteFileTemplate sftpRemoteFileTemplate) {
		return new SftpMessageHandlerSpec(sftpRemoteFileTemplate);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sftpRemoteFileTemplate the remote file template.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 * @since 5.4
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SftpRemoteFileTemplate sftpRemoteFileTemplate,
			FileExistsMode fileExistsMode) {

		return new SftpMessageHandlerSpec(sftpRemoteFileTemplate, fileExistsMode);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command}.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the FTP.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @since 6.4
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			AbstractRemoteFileOutboundGateway.Command command) {

		return outboundGateway(sessionFactory, command, null);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the FTP.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SftpOutboundGatewaySpec}
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			AbstractRemoteFileOutboundGateway.Command command, @Nullable String expression) {

		return outboundGateway(sessionFactory, command.getCommand(), expression);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command}.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the FTP.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @since 6.4
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			String command) {

		return outboundGateway(sessionFactory, command, null);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the FTP.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			String command, @Nullable String expression) {

		return new SftpOutboundGatewaySpec(new SftpOutboundGateway(sessionFactory, command, expression));
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command}.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate} to be based on.
	 * @param command the command to perform on the SFTP.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @since 6.4
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			AbstractRemoteFileOutboundGateway.Command command) {

		return outboundGateway(remoteFileTemplate, command, null);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the remote path.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate} to be based on.
	 * @param command the command to perform on the SFTP.
	 * @param expression the remote path SpEL expression.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			AbstractRemoteFileOutboundGateway.Command command, @Nullable String expression) {

		return outboundGateway(remoteFileTemplate, command.getCommand(), expression);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command}.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate} to be based on.
	 * @param command the command to perform on the SFTP.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @since 6.4
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			String command) {

		return outboundGateway(remoteFileTemplate, command, null);
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the remoteFilePath.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate} to be based on.
	 * @param command the command to perform on the SFTP.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			String command, @Nullable String expression) {

		return new SftpOutboundGatewaySpec(new SftpOutboundGateway(remoteFileTemplate, command, expression));
	}

	/**
	 * Produce a {@link SftpOutboundGatewaySpec} based on the {@link MessageSessionCallback}.
	 * @param sessionFactory the {@link SessionFactory} to connect to.
	 * @param messageSessionCallback the {@link MessageSessionCallback} to perform SFTP operation(s)
	 *                               with the {@code Message} context.
	 * @return the {@link SftpOutboundGatewaySpec}
	 * @see MessageSessionCallback
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			MessageSessionCallback<SftpClient.DirEntry, ?> messageSessionCallback) {

		return new SftpOutboundGatewaySpec(new SftpOutboundGateway(sessionFactory, messageSessionCallback));
	}

	private Sftp() {
	}

}
