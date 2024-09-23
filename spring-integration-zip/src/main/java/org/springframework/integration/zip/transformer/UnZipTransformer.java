/*
 * Copyright 2015-2024 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Transformer implementation that applies an UnZip transformation to the message
 * payload.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Ingo Dueppe
 * @author Ngoc Nhan
 *
 * @since 6.1
 */
public class UnZipTransformer extends AbstractZipTransformer {

	private volatile boolean expectSingleResult = false;

	/**
	 *
	 * This parameter indicates that only one result object shall be returned as
	 * a result from the executed Unzip operation. If set to <code>true</code> and
	 * more than 1 element is returned, then that
	 * 1 element is extracted and returned as payload.
	 * If the result map contains more than 1 element and
	 * {@link #expectSingleResult} is <code>true</code>, then a
	 * {@link MessagingException} is thrown.
	 * If set to <code>false</code>, the complete result list is returned as the
	 * payload. This is the {@code default}.
	 * @param expectSingleResult If not set explicitly, will default to false
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	@Override
	protected Object doZipTransform(final Message<?> message) {
		Object payload = message.getPayload();
		Object unzippedData;

		InputStream inputStream = null;

		try {
			if (payload instanceof final File filePayload) {
				if (filePayload.isDirectory()) {
					throw new UnsupportedOperationException("Cannot unzip a directory: " +
							filePayload.getAbsolutePath());
				}

				if (!SpringZipUtils.isValid(filePayload)) {
					throw new IllegalStateException("Not a zip file: " + filePayload.getAbsolutePath());
				}

				inputStream = new FileInputStream(filePayload);
			}
			else if (payload instanceof InputStream castInputStream) {
				inputStream = castInputStream;
			}
			else if (payload instanceof byte[] bytes) {
				inputStream = new ByteArrayInputStream(bytes);
			}
			else {
				throw new IllegalArgumentException("Unsupported payload type '" + payload.getClass().getSimpleName()
						+ "'. The only supported payload types are java.io.File, byte[] and java.io.InputStream");
			}

			final SortedMap<String, Object> uncompressedData = new TreeMap<>();

			ZipUtil.iterate(inputStream, new ZipEntryCallback() {

				@Override
				public void process(InputStream zipEntryInputStream, ZipEntry zipEntry) throws IOException {

					final String zipEntryName = zipEntry.getName();
					final long zipEntryTime = zipEntry.getTime();
					final long zipEntryCompressedSize = zipEntry.getCompressedSize();
					final String type = zipEntry.isDirectory() ? "directory" : "file";

					logger.info(() -> String.format("Unpacking Zip Entry - Name: '%s',Time: '%s', " +
									"Compressed Size: '%s', Type: '%s'",
							zipEntryName, zipEntryTime, zipEntryCompressedSize, type));

					if (ZipResultType.FILE.equals(zipResultType)) {
						final File destinationFile = checkPath(message, zipEntryName);

						if (zipEntry.isDirectory()) {
							destinationFile.mkdirs(); //NOSONAR false positive
						}
						else {
							mkDirOfAncestorDirectories(destinationFile);
							SpringZipUtils.copy(zipEntryInputStream, destinationFile);
							uncompressedData.put(zipEntryName, destinationFile);
						}
					}
					else if (ZipResultType.BYTE_ARRAY.equals(zipResultType)) {
						if (!zipEntry.isDirectory()) {
							checkPath(message, zipEntryName);
							byte[] data = IOUtils.toByteArray(zipEntryInputStream);
							uncompressedData.put(zipEntryName, data);
						}
					}
					else {
						throw new IllegalStateException("Unsupported zipResultType: " + zipResultType);
					}
				}

				public File checkPath(final Message<?> message, final String zipEntryName) throws IOException {
					File tempDir = new File(workDirectory, message.getHeaders().getId().toString()); // NOSONAR
					tempDir.mkdirs(); //NOSONAR false positive
					final File destinationFile = new File(tempDir, zipEntryName);

					/* If we see the relative traversal string of ".." we need to make sure
					 * that the outputdir + name doesn't leave the outputdir.
					 */
					if (!destinationFile.getCanonicalPath()
							.startsWith(tempDir.getCanonicalPath() + File.separator)) {

						throw new ZipException("The file " + zipEntryName +
								" is trying to leave the target output directory of " + workDirectory);
					}
					return destinationFile;
				}
			});

			if (uncompressedData.isEmpty()) {
				logger.warn(() -> "No data unzipped from payload with message Id " + message.getHeaders().getId());
				unzippedData = null;
			}
			else {

				if (this.expectSingleResult) {
					if (uncompressedData.size() == 1) {
						unzippedData = uncompressedData.values().iterator().next();
					}
					else {
						throw new MessagingException(message,
								String.format("The UnZip operation extracted %s "
										+ "result objects but expectSingleResult was 'true'.", uncompressedData
										.size()));
					}
				}
				else {
					unzippedData = uncompressedData;
				}

			}

			IOUtils.closeQuietly(inputStream);
			if (payload instanceof File filePayload && this.deleteFiles && !filePayload.delete()) {
				logger.warn(() -> "failed to delete File '" + filePayload + "'");
			}
		}
		catch (FileNotFoundException ex) {
			throw new UncheckedIOException(ex);
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}
		return unzippedData;
	}

	private static void mkDirOfAncestorDirectories(File destinationFile) {
		File parentDirectory = destinationFile.getParentFile();
		if (parentDirectory != null) {
			parentDirectory.mkdirs(); // NOSONAR
		}
	}

}
