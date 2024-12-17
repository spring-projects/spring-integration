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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link SmbFile} extension to  represent an SMB share directory.
 *
 * @author Markus Spann
 * @author Gregory Bragg
 * @author Adam Jones
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 6.0
 */
public class SmbShare extends SmbFile {

	private static final Log logger = LogFactory.getLog(SmbShare.class);

	private final Lock lock = new ReentrantLock();

	private final AtomicBoolean open = new AtomicBoolean(false);

	private final AtomicBoolean closeContext = new AtomicBoolean(false);

	/**
	 * Initializes the jCIFS library with default properties.
	 * @param _smbConfig the SMB share configuration
	 * @throws IOException if an invalid SMB URL was constructed by jCIFS
	 */
	public SmbShare(SmbConfig _smbConfig) throws IOException {
		super(StringUtils.cleanPath(_smbConfig.validate().rawUrl()),
				SingletonContext.getInstance().withCredentials(
						new NtlmPasswordAuthenticator(
								_smbConfig.getDomain(), _smbConfig.getUsername(), _smbConfig.getPassword())));
	}

	/**
	 * Initializes the jCIFS library with a custom client context configuration.
	 * @param _smbConfig the SMB share configuration
	 * @param _context that holds the client configuration, shared services as well as the active credentials
	 * @throws IOException if an invalid SMB URL was constructed by jCIFS
	 */
	public SmbShare(SmbConfig _smbConfig, CIFSContext _context) throws IOException {
		super(StringUtils.cleanPath(_smbConfig.validate().rawUrl()), _context);
	}

	/**
	 * Initializes the jCIFS library with custom properties such as
	 * 'jcifs.smb.client.minVersion' and 'jcifs.smb.client.maxVersion'
	 * for setting the minimum/maximum SMB supported versions.
	 * @param _smbConfig the SMB share configuration
	 * @param _props the custom property set for jCIFS to initialize
	 * @throws IOException if an invalid property was set or an invalid SMB URL was constructed by jCIFS
	 */
	public SmbShare(SmbConfig _smbConfig, Properties _props) throws IOException {
		super(StringUtils.cleanPath(_smbConfig.validate().rawUrl()),
				new BaseContext(
						new PropertyConfiguration(_props)).withCredentials(
						new NtlmPasswordAuthenticator(
								_smbConfig.getDomain(), _smbConfig.getUsername(), _smbConfig.getPassword())));

		this.closeContext.set(true);
	}

	public void init() throws IOException {
		boolean canRead;
		try {
			if (!exists()) {
				logger.info("SMB root directory does not exist. Creating it.");
				mkdirs();
			}
			canRead = canRead();
		}
		catch (SmbException _ex) {
			if (this.closeContext.get()) {
				try {
					getContext().close();
				}
				catch (CIFSException e) {
					logger.error("Unable to close share: " + this);
				}
			}
			throw new IOException("Unable to initialize share: " + this, _ex);
		}
		Assert.isTrue(canRead, "Share is not accessible " + this);
		this.open.set(true);
	}

	/**
	 * Checks whether the share is accessible.
	 * Note: jcifs.smb.SmbFile defines a package-protected method isOpen().
	 * @return true if open
	 */
	boolean isOpened() {
		return this.open.get();
	}

	@Override
	public void close() {
		this.lock.lock();
		try {
			this.open.set(false);
			if (this.closeContext.get()) {
				try {
					getContext().close();
				}
				catch (CIFSException e) {
					logger.error("Unable to close share: " + this);
				}
			}
			super.close();
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Tests to see if two {@link SmbShare} objects are equal.
	 * Relies on a super implementation.
	 * @param other another {@link SmbShare} object to compare for equality.
	 * @return equality result.
	 */
	@Override
	public boolean equals(Object other) { // NOSONAR
		return super.equals(other);
	}

	/**
	 * Return a cache code from the super class.
	 * @return A hashcode for this share
	 */
	@Override
	public int hashCode() { // NOSONAR
		return super.hashCode();
	}

}
