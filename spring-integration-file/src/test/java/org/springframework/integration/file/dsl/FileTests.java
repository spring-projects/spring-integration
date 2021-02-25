/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.file.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.RecursiveDirectoryScanner;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.support.FileUtils;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FileTests {

	@TempDir
	static File tmpDir;

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("fileFlow1Input")
	private MessageChannel fileFlow1Input;

	@Autowired
	@Qualifier("fileWriting.handler")
	private MessageHandler fileWritingMessageHandler;

	@Autowired
	@Qualifier("tailChannel")
	private PollableChannel tailChannel;

	@Autowired
	private ApacheCommonsFileTailingMessageProducer tailer;

	@Autowired
	@Qualifier("fileReadingResultChannel")
	private PollableChannel fileReadingResultChannel;

	@Autowired
	@Qualifier("fileWritingInput")
	private MessageChannel fileWritingInput;

	@Autowired
	@Qualifier("fileWritingResultChannel")
	private PollableChannel fileWritingResultChannel;

	@Autowired
	@Qualifier("fileSplittingResultChannel")
	private PollableChannel fileSplittingResultChannel;

	@Autowired
	@Qualifier("fileTriggerFlow.input")
	private MessageChannel fileTriggerFlowInput;

	@Autowired
	private CountDownLatch flushPredicateCalled;

	@Test
	public void testFileHandler() throws Exception {
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(FileHeaders.FILENAME, "foo").build();
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.fileFlow1Input.send(message))
				.withCauseInstanceOf(NullPointerException.class);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setBeanFactory(this.beanFactory);
		Object targetFileWritingMessageHandler = this.fileWritingMessageHandler;
		if (this.fileWritingMessageHandler instanceof Advised) {
			TargetSource targetSource = ((Advised) this.fileWritingMessageHandler).getTargetSource();
			if (targetSource != null) {
				targetFileWritingMessageHandler = targetSource.getTarget();
			}
		}
		DirectFieldAccessor dfa = new DirectFieldAccessor(targetFileWritingMessageHandler);
		assertThat(dfa.getPropertyValue("flushWhenIdle")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("flushInterval")).isEqualTo(60000L);
		dfa.setPropertyValue("fileNameGenerator", fileNameGenerator);
		this.fileFlow1Input.send(message);

		assertThat(new File(tmpDir, "foo").exists()).isTrue();

		this.fileTriggerFlowInput.send(new GenericMessage<>("trigger"));
		assertThat(this.flushPredicateCalled.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testMessageProducerFlow() throws Exception {
		File tailTestFile = new File(tmpDir, "TailTest");
		FileOutputStream file = new FileOutputStream(tailTestFile);
		for (int i = 0; i < 50; i++) {
			file.write((i + "\n").getBytes());
		}
		file.close();
		this.tailer.start();
		for (int i = 0; i < 50; i++) {
			Message<?> message = this.tailChannel.receive(5000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo("hello " + i);
		}
		assertThat(this.tailChannel.receive(1)).isNull();

		this.controlBus.send("@tailer.stop()");

		while (!tailTestFile.delete()) {
			Thread.sleep(100);
		}
	}

	@Autowired
	private PollableChannel filePollingErrorChannel;

	@Test
	public void testFileReadingFlow() throws Exception {
		List<Integer> evens = new ArrayList<>(25);
		for (int i = 0; i < 50; i++) {
			boolean even = i % 2 == 0;
			String extension = even ? ".sitest" : ".foofile";
			if (even) {
				evens.add(i);
			}
			File tmpFile = new File(tmpDir, i + extension + ".tmp");
			FileOutputStream stream = new FileOutputStream(tmpFile);
			stream.write(("" + i).getBytes());
			stream.flush();
			stream.close();
			tmpFile.renameTo(new File(tmpDir, i + extension));
		}

		Message<?> message = fileReadingResultChannel.receive(60000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) payload;
		assertThat(result.size()).isEqualTo(25);
		result.forEach(s -> assertThat(evens.contains(Integer.parseInt(s))).isTrue());

		new File(tmpDir, "a.sitest").createNewFile();
		Message<?> receive = this.filePollingErrorChannel.receive(60000);
		assertThat(receive).isNotNull();
		assertThat(receive).isInstanceOf(ErrorMessage.class);
	}

	@Test
	public void testFileWritingFlow() throws Exception {
		String payload = "Spring Integration";
		this.fileWritingInput.send(new GenericMessage<>(payload));
		Message<?> receive = this.fileWritingResultChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(File.class);
		File resultFile = (File) receive.getPayload();
		assertThat(resultFile.getAbsolutePath())
				.endsWith(TestUtils.applySystemFileSeparator("fileWritingFlow/foo.write"));
		String fileContent = FileCopyUtils.copyToString(new FileReader(resultFile));
		assertThat(fileContent).isEqualTo(payload);
		if (FileUtils.IS_POSIX) {
			assertThat(java.nio.file.Files.getPosixFilePermissions(resultFile.toPath()).size()).isEqualTo(9);
		}
	}

	@Autowired
	@Qualifier("fileSplitter.handler")
	private MessageHandler fileSplitter;

	@Test
	public void testFileSplitterFlow() throws Exception {
		FileOutputStream file = new FileOutputStream(new File(tmpDir, "foo.tmp"));
		file.write(("HelloWorld\näöüß").getBytes(Charset.defaultCharset()));
		file.flush();
		file.close();

		Message<?> receive = this.fileSplittingResultChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(FileSplitter.FileMarker.class); // FileMarker.Mark.START
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(0);
		receive = this.fileSplittingResultChannel.receive(10000);
		assertThat(receive).isNotNull(); //HelloWorld
		receive = this.fileSplittingResultChannel.receive(10000);
		assertThat(receive).isNotNull(); //äöüß
		receive = this.fileSplittingResultChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(FileSplitter.FileMarker.class); // FileMarker.Mark.END
		assertThat(this.fileSplittingResultChannel.receive(1)).isNull();

		assertThat(TestUtils.getPropertyValue(this.fileSplitter, "charset")).isEqualTo(StandardCharsets.US_ASCII);
	}

	@Autowired
	@Qualifier("dynamicAdaptersResult")
	PollableChannel dynamicAdaptersResult;

	@Autowired
	private MyService myService;

	@Test
	public void testDynamicFileFlows() throws Exception {
		File newFolder1 = java.nio.file.Files.createTempDirectory(tmpDir.toPath(), "junit").toFile();
		FileOutputStream file = new FileOutputStream(new File(newFolder1, "foo"));
		file.write(("foo").getBytes());
		file.flush();
		file.close();

		File newFolder2 = java.nio.file.Files.createTempDirectory(tmpDir.toPath(), "junit").toFile();
		file = new FileOutputStream(new File(newFolder2, "bar"));
		file.write(("bar").getBytes());
		file.flush();
		file.close();

		this.myService.pollDirectories(newFolder1, newFolder2);

		Set<String> payloads = new TreeSet<>();
		Message<?> receive = this.dynamicAdaptersResult.receive(10000);
		assertThat(receive).isNotNull();
		payloads.add((String) receive.getPayload());
		receive = this.dynamicAdaptersResult.receive(10000);
		assertThat(receive).isNotNull();
		payloads.add((String) receive.getPayload());

		assertThat(payloads.toArray()).isEqualTo(new String[]{ "bar", "foo" });

		assertThat(TestUtils.getPropertyValue(
				this.beanFactory.getBean(newFolder1.getName() + ".adapter.source"),
				"scanner"))
				.isInstanceOf(RecursiveDirectoryScanner.class);
	}

	@MessagingGateway(defaultRequestChannel = "controlBus.input")
	private interface ControlBusGateway {

		void send(String command);

	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	@IntegrationComponentScan
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow controlBus() {
			return IntegrationFlowDefinition::controlBus;
		}

		@Bean
		public IntegrationFlow fileTriggerFlow() {
			return f -> f.handle("fileWriting.handler", "trigger");
		}

		@Bean
		public CountDownLatch flushPredicateCalled() {
			return new CountDownLatch(1);
		}

		@Bean
		public IntegrationFlow fileFlow1() {
			return IntegrationFlows.from("fileFlow1Input")
					.handle(Files.outboundAdapter("'file://" + tmpDir.getAbsolutePath() + '\'')
									.fileNameGenerator(message -> null)
									.fileExistsMode(FileExistsMode.APPEND_NO_FLUSH)
									.flushInterval(60000)
									.flushWhenIdle(false)
									.flushPredicate((fileAbsolutePath, firstWrite, lastWrite, filterMessage) -> {
										flushPredicateCalled().countDown();
										return true;
									}),
							c -> c.id("fileWriting"))
					.get();
		}

		@Bean
		public IntegrationFlow tailFlow() {
			return IntegrationFlows
					.from(Files.tailAdapter(new File(tmpDir, "TailTest"))
							.delay(500)
							.end(false)
							.id("tailer")
							.autoStartup(false))
					.transform("hello "::concat)
					.channel(MessageChannels.queue("tailChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow fileReadingFlow() {
			return IntegrationFlows
					.from(Files.inboundAdapter(tmpDir)
									.patternFilter("*.sitest")
									.useWatchService(true)
									.watchEvents(FileReadingMessageSource.WatchEventType.CREATE,
											FileReadingMessageSource.WatchEventType.MODIFY),
							e -> e.poller(Pollers.fixedDelay(100)
									.errorChannel("filePollingErrorChannel")))
					.filter(File.class, p -> !p.getName().startsWith("a"),
							e -> e.throwExceptionOnRejection(true))
					.transform(Files.toStringTransformer())
					.aggregate(a -> a.correlationExpression("1")
							.releaseStrategy(g -> g.size() == 25))
					.channel(MessageChannels.queue("fileReadingResultChannel"))
					.get();
		}

		@Bean
		public PollableChannel filePollingErrorChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow fileWritingFlow() {
			return IntegrationFlows.from("fileWritingInput")
					.enrichHeaders(h -> h.header(FileHeaders.FILENAME, "foo.write")
							.header("directory", new File(tmpDir, "fileWritingFlow")))
					.handle(Files.outboundGateway(m -> m.getHeaders().get("directory"))
							.preserveTimestamp(true)
							.chmod(0777))
					.channel(MessageChannels.queue("fileWritingResultChannel"))
					.get();
		}


		@Bean
		public IntegrationFlow fileSplitterFlow(BeanFactory beanFactory) {
			ExpressionFileListFilter<File> fileExpressionFileListFilter =
					new ExpressionFileListFilter<>(new FunctionExpression<File>(f -> "foo.tmp".equals(f.getName())));
			fileExpressionFileListFilter.setBeanFactory(beanFactory);

			return IntegrationFlows
					.from(Files.inboundAdapter(tmpDir)
									.filter(new ChainFileListFilter<File>()
											.addFilter(new AcceptOnceFileListFilter<>())
											.addFilter(fileExpressionFileListFilter)),
							e -> e.poller(p -> p.fixedDelay(100)))
					.split(Files.splitter()
									.markers()
									.charset(StandardCharsets.US_ASCII)
									.applySequence(true),
							e -> e.id("fileSplitter"))
					.channel(c -> c.queue("fileSplittingResultChannel"))
					.get();
		}

		@Bean
		public PollableChannel dynamicAdaptersResult() {
			return new QueueChannel();
		}

	}

	@Service
	public static class MyService {

		@Autowired
		private AutowireCapableBeanFactory beanFactory;

		@Autowired
		@Qualifier("dynamicAdaptersResult")
		PollableChannel dynamicAdaptersResult;

		void pollDirectories(File... directories) {
			for (File directory : directories) {
				StandardIntegrationFlow integrationFlow = IntegrationFlows
						.from(Files.inboundAdapter(directory).recursive(true),
								e -> e.poller(p -> p.fixedDelay(1000))
										.id(directory.getName() + ".adapter"))
						.transform(Files.toStringTransformer(),
								e -> e.id(directory.getName() + ".transformer"))
						.channel(this.dynamicAdaptersResult)
						.get();
				this.beanFactory.initializeBean(integrationFlow, directory.getName());
				this.beanFactory.getBean(directory.getName() + ".transformer", Lifecycle.class).start();
				this.beanFactory.getBean(directory.getName() + ".adapter", Lifecycle.class).start();
			}
		}

	}

}
