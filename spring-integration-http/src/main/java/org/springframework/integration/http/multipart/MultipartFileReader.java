/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
