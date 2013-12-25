/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileOutboundChannelAdapterIntegrationTests {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld";

	static File workDir;

	FileWritingMessageHandler handler;

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
	@Before
	public void setUp() throws Exception {
		sourceFile = File.createTempFile("anyFile", ".txt");
		sourceFile.deleteOnExit();
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING),
				new FileOutputStream(sourceFile, false));
		message = MessageBuilder.withPayload(sourceFile).build();
	}

	@After
	public void tearDown() {
		sourceFile.delete();
	}

	@Test
	public void saveToBaseDir() throws Exception {
		this.inputChannelSaveToBaseDir.send(message);

		Assert.assertTrue(new File("target/base-directory/foo.txt").exists());

	}

	@Test
	public void saveToBaseDirDeleteSourceFile() throws Exception {
		Assert.assertTrue(sourceFile.exists());
		this.inputChannelSaveToBaseDirDeleteSource.send(message);
		Assert.assertTrue(new File("target/base-directory/foo.txt").exists());
		Assert.assertFalse(sourceFile.exists());
	}

	@Test
	public void saveToSubDir() throws Exception {
		this.inputChannelSaveToSubDir.send(message);
		Assert.assertTrue(new File("target/base-directory/sub-directory/foo.txt").exists());
	}

	@Test
	public void saveToSubDirWithWrongExpression() throws Exception {

		try {
			this.inputChannelSaveToSubDirWrongExpression.send(message);
		} catch (MessageHandlingException e) {
			Assert.assertEquals(
					TestUtils.applySystemFileSeparator("Destination path [target/base-directory/sub-directory/foo.txt] does not point to a directory."),
					e.getCause().getMessage());
			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown");
	}

	@Test
	public void saveToSubDirWithEmptyStringExpression() throws Exception {

		try {
			this.inputChannelSaveToSubDirEmptyStringExpression.send(message);
		} catch (MessageHandlingException e) {
			Assert.assertEquals("Unable to resolve destination directory name for the provided Expression ''   ''.", e.getCause().getMessage());
			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown");
	}

	@Test
	public void saveToSubDir2() throws Exception {

		final Message<File> message2 = MessageBuilder.fromMessage(message)
													.setHeader("myFileLocation", "target/base-directory/headerdir")
													.build();

		this.inputChannelSaveToSubDirWithHeader.send(message2);
		Assert.assertTrue(new File("target/base-directory/headerdir/foo.txt").exists());
	}

	@Test
	public void saveToSubDirAutoCreateOff() throws Exception {

		try {
			this.inputChannelSaveToSubDirAutoCreateOff.send(message);
		} catch (MessageHandlingException e) {
			Assert.assertEquals(
					TestUtils.applySystemFileSeparator("Destination directory [target/base-directory2/sub-directory2] does not exist."),
					e.getCause().getMessage());
			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown");
	}

	@Test
	public void saveToSubWithFileExpression() throws Exception {

		final File directory = new File("target/base-directory/sub-directory");
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", directory)
				.build();
		this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader);
		Assert.assertTrue(new File("target/base-directory/sub-directory/foo.txt").exists());
	}

	@Test
	public void saveToSubWithFileExpressionNull() throws Exception {

		final File directory = null;
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", directory)
				.build();

		try {
			this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader);
		} catch (MessageHandlingException e) {
			Assert.assertEquals("The provided destinationDirectoryExpression " +
					"(headers['subDirectory']) must not resolve to null.",
					e.getCause().getMessage());

			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown");
	}

	@Test
	public void saveToSubWithFileExpressionUnsupportedObjectType() throws Exception {

		final Integer unsupportedObject = Integer.valueOf(1234);
		final Message<File> messageWithFileHeader = MessageBuilder.fromMessage(message)
				.setHeader("subDirectory", unsupportedObject)
				.build();

		try {
			this.inputChannelSaveToSubDirWithFile.send(messageWithFileHeader);
		} catch (MessageHandlingException e) {
			Assert.assertEquals("The provided destinationDirectoryExpression" +
					" (headers['subDirectory']) must be of type " +
					"java.io.File or be a String.",
					e.getCause().getMessage());

			return;
		}

		Assert.fail("Was expecting a MessageHandlingException to be thrown");
	}

}
