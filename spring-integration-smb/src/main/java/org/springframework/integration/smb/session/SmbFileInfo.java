/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.session;

import java.io.IOException;

import jcifs.internal.dtyp.ACE;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;

/**
 * An {@link AbstractFileInfo} implementation for SMB protocol.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbFileInfo extends AbstractFileInfo<SmbFile> {

	private static final Log logger = LogFactory.getLog(SmbFileInfo.class);

	private final SmbFile smbFile;

	public SmbFileInfo(SmbFile smbFile) {
		Assert.notNull(smbFile, "SmbFile must not be null");
		this.smbFile = smbFile;
	}

	@Override
	public boolean isDirectory() {
		try {
			return this.smbFile.isDirectory();
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
	public boolean isLink() {
		return false;
	}

	@Override
	public long getSize() {
		try {
			return this.smbFile.length();
		}
		catch (SmbException se) {
			logger.error("Unable to determine file size", se);
			return 0L;
		}
	}

	@Override
	public long getModified() {
		return this.smbFile.getLastModified();
	}

	@Override
	public String getFilename() {
		return this.smbFile.getName();
	}

	/**
	 * An Access Control Entry (ACE) is an element in a security descriptor such as
	 * those associated with files and directories. The Windows OS determines which
	 * users have the necessary permissions to access objects based on these entries.
	 * A readable, formatted list of security descriptor entries and associated
	 * permissions will be returned by this implementation.
	 *
	 * <pre>
	 * WNET\alice - Deny Write, Deny Modify, Direct - This folder only
	 * SYSTEM - Allow Read, Allow Write, Allow Modify, Allow Execute, Allow Delete, Inherited - This folder only
	 * WNET\alice - Allow Read, Allow Write, Allow Modify, Allow Execute, Allow Delete, Inherited - This folder only
	 * Administrators - Allow Read, Allow Write, Allow Modify, Allow Execute, Allow Delete, Inherited - This folder only
	 * </pre>
	 *
	 * @return a list of Access Control Entry (ACE) objects representing the security
	 * descriptor entry and permissions associated with this file or directory.
	 * @see jcifs.ACE
	 * @see jcifs.SID
	 */
	@Override
	public String getPermissions() {
		ACE[] aces;
		try {
			aces = this.smbFile.getSecurity(true);
		}
		catch (IOException ioe) {
			logger.error("Unable to determine security descriptor information for this SmbFile", ioe);
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (ACE ace : aces) {
			sb.append(ace.getSID().toDisplayString());
			sb.append(" - ");

			if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0) {
				sb.append(aceToAllowFlag(ace));
				sb.append("Read, ");
			}
			if ((ace.getAccessMask() & ACE.FILE_WRITE_DATA) != 0) {
				sb.append(aceToAllowFlag(ace));
				sb.append("Write, ");
			}
			if ((ace.getAccessMask() & ACE.FILE_APPEND_DATA) != 0) {
				sb.append(aceToAllowFlag(ace));
				sb.append("Modify, ");
			}
			if ((ace.getAccessMask() & ACE.FILE_EXECUTE) != 0) {
				sb.append(aceToAllowFlag(ace));
				sb.append("Execute, ");
			}
			if ((ace.getAccessMask() & ACE.FILE_DELETE) != 0
					|| (ace.getAccessMask() & ACE.DELETE) != 0) {
				sb.append(aceToAllowFlag(ace));
				sb.append("Delete, ");
			}

			sb.append(ace.isInherited() ? "Inherited - " : "Direct - ");
			sb.append(ace.getApplyToText());
			sb.append("\n");
		}
		logger.debug(this.getFilename());
		logger.debug(sb);

		return sb.toString();
	}

	@Override
	public SmbFile getFileInfo() {
		return this.smbFile;
	}

	@Override
	public String toString() {
		return "SmbFileInfo{smbFile=" + this.smbFile + '}';
	}

	private static String aceToAllowFlag(ACE ace) {
		return ace.isAllow() ? "Allow " : "Deny ";
	}

}
