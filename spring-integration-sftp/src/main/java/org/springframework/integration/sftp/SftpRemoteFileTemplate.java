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
package org.springframework.integration.sftp;

import java.io.File;
import java.util.List;

import org.springframework.integration.Message;
import org.springframework.integration.file.remote.AbstractRemoteFileTemplate;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.sftp.session.SftpFileInfo;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class SftpRemoteFileTemplate extends AbstractRemoteFileTemplate<SftpFileInfo> {

	public File get(Message<?> message) {
		// TODO Auto-generated method stub
		return null;
	}

	public void execute(Message<?> message) {
		// TODO Auto-generated method stub

	}

	public List<FileInfo<SftpFileInfo>> ls(Message<?> message) {
		// TODO Auto-generated method stub
		return null;
	}

}
