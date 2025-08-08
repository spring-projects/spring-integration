/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.multipart;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.multipart.MultipartFile;

/**
 * {@link MultipartFileReader} implementation that copies the MultipartFile's
 * content to a new temporary File in the specified directory. If no directory
 * is provided, the Files will be created in the default temporary directory.
 *
 * @author Mark Fisher
 * @author Artyem Bilan
 *
 * @since 2.0
 */
public class FileCopyingMultipartFileReader implements MultipartFileReader<MultipartFile> {

	private static final Log LOGGER = LogFactory.getLog(FileCopyingMultipartFileReader.class);

	private final File directory;

	private String prefix = "si_";

	private String suffix = ".tmp";

	/**
	 * Create a {@link FileCopyingMultipartFileReader} that creates temporary
	 * Files in the default temporary directory.
	 */
	public FileCopyingMultipartFileReader() {
		this(null);
	}

	/**
	 * Create a {@link FileCopyingMultipartFileReader} that creates temporary
	 * Files in the given directory.
	 * @param directory The directory.
	 */
	public FileCopyingMultipartFileReader(File directory) {
		this.directory = directory;
	}

	/**
	 * Specify the prefix to use for temporary files.
	 * @param prefix The prefix.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify the suffix to use for temporary files.
	 * @param suffix The suffix.
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	@Override
	public MultipartFile readMultipartFile(MultipartFile multipartFile) throws IOException {
		File upload = File.createTempFile(this.prefix, this.suffix, this.directory);
		multipartFile.transferTo(upload);
		UploadedMultipartFile uploadedMultipartFile = new UploadedMultipartFile(upload, multipartFile.getSize(),
				multipartFile.getContentType(), multipartFile.getName(), multipartFile.getOriginalFilename());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("copied uploaded file [" + multipartFile.getOriginalFilename() +
					"] to [" + upload.getAbsolutePath() + "]");
		}
		return uploadedMultipartFile;
	}

}
