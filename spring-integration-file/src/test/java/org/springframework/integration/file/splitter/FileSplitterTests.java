/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.file.splitter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeaderKey;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.splitter.FileSplitter.FileMarker;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.util.FileCopyUtils;

import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ruslan Stelmachenko
 *
 * @since 4.1.2
 */
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FileSplitterTests {

	private static File file;

	private static final String SAMPLE_CONTENT = "HelloWorld\näöüß";

	@Autowired
	private MessageChannel input1;

	@Autowired
	private MessageChannel input2;

	@Autowired
	private MessageChannel input3;

	@Autowired
	private PollableChannel output;

	@BeforeClass
	public static void setup() throws IOException {
		file = File.createTempFile("foo", ".txt");
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes("UTF-8"),
				new FileOutputStream(file, false));
	}

	@AfterClass
	public static void tearDown() {
		file.delete();
	}

	@Test
	public void testFileSplitter() throws Exception {
		this.input1.send(new GenericMessage<File>(file));
		Message<?> receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals("HelloWorld", receive.getPayload());
		assertEquals(2, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertEquals("äöüß", receive.getPayload());
		assertEquals(file, receive.getHeaders().get(FileHeaders.ORIGINAL_FILE));
		assertEquals(file.getName(), receive.getHeaders().get(FileHeaders.FILENAME));
		assertNull(this.output.receive(1));

		this.input1.send(new GenericMessage<String>(file.getAbsolutePath()));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(2, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertEquals(file, receive.getHeaders().get(FileHeaders.ORIGINAL_FILE));
		assertEquals(file.getName(), receive.getHeaders().get(FileHeaders.FILENAME));
		assertNull(this.output.receive(1));

		this.input1.send(new GenericMessage<Reader>(new FileReader(file)));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(2, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.output.receive(1));

		this.input2.send(new GenericMessage<File>(file));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.output.receive(1));

		this.input2.send(new GenericMessage<InputStream>(new ByteArrayInputStream(SAMPLE_CONTENT.getBytes("UTF-8"))));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.output.receive(1));

		try {
			this.input2.send(new GenericMessage<String>("bar"));
			fail("FileNotFoundException expected");
		}
		catch (Exception e) {
			assertThat(e.getCause(), instanceOf(FileNotFoundException.class));
			assertThat(e.getMessage(), containsString("failed to read file [bar]"));
		}
		this.input2.send(new GenericMessage<Date>(new Date()));
		receive = this.output.receive(10000);
		assertNotNull(receive);
		assertEquals(1, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertThat(receive.getPayload(), instanceOf(Date.class));
		assertNull(this.output.receive(1));

		this.input3.send(new GenericMessage<File>(file));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.output.receive(1));

		this.input3.send(new GenericMessage<InputStream>(new ByteArrayInputStream(SAMPLE_CONTENT.getBytes("UTF-8"))));
		receive = this.output.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.output.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.output.receive(1));
	}

	@Test
	public void testMarkers() {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<File>(file));
		Message<?> received = outputChannel.receive(0);
		assertNotNull(received);
		assertNull(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertEquals("START", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.START, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertNotNull(outputChannel.receive(0));
		assertNotNull(outputChannel.receive(0));
		received = outputChannel.receive(0);
		assertNotNull(received);
		assertEquals("END", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.END, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(2, fileMarker.getLineCount());
	}

	@Test
	public void testMarkersEmptyFile() throws IOException {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		File file = File.createTempFile("empty", ".txt");
		splitter.handleMessage(new GenericMessage<File>(file));
		Message<?> received = outputChannel.receive(0);
		assertNotNull(received);
		assertNull(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertEquals("START", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileMarker.Mark.START, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(0, fileMarker.getLineCount());

		received = outputChannel.receive(0);
		assertNotNull(received);

		assertEquals("END", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileMarker.Mark.END, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(0, fileMarker.getLineCount());
	}

	@Test
	public void testMarkersJson() throws Exception {
		JsonObjectMapper<?, ?> objectMapper = JsonObjectMapperProvider.newInstance();
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true, true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<File>(file));
		Message<?> received = outputChannel.receive(0);
		assertNotNull(received);
		assertNull(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertEquals("START", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(String.class));
		String payload = (String) received.getPayload();
		assertThat(payload, containsString("\"mark\":\"START\",\"lineCount\":0"));
		FileMarker fileMarker = objectMapper.fromJson(payload, FileSplitter.FileMarker.class);
		assertEquals(FileSplitter.FileMarker.Mark.START, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertNotNull(outputChannel.receive(0));
		assertNotNull(outputChannel.receive(0));
		received = outputChannel.receive(0);
		assertNotNull(received);
		assertEquals("END", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(String.class));
		fileMarker = objectMapper.fromJson((String) received.getPayload(), FileSplitter.FileMarker.class);
		assertEquals(FileSplitter.FileMarker.Mark.END, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(2, fileMarker.getLineCount());
	}

	@Test
	public void testFileSplitterReactive() {
		FluxMessageChannel outputChannel = new FluxMessageChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setApplySequence(true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));


		StepVerifier.create(outputChannel)
				.assertNext(m -> {
					assertThat(m, hasHeaderKey(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
					assertThat(m, hasHeader(FileHeaders.MARKER, "START"));
					assertThat(m, hasPayload(instanceOf(FileSplitter.FileMarker.class)));
					FileMarker fileMarker = (FileSplitter.FileMarker) m.getPayload();
					assertEquals(FileMarker.Mark.START, fileMarker.getMark());
					assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
				})
				.expectNextCount(2)
				.assertNext(m -> {
					assertThat(m, hasHeader(FileHeaders.MARKER, "END"));
					assertThat(m, hasPayload(instanceOf(FileSplitter.FileMarker.class)));
					FileMarker fileMarker = (FileSplitter.FileMarker) m.getPayload();
					assertEquals(FileMarker.Mark.END, fileMarker.getMark());
					assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
					assertEquals(2, fileMarker.getLineCount());
				})
				.then(() ->
						((Subscriber<?>) TestUtils.getPropertyValue(outputChannel, "subscribers", List.class).get(0))
								.onComplete())
				.verifyComplete();
	}

	@Test
	public void testFirstLineAsHeader() {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setFirstLineAsHeader("firstLine");
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertNotNull(received);
		assertNull(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertNull(received.getHeaders().get("firstLine"));
		assertEquals("START", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.START, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		received = outputChannel.receive(0);
		assertEquals("HelloWorld", received.getHeaders().get("firstLine"));
		assertNotNull(received);
		received = outputChannel.receive(0);
		assertNotNull(received);
		assertEquals("END", received.getHeaders().get(FileHeaders.MARKER));
		assertNull(received.getHeaders().get("firstLine"));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.END, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(1, fileMarker.getLineCount());
	}

	@Test
	public void testFirstLineAsHeaderOnlyHeader() throws IOException {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setFirstLineAsHeader("firstLine");
		splitter.setOutputChannel(outputChannel);
		File file = File.createTempFile("empty", ".txt");
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertNotNull(received);
		assertNull(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		assertNull(received.getHeaders().get("firstLine"));
		assertEquals("START", received.getHeaders().get(FileHeaders.MARKER));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.START, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		received = outputChannel.receive(0);
		assertNotNull(received);
		assertEquals("END", received.getHeaders().get(FileHeaders.MARKER));
		assertNull(received.getHeaders().get("firstLine"));
		assertThat(received.getPayload(), instanceOf(FileSplitter.FileMarker.class));
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertEquals(FileSplitter.FileMarker.Mark.END, fileMarker.getMark());
		assertEquals(file.getAbsolutePath(), fileMarker.getFilePath());
		assertEquals(0, fileMarker.getLineCount());
	}

	@Test
	public void testFileReaderClosedOnException() throws Exception {
		DirectChannel outputChannel = new DirectChannel();
		outputChannel.subscribe(message -> {
			throw new RuntimeException();
		});
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		FileReader fileReader = Mockito.spy(new FileReader(file));
		try {
			splitter.handleMessage(new GenericMessage<Reader>(fileReader));
		}
		catch (RuntimeException e) {
			// ignore
		}
		Mockito.verify(fileReader).close();
	}

	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/file/splitter/FileSplitterTests-context.xml")
	public static class ContextConfiguration {

		@Bean
		public PollableChannel output() {
			return new QueueChannel();
		}

		@Bean
		@Splitter(inputChannel = "input2")
		public MessageHandler fileSplitter2() {
			FileSplitter fileSplitter = new FileSplitter(true);
			fileSplitter.setOutputChannel(output());
			return fileSplitter;
		}

		@Bean
		@Splitter(inputChannel = "input3")
		public MessageHandler fileSplitter3() {
			FileSplitter fileSplitter = new FileSplitter();
			fileSplitter.setCharset(Charset.defaultCharset());
			fileSplitter.setOutputChannel(output());
			return fileSplitter;
		}

	}

}
