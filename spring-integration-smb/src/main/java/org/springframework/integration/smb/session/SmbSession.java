/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.session;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NestedIOException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link Session} interface for Server Message Block (SMB) 
 * also known as Common Internet File System (CIFS). The Samba project set out to  
 * create non-Windows implementations of SMB. Often Samba is thus used synonymously to SMB.
 * 
 * SMB is an application-layer network protocol that manages shared access to files, printers 
 * and other networked resources.
 *
 * See <a href="http://en.wikipedia.org/wiki/Server_Message_Block">Server Message Block</a> 
 * for more details.
 *
 * Inspired by the sprint-integration-ftp implementation done by Mark Fisher and Oleg Zhurakousky.   
 *
 * @author Markus Spann
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 *
 * @since 2.1.1
 */
public class SmbSession implements Session<SmbFile> {

	private final Log           logger         = LogFactory.getLog(SmbSession.class);
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	static {
		configureJcifs();
	}

	private final SmbShare smbShare;

	/**
	 * Constructor for an SMB session.
	 * @param _host server NetBIOS name, DNS name, or IP address, case-insensitive
	 * @param _port port
	 * @param _domain case-sensitive domain name
	 * @param _user case-sensitive user name
	 * @param _password case-sensitive password
	 * @param _shareAndDir server root SMB directory
	 * @param _replaceFile replace existing files if true
	 * @param _useTempFile make use temporary files when writing
	 * @throws IOException in case of I/O errors 
	 */
	SmbSession(String _host, int _port, String _domain, String _user, String _password, String _shareAndDir, boolean _replaceFile, boolean _useTempFile) throws IOException {
		this(new SmbShare(new SmbConfig(_host, _port, _domain, _user, _password, _shareAndDir)));

		smbShare.setReplaceFile(_replaceFile);
		smbShare.setUseTempFile(_useTempFile);
	}

	/**
	 * Constructor for an SMB session.
	 * @param _smbShare SMB share resource
	 */
	public SmbSession(SmbShare _smbShare) {
		Assert.notNull(_smbShare, "smbShare must not be null");
		smbShare = _smbShare;
		logger.debug("New " + getClass().getName() + " created.");
	}

	/**
	 * Deletes the file or directory at the specified path.
	 * @param _path path to a remote file or directory
	 * @return true if delete successful, false if resource is non-existent
	 * @throws IOException on error conditions returned by a CIFS server
	 * @see org.springframework.integration.file.remote.session.Session#remove(java.lang.String)
	 */
	public boolean remove(String _path) throws IOException {
		Assert.hasText(_path, "path must not be empty");

		boolean removed = false;
		SmbFile removeFile = createSmbFileObject(_path);

		if (removeFile.exists()) {
			removeFile.delete();
			removed = true;
		}
		if (!removed) {
			logger.info("Could not remove non-existing resource [" + _path + "].");
		} else if (logger.isInfoEnabled()) {
			logger.info("Successfully removed resource [" + _path + "].");
		}
		return removed;
	}

	/**
	 * Returns the contents of the specified SMB resource as an array of SmbFile objects.
	 * In case the remote resource does not exist, an empty array is returned.
	 * @param _path path to a remote directory
	 * @return array of SmbFile objects
	 * @throws IOException on error conditions returned by a CIFS server or if the remote resource is not a directory.
	 * @see org.springframework.integration.file.remote.session.Session#list(java.lang.String)
	 */
	public SmbFile[] list(String _path) throws IOException {
		SmbFile[] files = new SmbFile[0];
		try {
			SmbFile smbDir = createSmbDirectoryObject(_path);
			if (!smbDir.exists()) {
            	logger.warn("Remote directory [" + _path + "] does not exist. Cannot list resources.");
            	return files;
			} else if (!smbDir.isDirectory()) {
				throw new NestedIOException("Resource [" + _path + "] is not a directory. Cannot list resources.");
            }

			files = smbDir.listFiles();

		} catch (SmbException _ex) {
			throw new NestedIOException("Failed to list resources in [" + _path + "].", _ex);
		}
		String msg = "Successfully listed " + files.length + " resource(s) in [" + _path + "]";
		if (logger.isDebugEnabled()) {
			logger.debug(msg + ": " + Arrays.toString(files));
		} else {
			logger.info(msg + ".");
		}

		return files;
	}

	/**
	 * Reads the remote resource specified by path and copies its contents to the specified
	 * {@link OutputStream}.
	 * @param _path path to a remote file
	 * @param _outputStream output stream
	 * @throws IOException on error conditions returned by a CIFS server or if the remote resource is not a file.
	 * @see org.springframework.integration.file.remote.session.Session#read(java.lang.String, java.io.OutputStream)
	 */
	public void read(String _path, OutputStream _outputStream) throws IOException {
		Assert.hasText(_path, "path must not be empty");
		Assert.notNull(_outputStream, "outputStream must not be null");

		try {

			SmbFile remoteFile = createSmbFileObject(_path);
			if (!remoteFile.isFile()) {
				throw new NestedIOException("Resource [" + _path + "] is not a file.");
			}
			FileCopyUtils.copy(remoteFile.getInputStream(), _outputStream);

		} catch (SmbException _ex) {
			throw new NestedIOException("Failed to read resource [" + _path + "].", _ex);
		}
		logger.info("Successfully read resource [" + _path + "].");
	}

	/**
	 * Writes contents of the specified {@link InputStream} to the remote resource 
	 * specified by path. Remote directories are created implicitely as required. 
	 * @param _inputStream input stream
	 * @param _path remote path (of a file) to write to
	 * @throws IOException on error conditions returned by a CIFS server
	 * @see org.springframework.integration.file.remote.session.Session#write(java.io.InputStream, java.lang.String)
	 */
	public void write(InputStream _inputStream, String _path) throws IOException {
		Assert.notNull(_inputStream, "inputStream must not be empty");
		Assert.hasText(_path, "path must not be null");

		try {

			mkdirs(_path);

			SmbFile targetFile = createSmbFileObject(_path);

			if (smbShare.isUseTempFile()) {

				String tempFileName = _path + smbShare.newTempFileSuffix();
				SmbFile tempFile = createSmbFileObject(tempFileName);
				tempFile.createNewFile();
				Assert.isTrue(tempFile.canWrite(), "Temporary file [" + tempFileName + "] is not writable.");

				FileCopyUtils.copy(_inputStream, tempFile.getOutputStream());

				if (targetFile.exists() && smbShare.isReplaceFile()) {
					targetFile.delete();
				}
				tempFile.renameTo(targetFile);

			} else {

				FileCopyUtils.copy(_inputStream, targetFile.getOutputStream());

			}

		} catch (SmbException _ex) {
			throw new NestedIOException("Failed to write resource [" + _path + "].", _ex);
		}
		logger.info("Successfully wrote remote file [" + _path + "].");
	}

	/**
	 * Convenience method to write a local file object to a remote location.
	 * @see org.springframework.integration.smb.session.SmbSession.write(InputStream, String)
	 */
	public SmbFile write(File _file, String _path) throws IOException {
		return writeAndClose(new FileInputStream(_file), _path);
	}

	/**
	 * Convenience method to write a byte array to a remote location.
	 * @see org.springframework.integration.smb.session.SmbSession.write(InputStream, String)
	 */
	public SmbFile write(byte[] _contents, String _path) throws IOException {
		return writeAndClose(new ByteArrayInputStream(_contents), _path);
	}

	/**
	 * Creates the specified remote path if not yet exists.
	 * If the specified resource is a file rather than a path, creates all directories leading
	 * to that file. 
	 * @param _path remote path to create
	 * @return always true (error states are express by exceptions)
	 * @throws IOException on error conditions returned by a CIFS server
	 * @see org.springframework.integration.file.remote.session.Session#mkdir(java.lang.String)
	 */
	public boolean mkdir(String _path) throws IOException {
		try {
			SmbFile dir = createSmbDirectoryObject(_path);
			if (!dir.exists()) {
				dir.mkdirs();
				logger.info("Successfully created remote directory [" + _path + "] in share [" + smbShare + "].");
			} else {
				logger.info("Remote directory [" + _path + "] exists in share [" + smbShare + "].");
			}
			return true;
		} catch (SmbException _ex) {
			throw new NestedIOException("Failed to create directory [" + _path + "].", _ex);
		}
	}

	/**
	 * Checks whether the remote resource exists.
	 * @param _path remote path
	 * @return true if exists, false otherwise
	 * @throws IOException  on error conditions returned by a CIFS server
	 * @see org.springframework.integration.file.remote.session.Session#exists(java.lang.String)
	 */
	public boolean exists(String _path) throws IOException {
		return createSmbFileObject(_path).exists();
	}

	/**
	 * Checks whether the remote resource is a file.
	 * @param _path remote path
	 * @return true if resource is a file, false otherwise
	 * @throws IOException on error conditions returned by a CIFS server
	 */
	public boolean isFile(String _path) throws IOException {
		SmbFile resource = createSmbFileObject(_path);
		return resource.exists() && resource.isFile();
	}

	/**
	 * Checks whether the remote resource is a directory.
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
		int idxPath = _path.lastIndexOf(FILE_SEPARATOR);
		if (idxPath > -1) {
			String path = _path.substring(0, idxPath + 1);
			mkdir(path);
			return path;
		}
		return null;
	}

	/**
	 * Renames a remote resource.
	 * @param _pathFrom remote source path
	 * @param _pathTo remote target path
	 * @throws IOException on error conditions returned by a CIFS server
	 * @see org.springframework.integration.file.remote.session.Session#rename(java.lang.String, java.lang.String)
	 */
	public void rename(String _pathFrom, String _pathTo) throws IOException {
		try {

			SmbFile smbFileFrom = createSmbFileObject(_pathFrom);
			SmbFile smbFileTo = createSmbFileObject(_pathTo);
			if (smbShare.isReplaceFile() && smbFileTo.exists()) {
				smbFileTo.delete();
			}
			smbFileFrom.renameTo(smbFileTo);

		} catch (SmbException _ex) {
			throw new NestedIOException("Failed to rename [" + _pathFrom + "] to [" + _pathTo + "].", _ex);
		}
		logger.info("Successfully renamed remote resource [" + _pathFrom + "] to [" + _pathTo + "].");

	}

	/**
	 * Closes this SMB session.
	 * @see org.springframework.integration.file.remote.session.Session#close()
	 */
	public void close() {
		smbShare.doClose();
	}

	/**
	 * Checks with this SMB session is open and ready for work by attempting
	 * to list remote files and checking for error conditions..
	 * @return true if the session is open, false otherwise
	 * @see org.springframework.integration.file.remote.session.Session#isOpen()
	 */
	public boolean isOpen() {
		if (!smbShare.isOpened()) {
			return false;
		}
		try {
			smbShare.listFiles();
		} catch (Exception _ex) {
			close();
		}
		return smbShare.isOpened();
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
     * @param _path remote path
     * @param _isDirectory Boolean object to indicate the path is a directory, may be null
     * @return SmbFile object for path
     * @throws IOException in case of I/O errors 
     */
    private SmbFile createSmbFileObject(String _path, Boolean _isDirectory) throws IOException {
		String path = StringUtils.cleanPath(_path);
		if (!StringUtils.hasText(path)) {
			return smbShare;
		}

    	SmbFile smbFile = new SmbFile(smbShare, path);
    
    	boolean appendFileSeparator = !path.endsWith(FILE_SEPARATOR);
    	if (appendFileSeparator) {
    		try {
    			appendFileSeparator = smbFile.isDirectory() || (_isDirectory != null && _isDirectory);
    		} catch (Exception _ex) {
    			appendFileSeparator = false;
    		}
    	}
    	if (appendFileSeparator) {
    		smbFile = createSmbFileObject(path + FILE_SEPARATOR);
    	}
    	if (logger.isDebugEnabled()) {
    		logger.debug("Created new " + SmbFile.class.getName() + "[" + smbFile + "] for path [" + path + "].");
    	}
    	return smbFile;
    }

	/**
	 * Creates an SMB file object pointing to a remote file.
	 */
	public SmbFile createSmbFileObject(String _path) throws IOException {
    	return createSmbFileObject(_path, null);
    }

	/**
	 * Creates an SMB file object pointing to a remote directory.
	 */
	public SmbFile createSmbDirectoryObject(String _path) throws IOException {
    	return createSmbFileObject(_path, true);
    }

	/**
	 * Static configuration of the JCIFS library.
	 * The log level of this class is mapped to a suitable <code>jcifs.util.loglevel</code>
	 */
	static void configureJcifs() {
        // TODO jcifs.Config.setProperty("jcifs.smb.client.useExtendedSecurity", "false");
    	// TODO jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");
    
    	// set JCIFS SMB client library' log level unless already configured by system property
    	final String sysPropLogLevel = "jcifs.util.loglevel";
    
    	if (jcifs.Config.getProperty(sysPropLogLevel) == null) {
    		// set log level according to this class' logger's log level.
    		Log log = LogFactory.getLog(SmbSession.class);
    		if (log.isTraceEnabled()) {
    			jcifs.Config.setProperty(sysPropLogLevel, "N");
    		} else if (log.isDebugEnabled()) {
    			jcifs.Config.setProperty(sysPropLogLevel, "3");
    		} else {
    			jcifs.Config.setProperty(sysPropLogLevel, "1");
    		}
    	}
    }

}
