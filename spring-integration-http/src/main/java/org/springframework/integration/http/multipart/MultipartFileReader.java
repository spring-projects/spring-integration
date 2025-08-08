/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.multipart;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy for reading {@link MultipartFile} content.
 *
 * @param <T> the expected file content type.
 *
 * @author Mark Fisher
 *
 * @since 2.0
 */
public interface MultipartFileReader<T> {

	/**
	 * Read {@link MultipartFile} content.
	 * @param multipartFile The multipart file.
	 * @return The result of reading the file.
	 * @throws IOException Any IOException.
	 */
	T readMultipartFile(MultipartFile multipartFile) throws IOException;

}
