/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.dsl;

import java.io.File;
import java.util.Comparator;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.smb.outbound.SmbOutboundGateway;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;
import org.springframework.lang.Nullable;

/**
 * The factory for SMB components.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class Smb {

	/**
	 * A {@link SmbInboundChannelAdapterSpec} factory for an inbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @return the spec.
	 */
	public static SmbInboundChannelAdapterSpec inboundAdapter(SessionFactory<SmbFile> sessionFactory) {
		return inboundAdapter(sessionFactory, null);
	}

	/**
	 * A {@link SmbInboundChannelAdapterSpec} factory for an inbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param receptionOrderComparator the comparator.
	 * @return the spec.
	 */
	public static SmbInboundChannelAdapterSpec inboundAdapter(SessionFactory<SmbFile> sessionFactory,
			@Nullable Comparator<File> receptionOrderComparator) {

		return new SmbInboundChannelAdapterSpec(sessionFactory, receptionOrderComparator);
	}

	/**
	 * A {@link SmbStreamingInboundChannelAdapterSpec} factory for an inbound channel
	 * adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @return the spec.
	 */
	public static SmbStreamingInboundChannelAdapterSpec inboundStreamingAdapter(
			RemoteFileTemplate<SmbFile> remoteFileTemplate) {

		return inboundStreamingAdapter(remoteFileTemplate, null);
	}

	/**
	 * A {@link SmbStreamingInboundChannelAdapterSpec} factory for an inbound channel
	 * adapter spec.
	 * @param remoteFileTemplate the remote file template.
	 * @param receptionOrderComparator the comparator.
	 * @return the spec.
	 */
	public static SmbStreamingInboundChannelAdapterSpec inboundStreamingAdapter(
			RemoteFileTemplate<SmbFile> remoteFileTemplate,
			@Nullable Comparator<SmbFile> receptionOrderComparator) {

		return new SmbStreamingInboundChannelAdapterSpec(remoteFileTemplate, receptionOrderComparator);
	}

	/**
	 * A {@link SmbMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @return the spec.
	 */
	public static SmbMessageHandlerSpec outboundAdapter(SessionFactory<SmbFile> sessionFactory) {
		return new SmbMessageHandlerSpec(sessionFactory);
	}

	/**
	 * A {@link SmbMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param sessionFactory the session factory.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 */
	public static SmbMessageHandlerSpec outboundAdapter(SessionFactory<SmbFile> sessionFactory,
			FileExistsMode fileExistsMode) {

		return outboundAdapter(new SmbRemoteFileTemplate(sessionFactory), fileExistsMode);
	}

	/**
	 * A {@link SmbMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param smbRemoteFileTemplate the remote file template.
	 * @return the spec.
	 */
	public static SmbMessageHandlerSpec outboundAdapter(SmbRemoteFileTemplate smbRemoteFileTemplate) {
		return new SmbMessageHandlerSpec(smbRemoteFileTemplate);
	}

	/**
	 * A {@link SmbMessageHandlerSpec} factory for an outbound channel adapter spec.
	 * @param smbRemoteFileTemplate the remote file template.
	 * @param fileExistsMode the file exists mode.
	 * @return the spec.
	 */
	public static SmbMessageHandlerSpec outboundAdapter(SmbRemoteFileTemplate smbRemoteFileTemplate,
			FileExistsMode fileExistsMode) {

		return new SmbMessageHandlerSpec(smbRemoteFileTemplate, fileExistsMode);
	}

	/**
	 * Produce a {@link SmbOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the SMB.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SmbOutboundGatewaySpec}
	 */
	public static SmbOutboundGatewaySpec outboundGateway(SessionFactory<SmbFile> sessionFactory,
			AbstractRemoteFileOutboundGateway.Command command, String expression) {

		return outboundGateway(sessionFactory, command.getCommand(), expression);
	}

	/**
	 * Produce a {@link SmbOutboundGatewaySpec} based on the {@link SessionFactory},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param sessionFactory the {@link SessionFactory}.
	 * @param command the command to perform on the SMB.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SmbOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SmbOutboundGatewaySpec outboundGateway(SessionFactory<SmbFile> sessionFactory,
			String command, String expression) {

		return new SmbOutboundGatewaySpec(new SmbOutboundGateway(sessionFactory, command, expression));
	}

	/**
	 * Produce a {@link SmbOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate}.
	 * @param command the command to perform on the SMB.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SmbOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SmbOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SmbFile> remoteFileTemplate,
			AbstractRemoteFileOutboundGateway.Command command, String expression) {

		return outboundGateway(remoteFileTemplate, command.getCommand(), expression);
	}

	/**
	 * Produce a {@link SmbOutboundGatewaySpec} based on the {@link RemoteFileTemplate},
	 * {@link AbstractRemoteFileOutboundGateway.Command} and {@code expression} for the
	 * remoteFilePath.
	 * @param remoteFileTemplate the {@link RemoteFileTemplate}.
	 * @param command the command to perform on the SMB.
	 * @param expression the remoteFilePath SpEL expression.
	 * @return the {@link SmbOutboundGatewaySpec}
	 * @see RemoteFileTemplate
	 */
	public static SmbOutboundGatewaySpec outboundGateway(RemoteFileTemplate<SmbFile> remoteFileTemplate,
			String command, String expression) {

		return new SmbOutboundGatewaySpec(new SmbOutboundGateway(remoteFileTemplate, command, expression));
	}

	/**
	 * Produce a {@link SmbOutboundGatewaySpec} based on the
	 * {@link MessageSessionCallback}.
	 * @param sessionFactory the {@link SessionFactory} to connect to.
	 * @param messageSessionCallback the {@link MessageSessionCallback} to perform SMB.
	 * operation(s) with the {@code Message} context.
	 * @return the {@link SmbOutboundGatewaySpec}
	 * @see MessageSessionCallback
	 */
	public static SmbOutboundGatewaySpec outboundGateway(SessionFactory<SmbFile> sessionFactory,
			MessageSessionCallback<SmbFile, ?> messageSessionCallback) {

		return new SmbOutboundGatewaySpec(new SmbOutboundGateway(sessionFactory, messageSessionCallback));
	}

	private Smb() {
	}

}
