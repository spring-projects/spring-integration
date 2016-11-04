/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.dsl;

import java.io.File;
import java.util.Comparator;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.Message;

/**
 * The Spring Integration File components Factory.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class Files {

	public static FileInboundChannelAdapterSpec inboundAdapter(File directory) {
		return inboundAdapter(directory, null);
	}

	public static FileInboundChannelAdapterSpec inboundAdapter(File directory,
			Comparator<File> receptionOrderComparator) {
		return new FileInboundChannelAdapterSpec(receptionOrderComparator).directory(directory);
	}

	public static FileWritingMessageHandlerSpec outboundAdapter(File destinationDirectory) {
		return new FileWritingMessageHandlerSpec(destinationDirectory).expectReply(false);
	}

	public static FileWritingMessageHandlerSpec outboundAdapter(String directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(false);
	}

	/**
	 * @param directoryExpression an expression to evaluate the target directory.
	 * @return the FileWritingMessageHandlerSpec instance.
	 */
	public static FileWritingMessageHandlerSpec outboundAdapter(Expression directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(false);
	}

	public static <P> FileWritingMessageHandlerSpec outboundAdapter(Function<Message<P>, ?> directoryFunction) {
		return new FileWritingMessageHandlerSpec(directoryFunction).expectReply(false);
	}

	public static FileWritingMessageHandlerSpec outboundGateway(File destinationDirectory) {
		return new FileWritingMessageHandlerSpec(destinationDirectory).expectReply(true);
	}

	public static FileWritingMessageHandlerSpec outboundGateway(String directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(true);
	}

	/**
	 * Create a {@link FileWritingMessageHandlerSpec} based on the provided {@link Expression} for directory.
	 * @param directoryExpression an expression to evaluate the target directory.
	 * @return the FileWritingMessageHandlerSpec instance.
	 */
	public static FileWritingMessageHandlerSpec outboundGateway(Expression directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(true);
	}

	public static <P> FileWritingMessageHandlerSpec outboundGateway(Function<Message<P>, ?> directoryFunction) {
		return new FileWritingMessageHandlerSpec(directoryFunction).expectReply(true);
	}

	public static TailAdapterSpec tailAdapter(File file) {
		return new TailAdapterSpec().file(file);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with default arguments.
	 * @return the {@link FileSplitterSpec} builder.
	 */
	public static FileSplitterSpec splitter() {
		return splitter(true);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with {@code iterator} flag specified.
	 * @param iterator the {@code boolean} flag to specify the {@code iterator} mode or not.
	 * @return the {@link FileSplitterSpec} builder.
	 */
	public static FileSplitterSpec splitter(boolean iterator) {
		return splitter(iterator, false);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with {@code iterator} and {@code markers}
	 * flags specified.
	 * @param iterator the {@code boolean} flag to specify the {@code iterator} mode or not.
	 * @param markers true to emit start of file/end of file marker messages before/after the data.
	 * @return the {@link FileSplitterSpec} builder.
	 */
	public static FileSplitterSpec splitter(boolean iterator, boolean markers) {
		return new FileSplitterSpec(iterator, markers);
	}

	/**
	 * Create a {@link FileToStringTransformer} instance with default {@code charset} and no delete files afterwards.
	 * @return the {@link FileToStringTransformer}.
	 */
	public static FileToStringTransformer toStringTransformer() {
		return toStringTransformer(false);
	}

	/**
	 * Create a {@link FileToStringTransformer} instance with default {@code charset} and with delete files flag.
	 * @param deleteFiles true to delete the file.
	 * @return the {@link FileToStringTransformer}.
	 */
	public static FileToStringTransformer toStringTransformer(boolean deleteFiles) {
		return toStringTransformer(null, deleteFiles);
	}

	/**
	 * Create a {@link FileToStringTransformer} instance with provided {@code charset} and no delete files afterwards.
	 * @param charset The charset.
	 * @return the {@link FileToStringTransformer}.
	 */
	public static FileToStringTransformer toStringTransformer(String charset) {
		return toStringTransformer(charset, false);
	}

	/**
	 * Create a {@link FileToStringTransformer} instance with provided {@code charset} and delete files flag.
	 * @param charset The charset.
	 * @param deleteFiles true to delete the file.
	 * @return the {@link FileToStringTransformer}.
	 */
	public static FileToStringTransformer toStringTransformer(String charset, boolean deleteFiles) {
		FileToStringTransformer transformer = new FileToStringTransformer();
		if (charset != null) {
			transformer.setCharset(charset);
		}
		transformer.setDeleteFiles(deleteFiles);
		return transformer;
	}

	/**
	 * Create a {@link FileToByteArrayTransformer} instance.
	 * @return the {@link FileToByteArrayTransformer}.
	 */
	public static FileToByteArrayTransformer toByteArrayTransformer() {
		return toByteArrayTransformer(false);
	}

	/**
	 * Create a {@link FileToByteArrayTransformer} instance.
	 * @param deleteFiles specify whether to delete the File after transformation.
	 * Default is <em>false</em>.
	 * @return the {@link FileToByteArrayTransformer}.
	 */
	public static FileToByteArrayTransformer toByteArrayTransformer(boolean deleteFiles) {
		FileToByteArrayTransformer fileToByteArrayTransformer = new FileToByteArrayTransformer();
		fileToByteArrayTransformer.setDeleteFiles(deleteFiles);
		return fileToByteArrayTransformer;
	}

}
