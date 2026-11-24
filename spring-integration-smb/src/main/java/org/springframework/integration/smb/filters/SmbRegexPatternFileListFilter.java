/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.smb.filters;

import java.io.UncheckedIOException;
import java.util.regex.Pattern;

import org.codelibs.jcifs.smb.impl.SmbException;
import org.codelibs.jcifs.smb.impl.SmbFile;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SMB.
 *
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Daniel Frey
 *
 * @since 6.0
 */
public class SmbRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<SmbFile> {

	public SmbRegexPatternFileListFilter(String pattern) {
		this(Pattern.compile(pattern));
	}

	public SmbRegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	/**
	 * Gets the specified SMB file's name.
	 * @param file SMB file object
	 * @return file name
	 * @see AbstractRegexPatternFileListFilter#getFilename(java.lang.Object)
	 */
	@Override
	protected String getFilename(SmbFile file) {
		return file.getName();
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file.isDirectory();
		}
		catch (SmbException e) {
			throw new UncheckedIOException(e);
		}
	}

}
