/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.integration.smb.session;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

import org.springframework.core.log.LogAccessor;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link Session} interface for Server Message Block (SMB)
 * also known as Common Internet File System (CIFS). The Samba project set out to
 * create non-Windows implementations of SMB. Often Samba is thus used synonymously to SMB.
 * <p>
 * SMB is an application-layer network protocol that manages shared access to files, printers
 * and other networked resources.
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Server_Message_Block">Server Message Block</a>
 * for more details.
 *
 * @author Markus Spann
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 * @author Gregory Bragg
 * @author Adam Jones
 * @author Paolo Fosser
 *
 * @since 6.0
 */
public class SmbSession implements Session<SmbFile> {

	private static final LogAccessor logger = new LogAccessor(SmbSession.class);

	private static final String SMB_FILE_SEPARATOR = "/";

	private final SmbShare smbShare;

	/**
	 * Constructor for an SMB session.
	 * @param _host server NetBIOS name, DNS name, or IP address, case-insensitive
	 * @param _port port
	 * @param _domain case-sensitive domain name
	 * @param _user case-sensitive username
	 * @param _password case-sensitive password
	 * @param _shareAndDir server root SMB directory
	 * @throws IOException in case of I/O errors
	 */
	SmbSession(String _host, int _port, String _domain, String _user, String _password, String _shareAndDir)
			throws IOException {

		this(new SmbShare(new SmbConfig(_host, _port, _domain, _user, _password, _shareAndDir)));
	}

	/**
	 * Constructor for an SMB session.
	 * @param _smbShare SMB share resource
	 */
	public SmbSession(SmbShare _smbShare) {
		Assert.notNull(_smbShare, "smbShare must not be null");
		this.smbShare = _smbShare;
	}

	/**
	 * Delete the file or directory at the specified path.
	 * @param _path path to a remote file or directory
	 * @return true if delete successful, false if resource is non-existent
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	@Override
	public boolean remove(String _path) throws IOException {
		Assert.hasText(_path, "path must not be empty");

		boolean removed = false;
		SmbFile removeFile = createSmbFileObject(_path);

		if (removeFile.exists()) {
			removeFile.delete();
			removed = true;
		}
		if (!removed) {
			logger.info(() -> "Could not remove non-existing resource [" + _path + "].");
		}
		else {
			logger.info(() -> "Successfully removed resource [" + _path + "].");
		}
		return removed;
	}

	/**
	 * Return the content of the specified SMB resource as an array of SmbFile objects.
	 * In case the remote resource does not exist, an empty array is returned.
	 * @param path path to a remote directory or remote file path
	 * @return array of SmbFile objects
	 * @throws IOException on error conditions returned by a CIFS server.
	 */
	@Override
	public SmbFile[] list(String path) throws IOException {
		String remotePath = StringUtils.trimTrailingCharacter(path, '/');
		SmbFile[] files = null;
		int lastIndex = StringUtils.hasText(remotePath) ? remotePath.lastIndexOf('/') : 0;
		String remoteFileName = lastIndex > 0 ? remotePath.substring(lastIndex + 1) : null;
		if (StringUtils.hasText(remoteFileName)) {
			SmbFile remoteFile = createSmbFileObject(remotePath);
			if (remoteFile.isFile()) {
				files = new SmbFile[] {remoteFile};
			}
		}

		if (files == null) {
			try {
				SmbFile smbDir = createSmbDirectoryObject(remotePath);
				if (!smbDir.exists()) {
					logger.warn(() -> "Remote directory [" + remotePath + "] does not exist. Cannot list resources.");
					return new SmbFile[0];
				}
				else if (!smbDir.isDirectory()) {
					throw new IOException("[" + remotePath + "] is not a directory. Cannot list resources.");
				}

				files = smbDir.listFiles();
			}
			catch (SmbException _ex) {
				throw new IOException("Failed to list in [" + remotePath + "].", _ex);
			}
		}

		logListedFiles(remotePath, files);

		return files;
	}

	/**
	 * Return the contents of the specified SMB resource as an array of SmbFile filenames.
	 * In case the remote resource does not exist, an empty array is returned.
	 * @param _path path to a remote directory
	 * @return array of SmbFile filenames
	 * @throws IOException on error conditions returned by a CIFS server or if the remote resource is not a directory.
	 */
	@Override
	public String[] listNames(String _path) throws IOException {
		try {
			SmbFile smbDir = createSmbDirectoryObject(_path);
			if (!smbDir.exists()) {
				logger.warn(() -> "Remote directory [" + _path + "] does not exist. Cannot list resources.");
				return new String[0];
			}
			else if (!smbDir.isDirectory()) {
				throw new IOException("[" + _path + "] is not a directory. Cannot list resources.");
			}

			String[] fileNames = smbDir.list();

			logListedFiles(_path, fileNames);

			return fileNames;
		}
		catch (SmbException _ex) {
			throw new IOException("Failed to list resources in [" + _path + "].", _ex);
		}
	}

	private static void logListedFiles(String _path, Object[] files) {
		if (logger.isDebugEnabled()) {
			logger.debug("Successfully listed " + files.length + " in [" + _path + "]: " + Arrays.toString(files));
		}
		else {
			logger.info(() -> "Successfully listed " + files.length + " in [" + _path + "].");
		}
	}

	/**
	 * Read the remote resource specified by path and copies its contents to the specified
	 * {@link OutputStream}.
	 * @param _path path to a remote file
	 * @param _outputStream output stream
	 * @throws IOException on error conditions returned by a CIFS server or if the remote resource is not a file.
	 */
	@Override
	public void read(String _path, OutputStream _outputStream) throws IOException {
		Assert.hasText(_path, "path must not be empty");
		Assert.notNull(_outputStream, "outputStream must not be null");

		try {
			SmbFile remoteFile = createSmbFileObject(_path);
			if (!remoteFile.isFile()) {
				throw new IOException("[" + _path + "] is not a file.");
			}
			FileCopyUtils.copy(remoteFile.getInputStream(), _outputStream);
		}
		catch (SmbException _ex) {
			throw new IOException("Failed to read resource [" + _path + "].", _ex);
		}

		logger.info(() -> "Successfully read resource [" + _path + "].");
	}

	/**
	 * Write contents of the specified {@link InputStream} to the remote resource
	 * specified by path. Remote directories are created implicitly as required.
	 * @param _inputStream input stream
	 * @param _path remote path (of a file) to write to
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	@Override
	public void write(InputStream _inputStream, String _path) throws IOException {
		Assert.notNull(_inputStream, "inputStream must not be empty");
		Assert.hasText(_path, "path must not be null");

		try {
			mkdirs(_path);
			SmbFile targetFile = createSmbFileObject(_path);
			FileCopyUtils.copy(_inputStream, targetFile.getOutputStream());
		}
		catch (SmbException _ex) {
			throw new IOException("Failed to write resource [" + _path + "].", _ex);
		}
		logger.info(() -> "Successfully wrote remote file [" + _path + "].");
	}

	/**
	 * Convenience method to write a local file object to a remote location.
	 * @param _file the local file
	 * @param _path the remote path to write to
	 * @return the {@link SmbFile} for remote file
	 * @throws IOException the IO exception
	 */
	public SmbFile write(File _file, String _path) throws IOException {
		return writeAndClose(new FileInputStream(_file), _path);
	}

	/**
	 * Convenience method to write a byte array to a remote location.
	 * @param _contents the {@code byte[]} to write
	 * @param _path the remote file to write to
	 * @return the {@link SmbFile} for remote file
	 * @throws IOException the IO exception
	 */
	public SmbFile write(byte[] _contents, String _path) throws IOException {
		return writeAndClose(new ByteArrayInputStream(_contents), _path);
	}

	/**
	 * Create the specified remote path if not yet exists.
	 * If the specified resource is a file rather than a path, creates all directories leading
	 * to that file.
	 * @param _path remote path to create
	 * @return always true (error states are express by exceptions)
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	@Override
	public boolean mkdir(String _path) throws IOException {
		try {
			SmbFile dir = createSmbDirectoryObject(_path);
			if (!dir.exists()) {
				dir.mkdirs();
				logger.info(() ->
						"Successfully created remote directory [" + _path + "] in share [" + this.smbShare + "].");
			}
			else {
				logger.info(() -> "Remote directory [" + _path + "] exists in share [" + this.smbShare + "].");
			}
			return true;
		}
		catch (SmbException _ex) {
			throw new IOException("Failed to create directory [" + _path + "].", _ex);
		}
	}

	/**
	 * Check whether the remote resource exists.
	 * @param _path remote path
	 * @return true if exists, false otherwise
	 * @throws IOException  on error conditions returned by a CIFS server
	 */
	@Override
	public boolean exists(String _path) throws IOException {
		return createSmbFileObject(_path).exists();
	}

	/**
	 * Check whether the remote resource is a file.
	 * @param _path remote path
	 * @return true if resource is a file, false otherwise
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	public boolean isFile(String _path) throws IOException {
		SmbFile resource = createSmbFileObject(_path);
		return resource.exists() && resource.isFile();
	}

	/**
	 * Check whether the remote resource is a directory.
	 * @param _path remote path
	 * @return true if resource is a directory, false otherwise
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	public boolean isDirectory(String _path) throws IOException {
		SmbFile resource = createSmbFileObject(_path);
		return resource.exists() && resource.isDirectory();
	}

	/**
	 * Create all directories in the given remote path reference.
	 * @param _path path remote path, may be a file in which case the file name is ignored
	 * @return the path created or null
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	String mkdirs(String _path) throws IOException {
		int idxPath = _path.lastIndexOf(SMB_FILE_SEPARATOR);
		if (idxPath > -1) {
			String path = _path.substring(0, idxPath + 1);
			mkdir(path);
			return path;
		}
		return null;
	}

	@Override
	public void rename(String _pathFrom, String _pathTo) throws IOException {
		try {

			SmbFile smbFileFrom = createSmbFileObject(_pathFrom);
			SmbFile smbFileTo = createSmbFileObject(_pathTo);
			if (smbFileTo.exists()) {
				smbFileTo.delete();
			}
			smbFileFrom.renameTo(smbFileTo);

		}
		catch (SmbException _ex) {
			throw new IOException("Failed to rename [" + _pathFrom + "] to [" + _pathTo + "].", _ex);
		}
		logger.info(() -> "Successfully renamed remote resource [" + _pathFrom + "] to [" + _pathTo + "].");
	}

	@Override
	public void append(InputStream inputStream, String destination) throws IOException {
		SmbFile smbFile = createSmbFileObject(destination);
		OutputStream fileOutputStream = new SmbFileOutputStream(smbFile, true);
		FileCopyUtils.copy(inputStream, fileOutputStream);
	}

	@Override
	public boolean rmdir(String directory) throws IOException {
		SmbFile dir = createSmbDirectoryObject(directory);
		try {
			dir.delete();
		}
		catch (SmbException ex) {
			logger.info(() -> "Failed to remove remote directory [" + directory + "]: " + ex);
			return false;
		}
		logger.info(() -> "Successfully removed remote directory [" + directory + "].");
		return true;
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		SmbFile remoteFile = createSmbFileObject(source);
		if (!remoteFile.isFile()) {
			throw new IOException("[" + source + "] is not a file.");
		}
		return remoteFile.getInputStream();
	}

	@Override
	public boolean finalizeRaw() {
		return true;
	}

	@Override
	public Object getClientInstance() {
		return this.smbShare;
	}

	@Override
	public void close() {
		this.smbShare.close();
	}

	/**
	 * Check whether this SMB session is open and ready for work by attempting
	 * to list remote files and checking for error conditions.
	 * @return true if the session is open, false otherwise
	 */
	@Override
	public boolean isOpen() {
		if (!this.smbShare.isOpened()) {
			return false;
		}
		try {
			this.smbShare.listFiles();
		}
		catch (Exception _ex) {
			close();
		}
		return this.smbShare.isOpened();
	}

	/**
	 * Convenience method to write the specified input stream to a remote path and return
	 * the path as an SMB file object.
	 * @param _inputStream input stream
	 * @param _path remote path (of a file) to write to
	 * @return SMB file object
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	SmbFile writeAndClose(InputStream _inputStream, String _path) throws IOException {
		write(_inputStream, _path);
		_inputStream.close();
		return createSmbFileObject(_path);
	}

	/**
	 * Factory method for new SmbFile objects under this session's share for the specified path.
	 * @param path remote path
	 * @param isDirectory Boolean object to indicate the path is a directory, may be null
	 * @return SmbFile object for path
	 * @throws IOException in case of I/O errors
	 */
	private SmbFile createSmbFileObject(String path, Boolean isDirectory) throws IOException {

		final String cleanedPath = StringUtils.cleanPath(path);

		if (!StringUtils.hasText(cleanedPath)) {
			return this.smbShare;
		}

		SmbFile smbFile = new SmbFile(this.smbShare, cleanedPath);

		boolean appendFileSeparator = !cleanedPath.endsWith(SMB_FILE_SEPARATOR);
		if (appendFileSeparator) {
			try {
				appendFileSeparator = smbFile.isDirectory() || (isDirectory != null && isDirectory);
			}
			catch (SmbException ex) {
				appendFileSeparator = false;
			}
		}
		if (appendFileSeparator) {
			smbFile = createSmbFileObject(cleanedPath + SMB_FILE_SEPARATOR);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Created new " + SmbFile.class.getName() + "[" + smbFile + "] for path [" + path + "].");
		}
		return smbFile;
	}

	/**
	 * Create an SMB file object pointing to a remote file.
	 * @param _path the remote file path
	 * @return the {@link SmbFile} for remote path
	 * @throws IOException the IO exception
	 */
	public SmbFile createSmbFileObject(String _path) throws IOException {
		return createSmbFileObject(_path, null);
	}

	/**
	 * Create an SMB file object pointing to a remote directory.
	 * @param _path the remote directory path
	 * @return the {@link SmbFile} for remote path
	 * @throws IOException the IO exception
	 */
	public SmbFile createSmbDirectoryObject(String _path) throws IOException {
		return createSmbFileObject(_path, true);
	}

	@Override
	public String getHostPort() {
		URL url = this.smbShare.getURL();
		return url.getHost() + ":" + url.getPort();
	}

}
