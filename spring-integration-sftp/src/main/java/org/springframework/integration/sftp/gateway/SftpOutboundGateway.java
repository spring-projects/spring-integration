/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Outbound Gateway for performing remote file operations via SFTP.
 *
 * @author Gary Russell
 * @since 2.1
 */
public class SftpOutboundGateway extends AbstractRemoteFileOutboundGateway<LsEntry> {

	public SftpOutboundGateway(SessionFactory<LsEntry> sessionFactory, String command, String expression) {
		super(sessionFactory, command, expression);
	}

	private SftpOutboundGateway(RemoteFileTemplate<LsEntry> remoteFileTemplate, String command, String expression) {
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
		List<AbstractFileInfo<LsEntry>> canonicalFiles = new ArrayList<AbstractFileInfo<LsEntry>>();
		for (LsEntry file : files) {
			canonicalFiles.add(new SftpFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected long getModified(LsEntry file) {
		return ((long)file.getAttrs().getMTime()) * 1000;
	}

	@Override
	protected LsEntry enhanceNameWithSubDirectory(LsEntry file, String directory) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(file);
		accessor.setPropertyValue("filename", directory + file.getFilename());
		return file;
	}

	@Override
	public String getComponentType() {
		return "sftp:outbound-gateway";
	}

}
