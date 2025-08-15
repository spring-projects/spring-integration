/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.http.multipart;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * A {@link MultipartFile} implementation that represents an uploaded File.
 * The actual file content either exists in memory (in a byte array) or in a File.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class UploadedMultipartFile implements MultipartFile {

	private final @Nullable File file;

	private final byte @Nullable [] bytes;

	private final long size;

	private final @Nullable String contentType;

	private final String formParameterName;

	private final @Nullable String originalFilename;

	public UploadedMultipartFile(File file, long size, @Nullable String contentType, String formParameterName,
			@Nullable String originalFilename) {

		Assert.notNull(file, "file must not be null");
		Assert.hasText(contentType, "contentType is required");
		Assert.hasText(formParameterName, "formParameterName is required");
		Assert.hasText(originalFilename, "originalFilename is required");
		this.file = file;
		this.size = size;
		this.bytes = null;
		this.contentType = contentType;
		this.formParameterName = formParameterName;
		this.originalFilename = originalFilename;
	}

	public UploadedMultipartFile(byte[] bytes, @Nullable String contentType, String formParameterName, //NOSONAR - direct storage
			@Nullable String originalFilename) {

		Assert.notNull(bytes, "bytes must not be null");
		Assert.hasText(contentType, "contentType is required");
		Assert.hasText(formParameterName, "formParameterName is required");
		Assert.hasText(originalFilename, "originalFilename is required");
		this.bytes = bytes; //NOSONAR - direct storage
		this.size = bytes.length;
		this.file = null;
		this.contentType = contentType;
		this.formParameterName = formParameterName;
		this.originalFilename = originalFilename;
	}

	@Override
	public String getName() {
		return this.formParameterName;
	}

	@Override
	@SuppressWarnings("NullAway") // file is not null when bytes is
	public byte[] getBytes() throws IOException {
		if (this.bytes != null) {
			return this.bytes; //NOSONAR - direct access
		}
		return FileCopyUtils.copyToByteArray(this.file);
	}

	@Override
	public @Nullable String getContentType() {
		return this.contentType;
	}

	@Override
	@SuppressWarnings("NullAway") // file is not null when bytes is
	public InputStream getInputStream() throws IOException {
		if (this.bytes != null) {
			return new ByteArrayInputStream(this.bytes);
		}
		return new BufferedInputStream(new FileInputStream(this.file));
	}

	@Override
	public @Nullable String getOriginalFilename() {
		return this.originalFilename;
	}

	@Override
	public long getSize() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	@SuppressWarnings("NullAway") // file is not null when bytes is
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (this.bytes != null) {
			FileCopyUtils.copy(this.bytes, dest);
		}
		else {
			FileCopyUtils.copy(this.file, dest);
		}
	}

}
