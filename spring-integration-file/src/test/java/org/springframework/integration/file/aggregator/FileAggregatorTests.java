/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.file.aggregator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.5
 */
@SpringJUnitConfig
@DirtiesContext
public class FileAggregatorTests {

	static EmbeddedDatabase dataSource;

	@TempDir
	static File tmpDir;

	static File file;

	@Autowired
	@Qualifier("fileSplitterAggregatorFlow.input")
	MessageChannel fileSplitterAggregatorFlow;

	@Autowired
	@Qualifier("jdbcMessageStoreAggregatorFlow.input")
	MessageChannel jdbcMessageStoreAggregatorFlow;

	@Autowired
	PollableChannel resultChannel;

	@Autowired
	MessageChannel input;

	@Autowired
	PollableChannel output;

	@BeforeAll
	static void setup() throws IOException {
		file = new File(tmpDir, "foo.txt");
		String content =
				"file header\n" +
						"first line\n" +
						"second line\n" +
						"last line";
		FileCopyUtils.copy(content.getBytes(StandardCharsets.UTF_8), new FileOutputStream(file, false));

		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-drop-h2.sql")
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();
	}

	@AfterAll
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test
	void testFileAggregator() {
		this.fileSplitterAggregatorFlow.send(new GenericMessage<>(file));

		Message<?> receive = this.resultChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders())
				.containsEntry(FileHeaders.FILENAME, "foo.txt")
				.containsEntry(FileHeaders.LINE_COUNT, 3L)
				.containsEntry("firstLine", "file header")
				.doesNotContainKey(IntegrationMessageHeaderAccessor.CORRELATION_ID);

		assertThat(receive.getPayload())
				.isInstanceOf(List.class)
				.asList()
				.contains("SECOND LINE", "LAST LINE", "FIRST LINE");
	}

	@Test
	void testEmptyFileAggregator() throws IOException {
		File file = new File(tmpDir, "empty.txt");
		file.createNewFile();
		this.jdbcMessageStoreAggregatorFlow.send(new GenericMessage<>(file));

		Message<?> receive = this.resultChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders())
				.containsEntry(FileHeaders.FILENAME, "empty.txt")
				.containsEntry(FileHeaders.LINE_COUNT, 0L)
				.doesNotContainKey(IntegrationMessageHeaderAccessor.CORRELATION_ID);

		assertThat(receive.getPayload())
				.isInstanceOf(List.class)
				.asList()
				.isEmpty();
	}

	@Test
	void testFileAggregatorXmlConfig() {
		this.input.send(new GenericMessage<>(file));

		Message<?> receive = this.output.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders())
				.containsEntry(FileHeaders.FILENAME, "foo.txt")
				.containsEntry(FileHeaders.LINE_COUNT, 4L)
				.doesNotContainKeys("firstLine", IntegrationMessageHeaderAccessor.CORRELATION_ID);

		assertThat(receive.getPayload())
				.isInstanceOf(List.class)
				.asList()
				.containsExactly("file header", "first line", "second line", "last line");
	}

	@Configuration
	@EnableIntegration
	@ImportResource("org/springframework/integration/file/aggregator/FileAggregatorTests.xml")
	public static class Config {

		@Bean
		public IntegrationFlow fileSplitterAggregatorFlow(TaskExecutor taskExecutor) {
			return f -> f
					.split(Files.splitter()
							.markers()
							.firstLineAsHeader("firstLine"))
					.channel(c -> c.executor(taskExecutor))
					.filter(payload -> !(payload instanceof FileSplitter.FileMarker),
							e -> e.discardChannel("aggregatorChannel"))
					.<String, String>transform(String::toUpperCase)
					.channel("aggregatorChannel")
					.aggregate(new FileAggregator())
					.channel(resultChannel());
		}

		@Bean
		public IntegrationFlow jdbcMessageStoreAggregatorFlow() {
			return f -> f
					.split(Files.splitter().markers())
					.aggregate(aggregator ->
							aggregator.processor(new FileAggregator())
									.messageStore(new JdbcMessageStore(dataSource)))
					.channel(resultChannel());
		}

		@Bean
		PollableChannel resultChannel() {
			return new QueueChannel();
		}

	}

}
