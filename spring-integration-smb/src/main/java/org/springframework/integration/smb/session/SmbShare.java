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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NestedIOException;
import org.springframework.util.Assert;

public class SmbShare extends SmbFile {

	private final Log           logger      = LogFactory.getLog(SmbShare.class);

	private final AtomicBoolean open        = new AtomicBoolean(false);

	private final AtomicBoolean replaceFile = new AtomicBoolean(false);

	private final AtomicBoolean useTempFile = new AtomicBoolean(false);

	public SmbShare(String _url) throws IOException {
		super(_url);
	}

	public SmbShare(SmbConfig _smbConfig) throws IOException {
		this(_smbConfig.validate().getUrl());
	}

	public void init() throws NestedIOException {
		boolean canRead = false;
		try {
			if (!exists()) {
				logger.info("SMB root directory does not exist. Creating it.");
				mkdirs();
			}
			canRead = canRead();
		} catch (SmbException _ex) {
			throw new NestedIOException("Unable to initialize share: " + this, _ex);
		}
		Assert.isTrue(canRead, "Share is not accessible " + this);
		open.set(true);
	}

	public boolean isReplaceFile() {
		return replaceFile.get();
	}

	public void setReplaceFile(boolean _replace) {
		this.replaceFile.set(_replace);
	}

	public boolean isUseTempFile() {
		return useTempFile.get();
	}

	public void setUseTempFile(boolean _useTempFile) {
		this.useTempFile.set(_useTempFile);
	}

	/**
	 * Checks whether the share is accessible.
	 * Note: jcifs.smb.SmbFile defines a package-protected method isOpen().
	 * @return true if open
	 */
	boolean isOpened() {
		return open.get();
	}

	/**
	 * Set the open state to closed.
	 * Note: jcifs.smb.SmbFile defines a package-protected method close().
	 */
	void doClose() {
		open.set(false);
	}

	public String newTempFileSuffix() {
	    return "-" + Long.toHexString(Double.doubleToLongBits(Math.random())) + ".tmp";
    }

	@Override
	public String toString() {
		return super.toString();
	}

}
