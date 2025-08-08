/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.FileHandle;
import org.apache.sshd.sftp.server.SftpEventListener;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * A listener for SFTP events emitted by an Apache Mina sshd/sftp server.
 * It emits selected events as Spring Framework {@code ApplicationEvent}s
 * which are subclasses of {@link ApacheMinaSftpEvent}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public class ApacheMinaSftpEventListener
		implements SftpEventListener, ApplicationEventPublisherAware, BeanNameAware, InitializingBean {

	private ApplicationEventPublisher applicationEventPublisher;

	private String beanName;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(this.applicationEventPublisher != null, "An ApplicationEventPublisher is required");
	}

	@Override
	public void initialized(ServerSession session, int version) {
		this.applicationEventPublisher.publishEvent(new SessionOpenedEvent(session, version));
	}

	@Override
	public void destroying(ServerSession session) {
		this.applicationEventPublisher.publishEvent(new SessionClosedEvent(session));
	}

	@Override
	public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) {
		this.applicationEventPublisher.publishEvent(new DirectoryCreatedEvent(session, path, attrs));
	}

	@Override
	public void removed(ServerSession session, Path path, boolean isDirectory, Throwable thrown) {
		this.applicationEventPublisher.publishEvent(new PathRemovedEvent(session, path, isDirectory, thrown));
	}

	@Override
	public void written(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data,
			int dataOffset, int dataLen, Throwable thrown) {

		this.applicationEventPublisher.publishEvent(new FileWrittenEvent(session, remoteHandle, localHandle.getFile(),
				dataLen, thrown));
	}

	@Override
	public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts,
			Throwable thrown) {

		this.applicationEventPublisher.publishEvent(new PathMovedEvent(session, srcPath, dstPath, thrown));
	}

	@Override
	public String toString() {
		return "ApacheMinaSftpEventListener [beanName=" + this.beanName + "]";
	}

}
