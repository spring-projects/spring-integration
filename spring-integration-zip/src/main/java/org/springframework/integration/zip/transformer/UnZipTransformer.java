/*
 * Copyright 2015-present the original author or authors.
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

/**
 * Transformer implementation that applies an UnZip transformation to the message
 * payload.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Ingo Dueppe
 * @author Ngoc Nhan
 * @author Glenn Renfro
 *
 * @since 6.1
 */
public class UnZipTransformer extends AbstractZipTransformer {

	/**
	 * Chunk size used when streaming zip entry contents so that boundary checks
	 * (max size / compression ratio) are enforced incrementally rather than
	 * after reading an entire entry into memory. Matches the JDK's own
	 * {@link java.io.BufferedInputStream} default buffer size.
	 */
	private static final int BUFFER_SIZE = 8 * 1024;

	private boolean expectSingleResult = false;

	private long maxUncompressedSize = 1024 * 1024 * 100;

	private int maxEntryCount = 1000;

	private double maxCompressionRatio = 100;

	/**
	 * Set the maximum allowed bytes for the cumulative total of all entries.
	 * @param maxUncompressedSize the maximum uncompressed size.  Default is 100MB
	 * @since 6.4.13
	 */
	public void setMaxUncompressedSize(long maxUncompressedSize) {
		this.maxUncompressedSize = maxUncompressedSize;
	}

	/**
	 * Set the maximum number of zip entries allowed within a single archive.
	 * Default is 1000. Setting to {@code 0} disables the check.
	 * @param maxEntryCount the maximum entry count
	 * @since 6.4.13
	 */
	public void setMaxEntryCount(int maxEntryCount) {
		this.maxEntryCount = maxEntryCount;
	}

	/**
	 * Set the maximum allowed ratio of uncompressedSize / compressedSize.
	 * Default is 100. Setting to {@code 0} disables the check.
	 * @param maxCompressionRatio the maximum compression ratio
	 * @since 6.4.13
	 */
	public void setMaxCompressionRatio(double maxCompressionRatio) {
		this.maxCompressionRatio = maxCompressionRatio;
	}

	/**
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
	public String getComponentType() {
		return "unzip-transformer";
	}

	@Override
	protected Object doZipTransform(Message<?> message) {
		Object payload = message.getPayload();
		Object unzippedData;

		InputStream inputStream = null;

		try {
			SortedMap<String, Object> uncompressedData = new TreeMap<>();

			UUID messageId = message.getHeaders().getId();
			Assert.state(messageId != null,
					() -> "The 'MessageHeaders.ID' must be provided in request message: " + message);

			ZipEntryCallback callback = buildZipEntryCallback(message, messageId, uncompressedData);

			try {
				if (payload instanceof File filePayload) {
					if (filePayload.isDirectory()) {
						throw new MessageTransformationException(message, "Cannot unzip a directory: " +
								filePayload.getAbsolutePath());
					}

					if (!SpringZipUtils.isValid(filePayload)) {
						throw new MessageTransformationException(message,
								"Not a zip file: " + filePayload.getAbsolutePath());
					}

					ZipUtil.iterate(filePayload, callback);
				}
				else {
					if (payload instanceof InputStream castInputStream) {
						inputStream = castInputStream;
					}
					else if (payload instanceof byte[] bytes) {
						inputStream = new ByteArrayInputStream(bytes);
					}
					else {
						throw new MessageTransformationException(message,
								"Unsupported payload type '" + payload.getClass().getSimpleName() +
										"'. The only supported payload types are " +
										"java.io.File, byte[] and java.io.InputStream");
					}
					ZipUtil.iterate(inputStream, callback);
				}
			}
			catch (Exception exception) {
				if (ZipResultType.FILE.equals(zipResultType)) {
					File tempDir = new File(workDirectory, messageId.toString());
					if (tempDir.exists()) {
						FileSystemUtils.deleteRecursively(tempDir);
					}
				}
				throw exception;
			}

			if (uncompressedData.isEmpty()) {
				throw new MessageTransformationException(message, "No data unzipped from message");
			}
			else {

				if (this.expectSingleResult) {
					if (uncompressedData.size() == 1) {
						unzippedData = uncompressedData.values().iterator().next();
					}
					else {
						throw new MessagingException(message,
								String.format("The UnZip operation extracted %s "
										+ "result objects but expectSingleResult was 'true'.", uncompressedData.size()));
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
		finally {
			IOUtils.closeQuietly(inputStream);
		}
		return unzippedData;
	}

	private ZipEntryCallback buildZipEntryCallback(Message<?> message, UUID messageId,
			Map<String, Object> uncompressedData) {

		AtomicInteger entryCounter = new AtomicInteger();
		BoundaryCheck boundaryCheck = buildBoundaryCheck(message);

		return (zipEntryInputStream, zipEntry) -> {
			if (entryCounter.addAndGet(1) > UnZipTransformer.this.maxEntryCount) {
				throw new MessageTransformationException(message, "Exceeded max entry count of " +
						UnZipTransformer.this.maxEntryCount);
			}

			String zipEntryName = zipEntry.getName();
			long zipEntryTime = zipEntry.getTime();
			long zipEntryCompressedSize = zipEntry.getCompressedSize();
			String type = zipEntry.isDirectory() ? "directory" : "file";

			logger.info(LogMessage.format(
					"Unpacking Zip Entry - Name: '%s',Time: '%s', Compressed Size: '%s', Type: '%s'",
					zipEntryName, zipEntryTime, zipEntryCompressedSize, type));

			if (ZipResultType.FILE.equals(this.zipResultType)) {
				File destinationFile = checkPath(messageId, zipEntryName);

				if (zipEntry.isDirectory()) {
					destinationFile.mkdirs();
				}
				else {
					mkDirOfAncestorDirectories(destinationFile);
					copyWithBoundaryChecks(zipEntryInputStream, destinationFile, zipEntryCompressedSize,
							boundaryCheck);
					uncompressedData.put(zipEntryName, destinationFile);
				}
			}
			else if (ZipResultType.BYTE_ARRAY.equals(zipResultType)) {
				if (!zipEntry.isDirectory()) {
					checkPath(messageId, zipEntryName);
					byte[] data = readWithBoundaryChecks(zipEntryInputStream, zipEntryCompressedSize,
							boundaryCheck);
					uncompressedData.put(zipEntryName, data);
				}
			}
			else {
				throw new IllegalStateException("Unsupported zipResultType: " + zipResultType);
			}
		};
	}

	private BoundaryCheck buildBoundaryCheck(Message<?> message) {
		AtomicLong cumulativeUncompressedBytesRead = new AtomicLong(1L);

		return (numberOfBytesRead, bytesReadForEntry, compressedSize) -> {
			if (cumulativeUncompressedBytesRead.addAndGet(numberOfBytesRead) > this.maxUncompressedSize) {
				throw new MessageTransformationException(message, "Exceeded max uncompressed size");
			}

			if (compressedSize > 0 && this.maxCompressionRatio > 0) {
				double ratio = (double) bytesReadForEntry / compressedSize;
				if (ratio > this.maxCompressionRatio) {
					throw new MessageTransformationException(message, "Exceeded max compression ratio");
				}
			}
		};
	}

	private File checkPath(UUID messageId, String zipEntryName) throws IOException {
		File tempDir = new File(this.workDirectory, messageId.toString());
		tempDir.mkdirs(); //NOSONAR false positive
		File destinationFile = new File(tempDir, zipEntryName);

		/* If we see the relative traversal string, of ".." we need to make sure
		 * that the outputdir + name doesn't leave the outputdir.
		 */
		if (!destinationFile.getCanonicalPath()
				.startsWith(tempDir.getCanonicalPath() + File.separator)) {

			throw new ZipException("The file " + zipEntryName +
					" is trying to leave the target output directory of " + this.workDirectory);
		}
		return destinationFile;
	}

	private static void mkDirOfAncestorDirectories(File destinationFile) {
		File parentDirectory = destinationFile.getParentFile();
		if (parentDirectory != null) {
			parentDirectory.mkdirs();
		}
	}

	private static byte[] readWithBoundaryChecks(InputStream zipEntryInputStream, long compressedSize,
			BoundaryCheck boundaryCheck) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyWithBoundaryChecks(zipEntryInputStream, out, compressedSize, boundaryCheck);
		return out.toByteArray();
	}

	private static void copyWithBoundaryChecks(InputStream zipEntryInputStream, File destinationFile,
			long compressedSize, BoundaryCheck boundaryCheck) throws IOException {

		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destinationFile))) {
			copyWithBoundaryChecks(zipEntryInputStream, out, compressedSize, boundaryCheck);
		}
	}

	private static void copyWithBoundaryChecks(InputStream zipEntryInputStream, OutputStream out, long compressedSize,
			BoundaryCheck boundaryCheck) throws IOException {

		byte[] buffer = new byte[BUFFER_SIZE];
		long bytesReadForEntry = 0;
		int numberOfBytes;
		while ((numberOfBytes = zipEntryInputStream.read(buffer)) != -1) {
			bytesReadForEntry += numberOfBytes;
			boundaryCheck.check(numberOfBytes, bytesReadForEntry, compressedSize);
			out.write(buffer, 0, numberOfBytes);
		}
	}

	@FunctionalInterface
	private interface BoundaryCheck {

		void check(int numberOfBytesRead, long bytesReadForEntry, long compressedSize);

	}

}
