/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.ftp.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.FtpFileInfo;

/**
 * Outbound Gateway for performing remote file operations via FTP/FTPS.
 *  
 * @author Gary Russell
 * @since 2.1
 */
public class FtpOutboundGateway extends AbstractRemoteFileOutboundGateway<FTPFile> {

	public FtpOutboundGateway(SessionFactory sessionFactory, String command,
			String expression) {
		super(sessionFactory, command, expression);
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file.isDirectory();
	}

	@Override
	protected boolean isLink(FTPFile file) {
		return file.isSymbolicLink();
	}

	@Override
	protected String getFilename(FTPFile file) {
		return file.getName();
	}

	@Override
	protected long getModified(FTPFile file) {
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	protected List<AbstractFileInfo<FTPFile>> asFileInfoList(Collection<FTPFile> files) {
		List<AbstractFileInfo<FTPFile>> canonicalFiles = new ArrayList<AbstractFileInfo<FTPFile>>();
		for (FTPFile file : files) {
			canonicalFiles.add(new FtpFileInfo(file));
		}
		return canonicalFiles;

	}

}
