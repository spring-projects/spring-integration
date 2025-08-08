/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.smb.session;

import java.util.List;

import jcifs.smb.NtStatus;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.lang.Nullable;

/**
 * The SMB-specific {@link RemoteFileTemplate} implementation.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbRemoteFileTemplate extends RemoteFileTemplate<SmbFile> {

	protected static final List<Integer> NOT_DIRTY_STATUSES = // NOSONAR
			List.of(
					NtStatus.NT_STATUS_INVALID_HANDLE,
					NtStatus.NT_STATUS_END_OF_FILE,
					NtStatus.NT_STATUS_NO_SUCH_FILE,
					NtStatus.NT_STATUS_DUPLICATE_NAME,
					NtStatus.NT_STATUS_FILE_IS_A_DIRECTORY,
					NtStatus.NT_STATUS_NOT_A_DIRECTORY,
					NtStatus.NT_STATUS_NOT_FOUND,
					NtStatus.NT_STATUS_OBJECT_NAME_COLLISION,
					NtStatus.NT_STATUS_OBJECT_NAME_INVALID,
					NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND,
					NtStatus.NT_STATUS_OBJECT_PATH_INVALID,
					NtStatus.NT_STATUS_OBJECT_PATH_NOT_FOUND,
					NtStatus.NT_STATUS_OBJECT_PATH_SYNTAX_BAD,
					NtStatus.NT_STATUS_CANNOT_DELETE
			);

	/**
	 * Construct a {@link SmbRemoteFileTemplate} with the supplied session factory.
	 *
	 * @param sessionFactory the session factory.
	 */
	public SmbRemoteFileTemplate(SessionFactory<SmbFile> sessionFactory) {
		super(sessionFactory);
	}

	@Override
	protected boolean shouldMarkSessionAsDirty(Exception ex) {
		SmbException smbException = findSmbException(ex);
		if (smbException != null) {
			return isStatusDirty(smbException.getNtStatus());
		}
		else {
			return super.shouldMarkSessionAsDirty(ex);
		}
	}

	/**
	 * Check if {@link SmbException#getNtStatus()} is treated as fatal.
	 * @param status the value from {@link SmbException#getNtStatus()}.
	 * @return true if {@link SmbException#getNtStatus()} is treated as fatal.
	 * @since 6.0.8
	 */
	protected boolean isStatusDirty(int status) {
		return !NOT_DIRTY_STATUSES.contains(status);
	}

	@Nullable
	private static SmbException findSmbException(Throwable ex) {
		if (ex == null || ex instanceof SmbException) {
			return (SmbException) ex;
		}
		else {
			return findSmbException(ex.getCause());
		}
	}

}
