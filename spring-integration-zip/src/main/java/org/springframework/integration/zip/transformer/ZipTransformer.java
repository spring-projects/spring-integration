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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;

import org.apache.commons.io.FilenameUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;

import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.zip.ZipHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Transformer} implementation that applies a Zip transformation to the message payload.
 * Keep in mind that Zip entry timestamps are recorded only to 2 second precision:
 * <p>
 * See also: <a href="https://www.mindprod.com/jgloss/zip.html"/>
 * <p>
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class ZipTransformer extends AbstractZipTransformer {

	private static final String ZIP_EXTENSION = ".zip";

	private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	private boolean useFileAttributes = true;

	private FileNameGenerator fileNameGenerator;

	/**
	 * Set the compression level. Default is {@link Deflater#DEFAULT_COMPRESSION}.
	 * @param compressionLevel Must be an integer value from 0-9.
	 */
	public void setCompressionLevel(int compressionLevel) {
		Assert.isTrue(compressionLevel >= 0 && compressionLevel <= 9, "Acceptable levels are 0-9");
		this.compressionLevel = compressionLevel;
	}

	/**
	 * Specify whether the name of the file shall be used for the zip entry.
	 * @param useFileAttributes Defaults to true if not set explicitly
	 */
	public void setUseFileAttributes(boolean useFileAttributes) {
		this.useFileAttributes = useFileAttributes;
	}

	/**
	 * Set a {@link FileNameGenerator} for zip file base name: the {@code .zip} extension is added to this name.
	 * Unless it already comes with {@code .zip} extension.
	 * Defaults to the {@link org.springframework.integration.file.DefaultFileNameGenerator}.
	 * The result of this generator is also used for zip entry(ies) if {@link ZipHeaders#ZIP_ENTRY_FILE_NAME}
	 * header is not provided in the request message.
	 * @param fileNameGenerator the {@link FileNameGenerator} to use.
	 * @since 6.4
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "'fileNameGenerator' must not be null");
		this.fileNameGenerator = fileNameGenerator;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.fileNameGenerator == null) {
			DefaultFileNameGenerator defaultFileNameGenerator = new DefaultFileNameGenerator();
			defaultFileNameGenerator.setBeanFactory(getBeanFactory());
			defaultFileNameGenerator.setConversionService(getConversionService());
			this.fileNameGenerator = defaultFileNameGenerator;
		}
	}

	/**
	 * The payload may encompass the following types:
	 * <ul>
	 *   <li>{@link File}
	 *...<li>{@link String}
	 *...<li>byte[]
	 *...<li>{@link Iterable}
	 * </ul>
	 * When providing an {@link Iterable}, nested Iterables are not supported. However,
	 * payloads can be any of the other supported types.
	 */
	@Override
	protected Object doZipTransform(Message<?> message) {
		Object payload = message.getPayload();
		MessageHeaders messageHeaders = message.getHeaders();

		String baseFileName = this.fileNameGenerator.generateFileName(message);
		String zipFileName = baseFileName.endsWith(ZIP_EXTENSION) ? baseFileName : baseFileName + ZIP_EXTENSION;

		String zipEntryName = (String) messageHeaders.getOrDefault(ZipHeaders.ZIP_ENTRY_FILE_NAME, baseFileName);
		Date lastModifiedDate = (Date) messageHeaders.getOrDefault(ZipHeaders.ZIP_ENTRY_LAST_MODIFIED_DATE, new Date());

		List<ZipEntrySource> entries = createZipEntries(payload, zipEntryName, lastModifiedDate);

		byte[] zippedBytes = SpringZipUtils.pack(entries, this.compressionLevel);

		Object zippedData;
		if (ZipResultType.FILE.equals(this.zipResultType)) {
			final File zippedFile = new File(this.workDirectory, zipFileName);
			try {
				FileCopyUtils.copy(zippedBytes, zippedFile);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			zippedData = zippedFile;
		}
		else if (ZipResultType.BYTE_ARRAY.equals(this.zipResultType)) {
			zippedData = zippedBytes;
		}
		else {
			throw new IllegalStateException("Unsupported zipResultType " + this.zipResultType);
		}

		deleteFilesIfAny(payload);

		return getMessageBuilderFactory()
				.withPayload(zippedData)
				.copyHeaders(messageHeaders)
				.setHeader(FileHeaders.FILENAME, zipFileName)
				.build();
	}

	private List<ZipEntrySource> createZipEntries(Object payload, String zipEntryName, Date lastModifiedDate) {
		List<ZipEntrySource> entries = new ArrayList<>();

		if (payload instanceof Iterable<?>) {
			int counter = 1;

			String baseName = FilenameUtils.getBaseName(zipEntryName);
			String fileExtension = FilenameUtils.getExtension(zipEntryName);

			if (StringUtils.hasText(fileExtension)) {
				fileExtension = FilenameUtils.EXTENSION_SEPARATOR_STR + fileExtension;
			}

			for (Object item : (Iterable<?>) payload) {

				final ZipEntrySource zipEntrySource =
						createZipEntrySource(item, lastModifiedDate, baseName + "_" + counter + fileExtension,
								this.useFileAttributes);
				logger.debug(() -> "ZipEntrySource path: '" + zipEntrySource.getPath() + "'");
				entries.add(zipEntrySource);
				counter++;
			}
		}
		else {
			final ZipEntrySource zipEntrySource =
					createZipEntrySource(payload, lastModifiedDate, zipEntryName, this.useFileAttributes);
			entries.add(zipEntrySource);
		}

		return entries;
	}

	private void deleteFilesIfAny(Object payload) {
		if (this.deleteFiles) {
			if (payload instanceof Iterable<?>) {
				for (Object item : (Iterable<?>) payload) {
					deleteFile(item);
				}
			}
			else {
				deleteFile(payload);
			}
		}
	}

	private void deleteFile(Object fileToDelete) {
		if (fileToDelete instanceof File && !((File) fileToDelete).delete()) {
			logger.warn(() -> "Failed to delete File '" + fileToDelete + "'");
		}
	}

	private ZipEntrySource createZipEntrySource(Object item,
			Date lastModifiedDate, String zipEntryName, boolean useFileAttributes) {

		if (item instanceof final File filePayload) {
			String fileName = useFileAttributes ? filePayload.getName() : zipEntryName;

			if (((File) item).isDirectory()) {
				throw new UnsupportedOperationException("Zipping of directories is not supported.");
			}

			return new FileSource(fileName, filePayload);

		}
		else if (item instanceof byte[] || item instanceof String) {
			byte[] bytesToCompress;

			if (item instanceof String) {
				bytesToCompress = ((String) item).getBytes(this.charset);
			}
			else {
				bytesToCompress = (byte[]) item;
			}

			return new ByteSource(zipEntryName, bytesToCompress, lastModifiedDate.getTime());
		}
		else {
			throw new IllegalArgumentException("Unsupported payload type. The only supported payloads are " +
					"java.io.File, java.lang.String, and byte[]");
		}
	}

}
