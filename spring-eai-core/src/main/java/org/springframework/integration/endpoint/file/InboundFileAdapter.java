/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.endpoint.AbstractInboundChannelAdapter;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A basic inbound channel adapter for polling a directory.
 * 
 * @author Mark Fisher
 */
public class InboundFileAdapter extends AbstractInboundChannelAdapter {

	private File directory;

	private File copyToDirectory;

	private FileFilter fileFilter;

	private FilenameFilter filenameFilter;

	private boolean isTextFile = true;


	public InboundFileAdapter(File directory) {
		Assert.notNull("directory must not be null");
		this.directory = directory;
	}

	public void setCopyToDirectory(File copyToDirectory) {
		this.copyToDirectory = copyToDirectory;
	}

	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	public void setIsTextFile(boolean isTextFile) {
		this.isTextFile = isTextFile;
	}


	@Override
	protected Object doReceiveObject() {
		System.out.println("polling");
		File[] files = null;
		if (this.fileFilter != null) {
			files = this.directory.listFiles(fileFilter);
		}
		else if (this.filenameFilter != null) {
			files = this.directory.listFiles(filenameFilter);
		}
		else {
			files = this.directory.listFiles();
		}
		if (files == null) {
			throw new MessageHandlingException("Problem occurred while polling for files. " +
					"Is '" + directory.getAbsolutePath() + "' a directory?");
		}
		if (files.length == 0) {
			return null;
		}
		File file = files[0];
		try {
			if (this.isTextFile) {
				FileReader reader = new FileReader(file);
				String result = FileCopyUtils.copyToString(reader);
				if (this.copyToDirectory != null) {
					FileWriter writer = new FileWriter(this.copyToDirectory.getAbsolutePath() + File.separator + file.getName());
					FileCopyUtils.copy(new FileReader(file), writer);
				}
				file.delete();
				return result;
			}
			return FileCopyUtils.copyToByteArray(file);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new MessageHandlingException("Failed to extract message", e);
		}
	}

}
