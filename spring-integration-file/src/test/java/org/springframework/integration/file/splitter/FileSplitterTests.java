/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.file.splitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

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
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ruslan Stelmachenko
 *
 * @since 4.1.2
 */
@SpringJUnitConfig(FileSplitterTests.ContextConfiguration.class)
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

	@Autowired
	private MetadataStoreSelector selector;

	@Autowired
	private ConcurrentMetadataStore store;

	@BeforeAll
	static void setup(@TempDir File tempDir) throws IOException {
		file = new File(tempDir, "foo.txt");
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(StandardCharsets.UTF_8),
				new FileOutputStream(file, false));
	}

	@Test
	void testFileSplitter() throws Exception {
		this.input1.send(new GenericMessage<>(file));
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getPayload()).isEqualTo("HelloWorld");
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(2);
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(1);
		assertThat(this.selector.accept(receive)).isTrue();
		assertThat(this.store.get(file.getAbsolutePath())).isEqualTo("1");
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();  //äöüß
		assertThat(receive.getPayload()).isEqualTo("äöüß");
		assertThat(receive.getHeaders().get(FileHeaders.ORIGINAL_FILE)).isEqualTo(file);
		assertThat(receive.getHeaders().get(FileHeaders.FILENAME)).isEqualTo(file.getName());
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(2);
		assertThat(this.output.receive(1)).isNull();
		assertThat(this.selector.accept(receive)).isTrue();
		assertThat(this.store.get(file.getAbsolutePath())).isEqualTo("2");
		assertThat(this.selector.accept(receive)).isFalse();

		this.input1.send(new GenericMessage<>(file.getAbsolutePath()));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(2);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(receive.getHeaders().get(FileHeaders.ORIGINAL_FILE)).isEqualTo(file);
		assertThat(receive.getHeaders().get(FileHeaders.FILENAME)).isEqualTo(file.getName());
		assertThat(this.output.receive(1)).isNull();

		this.input1.send(new GenericMessage<Reader>(new FileReader(file)));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(2);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(this.output.receive(1)).isNull();

		this.input2.send(new GenericMessage<>(file));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(0);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(this.output.receive(1)).isNull();

		this.input2.send(new GenericMessage<InputStream>(
				new ByteArrayInputStream(SAMPLE_CONTENT.getBytes(StandardCharsets.UTF_8))));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(0);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(this.output.receive(1)).isNull();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.input2.send(new GenericMessage<>("bar")))
				.withCauseInstanceOf(FileNotFoundException.class)
				.withMessageContaining("Failed to read file [bar]");

		this.input2.send(new GenericMessage<>(new Date()));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(1);
		assertThat(receive.getPayload()).isInstanceOf(Date.class);
		assertThat(this.output.receive(1)).isNull();

		this.input3.send(new GenericMessage<>(file));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(0);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(this.output.receive(1)).isNull();

		this.input3.send(new GenericMessage<>(
				new ByteArrayInputStream(SAMPLE_CONTENT.getBytes(StandardCharsets.UTF_8))));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(0);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		assertThat(this.output.receive(1)).isNull();
	}

	@Test
	void testMarkers() {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("START");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.START);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(outputChannel.receive(0)).isNotNull();
		assertThat(outputChannel.receive(0)).isNotNull();
		received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("END");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.END);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(2);
	}

	@Test
	void testMarkersEmptyFile() throws IOException {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		File file = File.createTempFile("empty", ".txt");
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("START");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileMarker.Mark.START);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(0);

		received = outputChannel.receive(0);
		assertThat(received).isNotNull();

		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("END");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileMarker.Mark.END);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(0);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMarkersJson() throws Exception {
		JsonObjectMapper<?, ?> objectMapper = JsonObjectMapperProvider.newInstance();
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true, true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("START");
		assertThat(received.getPayload()).isInstanceOf(String.class);
		String payload = (String) received.getPayload();
		assertThat((List<String>) JsonPathUtils.evaluate(payload, "$..mark")).hasSize(1).contains("START");
		assertThat((List<Integer>) JsonPathUtils.evaluate(payload, "$..lineCount")).hasSize(1).contains(0);
		FileMarker fileMarker = objectMapper.fromJson(payload, FileSplitter.FileMarker.class);
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.START);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(outputChannel.receive(0)).isNotNull();
		assertThat(outputChannel.receive(0)).isNotNull();
		received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("END");
		assertThat(received.getPayload()).isInstanceOf(String.class);
		fileMarker = objectMapper.fromJson(received.getPayload(), FileSplitter.FileMarker.class);
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.END);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(2);
	}

	@Test
	void testFileSplitterReactive() {
		FluxMessageChannel outputChannel = new FluxMessageChannel();
		StepVerifier verifier =
				StepVerifier.create(outputChannel)
						.assertNext(m -> {
							assertThat(m.getHeaders())
									.containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)
									.containsEntry(FileHeaders.MARKER, "START");
							assertThat(m.getPayload()).isInstanceOf(FileMarker.class);
							FileMarker fileMarker = (FileMarker) m.getPayload();
							assertThat(fileMarker.getMark()).isEqualTo(FileMarker.Mark.START);
							assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
						})
						.expectNextCount(2)
						.assertNext(m -> {
							assertThat(m.getHeaders()).containsEntry(FileHeaders.MARKER, "END");
							assertThat(m.getPayload()).isInstanceOf(FileMarker.class);
							FileMarker fileMarker = (FileMarker) m.getPayload();
							assertThat(fileMarker.getMark()).isEqualTo(FileMarker.Mark.END);
							assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
							assertThat(fileMarker.getLineCount()).isEqualTo(2);
						})
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setApplySequence(true);
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	void testFirstLineAsHeader() {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setFirstLineAsHeader("firstLine");
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isNull();
		assertThat(received.getHeaders().get("firstLine")).isNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("START");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.START);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		received = outputChannel.receive(0);
		assertThat(received.getHeaders().get("firstLine")).isEqualTo("HelloWorld");
		assertThat(received).isNotNull();
		received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("END");
		assertThat(received.getHeaders().get("firstLine")).isNull();
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.END);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(1);
	}

	@Test
	void testFirstLineAsHeaderOnlyHeader() throws IOException {
		QueueChannel outputChannel = new QueueChannel();
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setFirstLineAsHeader("firstLine");
		splitter.setOutputChannel(outputChannel);
		File file = File.createTempFile("empty", ".txt");
		splitter.handleMessage(new GenericMessage<>(file));
		Message<?> received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isNull();
		assertThat(received.getHeaders().get("firstLine")).isNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("START");
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		FileMarker fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.START);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		received = outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.MARKER)).isEqualTo("END");
		assertThat(received.getHeaders().get("firstLine")).isNull();
		assertThat(received.getPayload()).isInstanceOf(FileSplitter.FileMarker.class);
		fileMarker = (FileSplitter.FileMarker) received.getPayload();
		assertThat(fileMarker.getMark()).isEqualTo(FileSplitter.FileMarker.Mark.END);
		assertThat(fileMarker.getFilePath()).isEqualTo(file.getAbsolutePath());
		assertThat(fileMarker.getLineCount()).isEqualTo(0);
	}

	@Test
	void testFileReaderClosedOnException() throws Exception {
		DirectChannel outputChannel = new DirectChannel();
		outputChannel.subscribe(message -> {
			throw new RuntimeException();
		});
		FileSplitter splitter = new FileSplitter(true, true);
		splitter.setOutputChannel(outputChannel);
		FileReader fileReader = Mockito.spy(new FileReader(file));
		try {
			splitter.handleMessage(new GenericMessage<>(fileReader));
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

		@Bean
		public IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
			return new IdempotentReceiverInterceptor(selector());
		}

		@Bean
		public MetadataStoreSelector selector() {
			return new MetadataStoreSelector(
					message -> message.getHeaders().get(FileHeaders.ORIGINAL_FILE, File.class).getAbsolutePath(),
					message -> message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER).toString(),
					store())
					.compareValues((oldVal, newVal) -> Integer.parseInt(oldVal) < Integer.parseInt(newVal));
		}

		@Bean
		public ConcurrentMetadataStore store() {
			return new SimpleMetadataStore();
		}

	}

}
