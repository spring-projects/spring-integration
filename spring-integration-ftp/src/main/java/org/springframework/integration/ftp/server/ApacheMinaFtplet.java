/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.ftp.server;

import java.io.IOException;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * A listener for FTP events emitted by an Apache Mina ftp server.
 * It emits selected events as Spring Framework {@code ApplicationEvent}s
 * which are subclasses of {@link ApacheMinaFtpEvent}.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class ApacheMinaFtplet extends DefaultFtplet
		implements ApplicationEventPublisherAware, BeanNameAware, InitializingBean {

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
	public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new SessionOpenedEvent(session));
		return super.onConnect(session);
	}

	@Override
	public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new SessionClosedEvent(session));
		return super.onDisconnect(session);
	}

	@Override
	public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new PathRemovedEvent(session, request, false));
		return super.onDeleteEnd(session, request);
	}

	@Override
	public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new FileWrittenEvent(session, request, false));
		return super.onUploadEnd(session, request);
	}

	@Override
	public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new PathRemovedEvent(session, request, true));
		return super.onRmdirEnd(session, request);
	}

	@Override
	public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new DirectoryCreatedEvent(session, request));
		return super.onMkdirEnd(session, request);
	}

	@Override
	public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new FileWrittenEvent(session, request, false));
		return super.onAppendEnd(session, request);
	}

	@Override
	public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
		this.applicationEventPublisher.publishEvent(new PathMovedEvent(session, request));
		return super.onRenameEnd(session, request);
	}

	@Override
	public String toString() {
		return "ApacheMinaSftpEventListener [beanName=" + this.beanName + "]";
	}

}
