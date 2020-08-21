/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
class FileOutboundChannelAdapterIntegrationTests {

	static final String SAMPLE_CONTENT = "HelloWorld";

	@Autowired
	MessageChannel inputChannelSaveToBaseDir;

	@Autowired
	MessageChannel inputChannelSaveToBaseDirDeleteSource;

	@Autowired
	MessageChannel inputChannelSaveToSubDir;

	@Autowired
	MessageChannel inputChannelSaveToSubDirWithFile;

	@Autowired
	MessageChannel inputChannelSaveToSubDirAutoCreateOff;

	@Autowired
	MessageChannel inputChannelSaveToSubDirWrongExpression;

	@Autowired
	MessageChannel inputChannelSaveToSubDirWithHeader;

	@Autowired
	MessageChannel inputChannelSaveToSubDirEmptyStringExpression;

	Message<File> message;

	File sourceFile;

	@BeforeEach
	void setUp(@TempDir File tmpDir) throws Exception {
		sourceFile = new File(tmpDir, "anyFile.txt");
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(StandardCharsets.UTF_8),
				new FileOutputStream(sourceFile, false));
		message = MessageBuilder.withPayload(sourceFile).build();
	}

	@Test
	void saveToBaseDir() {
		this.inputChannelSaveToBaseDir.send(this.message);
		assertThat(new File("target/base-directory/foo.txt").exists()).isTrue();
	}

	@Test
	void saveToBaseDirDeleteSourceFile() {
		assertThat(sourceFile.exists()).isTrue();
		this.inputChannelSaveToBaseDirDeleteSource.send(this.message);
		assertThat(new File("target/base-directory/foo.txt").exists()).isTrue();
		assertThat(sourceFile.exists()).isFalse();
	}

	@Test
	void saveToSubDir() {
		this.inputChannelSaveToSubDir.send(this.message);
		assertThat(new File("target/base-directory/sub-directory/foo.txt").exists()).isTrue();
	}

	@Test
	void saveToSubDirWithWrongExpression() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.inputChannelSaveToSubDirWrongExpression.send(this.message))
				.withStackTraceContaining(TestUtils.applySystemFileSeparator(
						"Destination path [target/base-directory/sub-directory/foo.txt] does " +
								"not point to a directory."));
	}

	@Test
	void saveToSubDirWithEmptyStringExpression() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.inputChannelSaveToSubDirEmptyStringExpression.send(this.message))
				.withStackTraceContaining("Unable to resolve Destination Directory for " +
						"the provided Expression ''   ''.");
	}

	@Test
	void saveToSubDir2() {
		Message<File> message2 = MessageBuilder.fromMessage(message)
				.setHeader("myFileLocation", "target/base-directory/headerdir")
				.build();

		this.inputChannelSaveToSubDirWithHeader.send(message2);
		assertThat(new File("target/base-directory/headerdir/foo.txt").exists()).isTrue();
	}

	@Test
	void saveToSubDirAutoCreateOff() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.inputChannelSaveToSubDirAutoCreateOff.send(this.message))
				.withStackTraceContaining(TestUtils.applySystemFileSeparator("Destination directory " +
						"[target/base-directory2/sub-directory2] does not exist."));
	}

	@Test
	void saveToSubWithFileExpression() {
		final File directory = new File("target/base-directory/sub-directory");
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", directory)
				.build();
		this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader);
		assertThat(new File("target/base-directory/sub-directory/foo.txt").exists()).isTrue();
	}

	@Test
	void saveToSubWithFileExpressionNull() {
		final File directory = null;
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", directory)
				.build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader))
				.withStackTraceContaining("The provided Destination Directory expression " +
						"(headers['subDirectory']) must not evaluate to null.");
	}

	@Test
	void saveToSubWithFileExpressionUnsupportedObjectType() {
		final Integer unsupportedObject = 1234;
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", unsupportedObject)
				.build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader))
				.withStackTraceContaining("The provided Destination Directory expression" +
						" (headers['subDirectory']) must evaluate to type " +
						"java.io.File, String or org.springframework.core.io.Resource, not java.lang.Integer.");
	}

}
