/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.multipart;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

/**
 * {@link MultipartFileReader} implementation that reads the {@link MultipartFile}
 * content directly into a new {@link MultipartFile} instance that is not restricted
 * to the HTTP request scope.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class DefaultMultipartFileReader implements MultipartFileReader<MultipartFile> {

	public MultipartFile readMultipartFile(MultipartFile multipartFile) throws IOException {
		return new UploadedMultipartFile(multipartFile.getBytes(),
				multipartFile.getContentType(), multipartFile.getName(), multipartFile.getOriginalFilename());
	}

}
