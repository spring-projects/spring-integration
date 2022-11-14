/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.smb.outbound;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.session.SmbFileInfo;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * Outbound Gateway for performing remote file operations via SMB.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbOutboundGateway extends AbstractRemoteFileOutboundGateway<SmbFile> {

	private static final Log logger = LogFactory.getLog(SmbOutboundGateway.class);

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public SmbOutboundGateway(SessionFactory<SmbFile> sessionFactory,
			MessageSessionCallback<SmbFile, ?> messageSessionCallback) {

		this(new SmbRemoteFileTemplate(sessionFactory), messageSessionCallback);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public SmbOutboundGateway(RemoteFileTemplate<SmbFile> remoteFileTemplate,
			MessageSessionCallback<SmbFile, ?> messageSessionCallback) {

		super(remoteFileTemplate, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public SmbOutboundGateway(SessionFactory<SmbFile> sessionFactory, String command, String expression) {
		this(new SmbRemoteFileTemplate(sessionFactory), command, expression);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public SmbOutboundGateway(RemoteFileTemplate<SmbFile> remoteFileTemplate, String command, String expression) {
		super(remoteFileTemplate, command, expression);
	}

	/**
	 * Construct an instance with the supplied session factory
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the SMB Client.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 */
	public SmbOutboundGateway(SessionFactory<SmbFile> sessionFactory, String command) {
		this(sessionFactory, command, null);
	}

	/**
	 * Construct an instance with the supplied remote file template
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the SMB Client.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 */
	public SmbOutboundGateway(RemoteFileTemplate<SmbFile> remoteFileTemplate, String command) {
		this(remoteFileTemplate, command, null);
	}

	@Override
	public String getComponentType() {
		return "smb:outbound-gateway";
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file.isDirectory();
		}
		catch (SmbException se) {
			logger.error("Unable to determine if this SmbFile represents a directory", se);
			return false;
		}
	}

	/**
	 * Symbolic links are currently not supported in the JCIFS v2.x.x
	 * dependent library, so this method will always return false.
	 * @return false
	 */
	@Override
	protected boolean isLink(SmbFile file) {
		return false;
	}

	@Override
	protected String getFilename(SmbFile file) {
		return file.getName();
	}

	@Override
	protected String getFilename(AbstractFileInfo<SmbFile> file) {
		return file.getFilename();
	}

	@Override
	protected long getModified(SmbFile file) {
		return file.getLastModified();
	}

	@Override
	protected List<AbstractFileInfo<SmbFile>> asFileInfoList(Collection<SmbFile> files) {
		List<AbstractFileInfo<SmbFile>> canonicalFiles = new ArrayList<>();
		for (SmbFile file : files) {
			canonicalFiles.add(new SmbFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected SmbFile enhanceNameWithSubDirectory(SmbFile file, String directory) {
		try {
			file.renameTo(new SmbFile(file, directory), true);
			return file;
		}
		catch (SmbException | MalformedURLException | UnknownHostException e) {
			logger.error("Unable to enhance file name with a sub directory path", e);
			return null;
		}
	}

}
