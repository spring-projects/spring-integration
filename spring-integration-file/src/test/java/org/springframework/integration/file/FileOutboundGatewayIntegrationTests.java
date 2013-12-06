/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Alex Peters
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileOutboundGatewayIntegrationTests {

	@Qualifier("copyInput")
	@Autowired
	MessageChannel copyInputChannel;

	@Qualifier("moveInput")
	@Autowired
	MessageChannel moveInputChannel;

	@Qualifier("moveInput")
	@Autowired
	MessageChannel fileOutboundGatewayInsideChain;

	@Qualifier("output")
	@Autowired
	QueueChannel outputChannel;

	@Autowired
	BeanFactory beanFactory;

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\n��������";

	Message<File> message;

	File sourceFile;

	static File workDir;

	@BeforeClass
	public static void setupClass() {
		workDir = new File(System.getProperty("java.io.tmpdir"), "anyDir");
		workDir.mkdir();
		workDir.deleteOnExit();
	}

	@AfterClass
	public static void cleanUp() {
		if (workDir != null && workDir.exists()) {
			for (File file : workDir.listFiles()) {
				file.delete();
			}
		}
		workDir.delete();
	}

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
	public void instancesCreated() throws Exception {
		assertThat(beanFactory.getBean("copier"), is(notNullValue()));
		assertThat(beanFactory.getBean("mover"), is(notNullValue()));
	}

	@Test
	public void copy() throws Exception {
		copyInputChannel.send(message);
		List<Message<?>> result = outputChannel.clear();
		assertThat(result.size(), is(1));
		Message<?> resultMessage = result.get(0);
		File payloadFile = (File) resultMessage.getPayload();
		assertThat(payloadFile, is(not(sourceFile)));
		assertThat(resultMessage.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class),
				is(sourceFile));
		assertThat(sourceFile.exists(), is(true));
		assertThat(payloadFile.exists(), is(true));
	}

	@Test
	public void move() throws Exception {
		moveInputChannel.send(message);
		List<Message<?>> result = outputChannel.clear();
		assertThat(result.size(), is(1));
		Message<?> resultMessage = result.get(0);
		File payloadFile = (File) resultMessage.getPayload();
		assertThat(payloadFile, is(not(sourceFile)));
		assertThat(resultMessage.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class),
				is(sourceFile));
		assertThat(sourceFile.exists(), is(false));
		assertThat(payloadFile.exists(), is(true));
	}

	@Test //INT-1029
	public void moveInsideTheChain() throws Exception {
		// INT-2755
		Object bean = this.beanFactory.getBean("org.springframework.integration.handler.MessageHandlerChain#0$child.file-outbound-gateway-within-chain.handler");
		assertTrue(bean instanceof FileWritingMessageHandler);

		fileOutboundGatewayInsideChain.send(message);
		List<Message<?>> result = outputChannel.clear();
		assertThat(result.size(), is(1));
		Message<?> resultMessage = result.get(0);
		File payloadFile = (File) resultMessage.getPayload();
		assertThat(payloadFile, is(not(sourceFile)));
		assertThat(resultMessage.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class),
				is(sourceFile));
		assertThat(sourceFile.exists(), is(false));
		assertThat(payloadFile.exists(), is(true));
	}

}
