/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

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
	public static SftpInboundChannelAdapterSpec inboundAdapter(SessionFactory<ChannelSftp.LsEntry> sessionFactory) {
		return inboundAdapter(sessionFactory, null);
	}

	/**
	 * An {@link SftpInboundChannelAdapterSpec} factory for an inbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param receptionOrderComparator the comparator.
	 * @return the spec.
	 */
	public static SftpInboundChannelAdapterSpec inboundAdapter(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			Comparator<File> receptionOrderComparator) {

		return new SftpInboundChannelAdapterSpec(sessionFactory, receptionOrderComparator);
	}

	/**
	 * An {@link SftpStreamingInboundChannelAdapterSpec} factory for an inbound channel
	 * adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @return the spec.
	 */
	public static SftpStreamingInboundChannelAdapterSpec inboundStreamingAdapter(
			RemoteFileTemplate<LsEntry> remoteFileTemplate) {

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
			RemoteFileTemplate<LsEntry> remoteFileTemplate,
			Comparator<LsEntry> receptionOrderComparator) {

		return new SftpStreamingInboundChannelAdapterSpec(remoteFileTemplate, receptionOrderComparator);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @return the spec.
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SessionFactory<ChannelSftp.LsEntry> sessionFactory) {
		return new SftpMessageHandlerSpec(sessionFactory);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 */
	public static SftpMessageHandlerSpec outboundAdapter(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			FileExistsMode fileExistsMode) {

		return outboundAdapter(new SftpRemoteFileTemplate(sessionFactory), fileExistsMode);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @return the spec.
	 * @deprecated in favor of {@link #outboundAdapter(SftpRemoteFileTemplate)}
	 */
	@Deprecated
	public static SftpMessageHandlerSpec outboundAdapter(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate) {
		return new SftpMessageHandlerSpec(remoteFileTemplate);
	}

	/**
	 * An {@link SftpMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 * @deprecated in favor of
	 * {@link #outboundAdapter(SftpRemoteFileTemplate,FileExistsMode)}
	 */
	@Deprecated
	public static SftpMessageHandlerSpec outboundAdapter(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate,
			FileExistsMode fileExistsMode) {

		return new SftpMessageHandlerSpec(remoteFileTemplate, fileExistsMode);
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
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the FTP.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SftpOutboundGatewaySpec}
	 */
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			AbstractRemoteFileOutboundGateway.Command command, String expression) {

		return outboundGateway(sessionFactory, command.getCommand(), expression);
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
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			String command, String expression) {

		return new SftpOutboundGatewaySpec(new SftpOutboundGateway(sessionFactory, command, expression));
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
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate,
			AbstractRemoteFileOutboundGateway.Command command, String expression) {

		return outboundGateway(remoteFileTemplate, command.getCommand(), expression);
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
	public static SftpOutboundGatewaySpec outboundGateway(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate,
			String command, String expression) {

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
	public static SftpOutboundGatewaySpec outboundGateway(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			MessageSessionCallback<ChannelSftp.LsEntry, ?> messageSessionCallback) {

		return new SftpOutboundGatewaySpec(new SftpOutboundGateway(sessionFactory, messageSessionCallback));
	}

	private Sftp() {
	}

}
