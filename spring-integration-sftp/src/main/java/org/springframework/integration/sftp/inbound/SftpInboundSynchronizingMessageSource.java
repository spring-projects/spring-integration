/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.sftp.inbound;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.sftp.filters.SftpPatternMatchingFileListFilter;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpSessionPool;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;


/**
 * a {@link org.springframework.integration.core.MessageSource} implementation for SFTP
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpInboundSynchronizingMessageSource extends 
		AbstractInboundRemoteFileSystemSynchronizingMessageSource<ChannelSftp.LsEntry, SftpInboundSynchronizer> {
	
	/**
	 * the pool of sessions
	 */
	private final SftpSessionPool sessionPool;

	/**
	 * the remote path on the server
	 */
	private volatile String remoteDirectory;

	private volatile Pattern filenamePattern;

	public SftpInboundSynchronizingMessageSource(SftpSessionPool sessionPool){
		this.sessionPool = sessionPool;
	}

	public void setFilenamePattern(Pattern filenamePattern) {
		this.filenamePattern = filenamePattern;
	}
	
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	
	public String getComponentType(){
		return "sftp:inbound-channel-adapter";
	}
	
	public Message<File> receive() {
		/*
		 * Basically keep polling from the file source untill null,
		 * then attempt to sync up with remote directory which should populate the file source
		 * if anything is there  and poll on file source again and if its still null then return it.
		 */
		Message<File> message = this.fileSource.receive();
		if (message == null){
			this.checkThatRemotePathExists(this.remoteDirectory);
			this.synchronizer.syncRemoteToLocalFileSystem();
			message = this.fileSource.receive();
		}
		return message;
	}

	/**
	 * This method will check to ensure that the remote directory exists. If the directory
	 * doesnt exist, and autoCreatePath is 'true,' then this method makes a few reasonably sane attempts
	 * to create it. Otherwise, it fails fast.
	 *
	 * @param remotePath the path on the remote SSH / SFTP server to create.
	 * @return whether or not the directory is there (regardless of whether we created it in this method or it already
	 *         existed.)
	 */
	private boolean checkThatRemotePathExists(String remotePath) {
		SftpSession session = null;
		ChannelSftp channelSftp = null;
		try {
			session = this.sessionPool.getSession();
			session.start();
			channelSftp = session.getChannel();
		} 
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e){
			throw new MessagingException("Failed to get SftpSession while checking for existance of the remote directory", e);
		}
		
		try {
			SftpATTRS attrs = channelSftp.stat(remotePath);
			assert (attrs != null) && attrs.isDir() : "attrs can't be null, and should indicate that it's a directory!";
			return true;
		} 
		catch (Throwable th) {
			if (this.autoCreateDirectories && (this.sessionPool != null) && (session != null)) {
				try {
					if (channelSftp != null) {
						channelSftp.mkdir(remotePath);

						if (channelSftp.stat(remotePath).isDir()) {
							return true;
						}
					}
				} 
				catch (RuntimeException re) {
					throw re;
				}
				catch (Exception e){
					throw new MessagingException("Failed to auto-create remote directory", e);
				}

			}
		} 
		finally {
			this.sessionPool.release(session);
		}

		return false;
	}

	@Override
	protected void onInit() {
		try {
			if (this.localDirectory != null && !this.localDirectory.exists()) {
				if (this.autoCreateDirectories) {
					if (logger.isDebugEnabled()) {
						logger.debug("The '" + this.localDirectory + "' directory doesn't exist; Will create.");
					}
					this.localDirectory.getFile().mkdirs();
				}
				else {
					throw new FileNotFoundException(this.localDirectory.getFilename());
				}
			}
			/**
			 * Forwards files once they ultimately appear in the {@link #localDirectory}.
			 */
			this.fileSource = new FileReadingMessageSource();
			this.fileSource.setDirectory(this.localDirectory.getFile());
			this.fileSource.afterPropertiesSet();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessagingException("Failure during initialization of MessageSource for: "
					+ this.getComponentType(), e);
		}

		if (filenamePattern != null) {
			SftpPatternMatchingFileListFilter sftpFilePatternMatchingEntryListFilter =
					new SftpPatternMatchingFileListFilter(filenamePattern);
			this.synchronizer.setFilter(sftpFilePatternMatchingEntryListFilter);
		}
	}
}
