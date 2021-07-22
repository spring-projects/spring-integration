/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.sftp.support.GeneralSftpException;
import org.springframework.util.ReflectionUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;

/**
 * Outbound Gateway for performing remote file operations via SFTP.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class SftpOutboundGateway extends AbstractRemoteFileOutboundGateway<LsEntry> {

	private static final Method LS_ENTRY_SET_FILENAME_METHOD;

	static {
		LS_ENTRY_SET_FILENAME_METHOD = ReflectionUtils.findMethod(LsEntry.class, "setFilename", String.class);
		if (LS_ENTRY_SET_FILENAME_METHOD != null) {
			ReflectionUtils.makeAccessible(LS_ENTRY_SET_FILENAME_METHOD);
		}
	}

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public SftpOutboundGateway(SessionFactory<LsEntry> sessionFactory,
			MessageSessionCallback<LsEntry, ?> messageSessionCallback) {

		this(new SftpRemoteFileTemplate(sessionFactory), messageSessionCallback);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public SftpOutboundGateway(RemoteFileTemplate<LsEntry> remoteFileTemplate,
			MessageSessionCallback<LsEntry, ?> messageSessionCallback) {

		super(remoteFileTemplate, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public SftpOutboundGateway(SessionFactory<LsEntry> sessionFactory, String command, String expression) {
		this(new SftpRemoteFileTemplate(sessionFactory), command, expression);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public SftpOutboundGateway(RemoteFileTemplate<LsEntry> remoteFileTemplate, String command, String expression) {
		super(remoteFileTemplate, command, expression);
	}

	@Override
	protected boolean isDirectory(LsEntry file) {
		return file.getAttrs().isDir();
	}

	@Override
	protected boolean isLink(LsEntry file) {
		return file.getAttrs().isLink();
	}

	@Override
	protected String getFilename(LsEntry file) {
		return file.getFilename();
	}

	@Override
	protected String getFilename(AbstractFileInfo<LsEntry> file) {
		return file.getFilename();
	}

	@Override
	protected List<AbstractFileInfo<LsEntry>> asFileInfoList(Collection<LsEntry> files) {
		return files.stream()
				.map(SftpFileInfo::new)
				.collect(Collectors.toList());
	}

	@Override
	protected long getModified(LsEntry file) {
		return ((long) file.getAttrs().getMTime()) * 1000; // NOSONAR magic number
	}

	@Override
	protected LsEntry enhanceNameWithSubDirectory(LsEntry file, String directory) {
		ReflectionUtils.invokeMethod(LS_ENTRY_SET_FILENAME_METHOD, file, directory + file.getFilename());
		return file;
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
	protected void doChmod(RemoteFileOperations<LsEntry> remoteFileOperations, final String path, final int chmod) {
		remoteFileOperations
				.executeWithClient((ClientCallbackWithoutResult<ChannelSftp>) client -> {
					try {
						client.chmod(chmod, path);
					}
					catch (SftpException e) {
						throw new GeneralSftpException(
								"Failed to execute 'chmod " + Integer.toOctalString(chmod) + " " + path + "'", e);
					}
				});
	}

}
