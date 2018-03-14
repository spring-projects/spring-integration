/*
 * Copyright 2016-2018 the original author or authors.
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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

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
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FileTests {

	@ClassRule
	public static final TemporaryFolder tmpDir = new TemporaryFolder();

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
		try {
			this.fileFlow1Input.send(message);
			fail("NullPointerException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(NullPointerException.class));
		}
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
		assertEquals(Boolean.FALSE, dfa.getPropertyValue("flushWhenIdle"));
		assertEquals(60000L, dfa.getPropertyValue("flushInterval"));
		dfa.setPropertyValue("fileNameGenerator", fileNameGenerator);
		this.fileFlow1Input.send(message);

		assertTrue(new File(tmpDir.getRoot(), "foo").exists());

		this.fileTriggerFlowInput.send(new GenericMessage<>("trigger"));
		assertTrue(this.flushPredicateCalled.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testMessageProducerFlow() throws Exception {
		FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), "TailTest"));
		for (int i = 0; i < 50; i++) {
			file.write((i + "\n").getBytes());
		}
		this.tailer.start();
		for (int i = 0; i < 50; i++) {
			Message<?> message = this.tailChannel.receive(5000);
			assertNotNull(message);
			assertEquals("hello " + i, message.getPayload());
		}
		assertNull(this.tailChannel.receive(1));

		this.controlBus.send("@tailer.stop()");
		file.close();
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
			FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), i + extension));
			file.write(("" + i).getBytes());
			file.flush();
			file.close();
		}

		Message<?> message = fileReadingResultChannel.receive(60000);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertThat(payload, instanceOf(List.class));
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) payload;
		assertEquals(25, result.size());
		result.forEach(s -> assertTrue(evens.contains(Integer.parseInt(s))));

		new File(tmpDir.getRoot(), "a.sitest").createNewFile();
		Message<?> receive = this.filePollingErrorChannel.receive(60000);
		assertNotNull(receive);
		assertThat(receive, instanceOf(ErrorMessage.class));
	}

	@Test
	public void testFileWritingFlow() throws Exception {
		String payload = "Spring Integration";
		this.fileWritingInput.send(new GenericMessage<>(payload));
		Message<?> receive = this.fileWritingResultChannel.receive(1000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(File.class));
		File resultFile = (File) receive.getPayload();
		assertThat(resultFile.getAbsolutePath(),
				endsWith(TestUtils.applySystemFileSeparator("fileWritingFlow/foo.write")));
		String fileContent = StreamUtils.copyToString(new FileInputStream(resultFile), Charset.defaultCharset());
		assertEquals(payload, fileContent);
		if (FileUtils.IS_POSIX) {
			assertThat(java.nio.file.Files.getPosixFilePermissions(resultFile.toPath()).size(), equalTo(9));
		}
	}

	@Autowired
	@Qualifier("fileSplitter.handler")
	private MessageHandler fileSplitter;

	@Test
	public void testFileSplitterFlow() throws Exception {
		FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), "foo.tmp"));
		file.write(("HelloWorld\näöüß").getBytes(Charset.defaultCharset()));
		file.flush();
		file.close();

		Message<?> receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(FileSplitter.FileMarker.class)); // FileMarker.Mark.START
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive); //HelloWorld
		receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive); //äöüß
		receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(FileSplitter.FileMarker.class)); // FileMarker.Mark.END
		assertNull(this.fileSplittingResultChannel.receive(1));

		assertEquals(StandardCharsets.US_ASCII, TestUtils.getPropertyValue(this.fileSplitter, "charset"));
	}

	@Autowired
	@Qualifier("dynamicAdaptersResult")
	PollableChannel dynamicAdaptersResult;

	@Autowired
	private MyService myService;

	@Test
	public void testDynamicFileFlows() throws Exception {
		File newFolder1 = tmpDir.newFolder();
		FileOutputStream file = new FileOutputStream(new File(newFolder1, "foo"));
		file.write(("foo").getBytes());
		file.flush();
		file.close();

		File newFolder2 = tmpDir.newFolder();
		file = new FileOutputStream(new File(newFolder2, "bar"));
		file.write(("bar").getBytes());
		file.flush();
		file.close();

		this.myService.pollDirectories(newFolder1, newFolder2);

		Set<String> payloads = new TreeSet<>();
		Message<?> receive = this.dynamicAdaptersResult.receive(10000);
		assertNotNull(receive);
		payloads.add((String) receive.getPayload());
		receive = this.dynamicAdaptersResult.receive(10000);
		assertNotNull(receive);
		payloads.add((String) receive.getPayload());

		assertArrayEquals(new String[] { "bar", "foo" }, payloads.toArray());
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
					.handle(Files.outboundAdapter(tmpDir.getRoot())
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
					.from(Files.tailAdapter(new File(tmpDir.getRoot(), "TailTest"))
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
					.from(Files.inboundAdapter(tmpDir.getRoot())
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
							.header("directory", new File(tmpDir.getRoot(), "fileWritingFlow")))
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
					.from(Files.inboundAdapter(tmpDir.getRoot())
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
						.from(Files.inboundAdapter(directory)
										.scanner(new DefaultDirectoryScanner()),
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
