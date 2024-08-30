/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.sftp.gateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.lang.Nullable;

/**
 * Outbound Gateway for performing remote file operations via SFTP.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class SftpOutboundGateway extends AbstractRemoteFileOutboundGateway<SftpClient.DirEntry> {

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public SftpOutboundGateway(SessionFactory<SftpClient.DirEntry> sessionFactory,
			MessageSessionCallback<SftpClient.DirEntry, ?> messageSessionCallback) {

		this(new SftpRemoteFileTemplate(sessionFactory), messageSessionCallback);
		remoteFileTemplateExplicitlySet(false);
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

		this(new SftpRemoteFileTemplate(sessionFactory), command, expression);
		remoteFileTemplateExplicitlySet(false);
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

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file.getAttributes().isDirectory();
	}

	@Override
	protected boolean isLink(SftpClient.DirEntry file) {
		return file.getAttributes().isSymbolicLink();
	}

	@Override
	protected String getFilename(SftpClient.DirEntry file) {
		return file.getFilename();
	}

	@Override
	protected String getFilename(AbstractFileInfo<SftpClient.DirEntry> file) {
		return file.getFilename();
	}

	@Override
	protected List<AbstractFileInfo<SftpClient.DirEntry>> asFileInfoList(Collection<SftpClient.DirEntry> files) {
		return files.stream()
				.map(SftpFileInfo::new)
				.collect(Collectors.toList());
	}

	@Override
	protected long getModified(SftpClient.DirEntry file) {
		return file.getAttributes().getModifyTime().toMillis();
	}

	@Override
	protected SftpClient.DirEntry enhanceNameWithSubDirectory(SftpClient.DirEntry file, String directory) {
		return new SftpClient.DirEntry(directory + file.getFilename(), directory + file.getFilename(),
				file.getAttributes());
	}

	@Override
	public String getComponentType() {
		return "sftp:outbound-gateway";
	}

	@Override
	public boolean isChmodCapable() {
		return true;
	}

	@Override
	protected void doChmod(RemoteFileOperations<SftpClient.DirEntry> remoteFileOperations, String path, int chmod) {
		remoteFileOperations.executeWithClient((ClientCallbackWithoutResult<SftpClient>) client -> {
			try {
				SftpClient.Attributes attributes = client.stat(path);
				attributes.setPermissions(chmod);
				client.setStat(path, attributes);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(
						"Failed to execute 'chmod " + Integer.toOctalString(chmod) + " " + path + "'", ex);
			}
		});
	}

}
