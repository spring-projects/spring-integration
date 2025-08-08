/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author Mark Fisher
 */
public class StubMultipartFile implements MultipartFile {

	private final String parameterName;

	private final String filename;

	private final byte[] bytes;

	private final String text;

	public StubMultipartFile(String parameterName, String filename, String text) {
		this.parameterName = parameterName;
		this.filename = filename;
		if (text != null) {
			this.bytes = text.getBytes();
		}
		else {
			this.bytes = null;
		}
		this.text = text;
	}

	public StubMultipartFile(String parameterName, String filename, byte[] bytes) {
		this.parameterName = parameterName;
		this.filename = filename;
		this.bytes = bytes;
		this.text = null;
	}

	public byte[] getBytes() throws IOException {
		return this.bytes;
	}

	public String getContentType() {
		return (this.text != null) ? "text" : null;
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.bytes);
	}

	public String getName() {
		return this.parameterName;
	}

	public String getOriginalFilename() {
		return this.filename;
	}

	public long getSize() {
		return this.bytes.length;
	}

	public boolean isEmpty() {
		return this.bytes == null || this.bytes.length == 0;
	}

	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileOutputStream fos = new FileOutputStream(dest);
		fos.write(this.bytes);
		fos.close();
	}

}
