/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.zip.transformer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipException;

import org.springframework.core.log.LogAccessor;

/**
 * Once the Spring Integration Zip support matures, we need to contribute the
 * methods in this utility class back to the ZT Zip project.
 *
 * @author Gunnar Hillert
 *
 * @since 6.1
 */
public class SpringZipUtils {

	private static final LogAccessor logger = new LogAccessor(SpringZipUtils.class);

	public static byte[] pack(Collection<ZipEntrySource> entries, int compressionLevel) {
		logger.debug(() -> "Creating byte array from: " + entries);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		pack(entries, outputStream, compressionLevel);
		return outputStream.toByteArray();
	}

	public static void pack(Collection<ZipEntrySource> entries, File zip, int compressionLevel) {
		logger.debug(() -> "Creating '" + zip + "' from " + entries + ".");

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(zip);
		}
		catch (FileNotFoundException e) {
			throw new IllegalStateException(String.format("File '%s' not found.", zip.getAbsolutePath()), e);
		}
		pack(entries, outputStream, compressionLevel);

	}

	private static void pack(Collection<ZipEntrySource> entries, OutputStream outputStream, int compressionLevel) {

		ZipOutputStream out = null;
		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

		try {
			out = new ZipOutputStream(bufferedOutputStream);
			out.setLevel(compressionLevel);
			for (ZipEntrySource entry : entries) {
				addEntry(entry, out);
			}
		}
		catch (IOException e) {
			throw rethrow(e);
		}
		finally {
			IOUtils.closeQuietly(out);
		}

	}

	private static void addEntry(ZipEntrySource entry, ZipOutputStream out) throws IOException {
		out.putNextEntry(entry.getEntry());
		InputStream in = entry.getInputStream();
		if (in != null) {
			try {
				IOUtils.copy(in, out);
			}
			finally {
				IOUtils.closeQuietly(in);
			}
		}
		out.closeEntry();
	}

	private static ZipException rethrow(IOException e) {
		throw new ZipException(e);
	}

	public static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			IOUtils.copy(in, out);
		}
		finally {
			IOUtils.closeQuietly(out);
		}
	}

	public byte[] copy(InputStream in) throws IOException {
		return IOUtils.toByteArray(in);
	}

	static boolean isValid(final File file) {
		ZipFile zipfile = null;
		try {
			zipfile = new ZipFile(file);
			return true;
		}
		catch (IOException e) {
			return false;
		}
		finally {
			IOUtils.closeQuietly(zipfile);
		}
	}

}
