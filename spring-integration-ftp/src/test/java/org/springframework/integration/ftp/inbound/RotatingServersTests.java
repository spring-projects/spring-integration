/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.ftp.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.aop.RotatingServerAdvice;
import org.springframework.integration.file.remote.aop.RotationPolicy;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryLocator;
import org.springframework.integration.file.remote.session.SessionFactoryMapBuilder;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.7
 *
 */
public class RotatingServersTests extends FtpTestSupport {

	private static final String TMP_DIR = getLocalTempFolder().getAbsolutePath() + File.separator + "multiSF";

	@BeforeEach
	public void setup() {
		FtpRemoteFileTemplate rft = new FtpRemoteFileTemplate(sessionFactory());
		rft.execute(s -> {
			s.mkdir("foo");
			s.mkdir("bar");
			s.mkdir("baz");
			s.mkdir("qux");
			s.mkdir("fiz");
			s.mkdir("buz");
			ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
			s.write(bais, "foo/f1");
			s.write(bais, "baz/f2");
			s.write(bais, "fiz/f3");
			return null;
		});
	}

	@BeforeEach
	public void extraSetup(TestInfo info) {
		if (info.getTestMethod().get().getName().equals("testFairStreaming")) {
			FtpRemoteFileTemplate rft = new FtpRemoteFileTemplate(sessionFactory());
			rft.execute(s -> {
				ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
				// 2 files per server, remove empty dirs
				s.write(bais, "foo/f4");
				s.write(bais, "baz/f5");
				s.write(bais, "fiz/f6");
				s.rmdir("bar");
				s.rmdir("qux");
				s.rmdir("buz");
				return null;
			});
		}
	}

	@BeforeEach
	@AfterEach
	public void clean(TestInfo info) {
		recursiveDelete(new File(TMP_DIR), info);
	}

	@AfterEach
	public void extraCleanUp(TestInfo info) {
		FtpRemoteFileTemplate rft = new FtpRemoteFileTemplate(sessionFactory());
		rft.execute(s -> {
			if (info.getTestMethod().get().getName().equals("testFairStreaming")) {
				s.remove("foo/f4");
				s.remove("baz/f5");
				s.remove("fiz/f6");
			}
			s.remove("foo/f1");
			s.remove("baz/f2");
			s.remove("fiz/f3");
			return null;
		});
	}

	@Test
	public void testStandard() throws Exception {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StandardConfig.class)) {
			StandardConfig config = ctx.getBean(StandardConfig.class);
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			ctx.getBean(SourcePollingChannelAdapter.class).stop();
			List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
			assertThat(sfCalls).containsExactly(1, 1, 1, 2, 2, 2, 3, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1);
			File f1 = new File(TMP_DIR + File.separator + "standard" + File.separator + "f1");
			assertThat(f1.exists()).isTrue();
			File f2 = new File(TMP_DIR + File.separator + "standard" + File.separator + "f2");
			assertThat(f2.exists()).isTrue();
			File f3 = new File(TMP_DIR + File.separator + "standard" + File.separator + "f3");
			assertThat(f3.exists()).isTrue();
			assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		}
	}

	@Test
	public void testFair() throws Exception {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FairConfig.class)) {
			StandardConfig config = ctx.getBean(StandardConfig.class);
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			ctx.getBean(SourcePollingChannelAdapter.class).stop();
			List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
			assertThat(sfCalls).containsExactly(1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3);
			File f1 = new File(TMP_DIR + File.separator + "fair" + File.separator + "f1");
			assertThat(f1.exists()).isTrue();
			File f2 = new File(TMP_DIR + File.separator + "fair" + File.separator + "f2");
			assertThat(f2.exists()).isTrue();
			File f3 = new File(TMP_DIR + File.separator + "fair" + File.separator + "f3");
			assertThat(f3.exists()).isTrue();
			assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		}
	}

	@Test
	public void testVariableLocalDir() throws Exception {
		try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(VariableLocalConfig.class)) {
			StandardConfig config = ctx.getBean(StandardConfig.class);
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			ctx.getBean(SourcePollingChannelAdapter.class).stop();
			List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
			assertThat(sfCalls).containsExactly(1, 1, 1, 2, 2, 2, 3, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1);
			File f1 = new File(TMP_DIR + File.separator + "variable" + File.separator + "foo" + File.separator + "f1");
			assertThat(f1.exists()).isTrue();
			File f2 = new File(TMP_DIR + File.separator + "variable" + File.separator + "baz" + File.separator + "f2");
			assertThat(f2.exists()).isTrue();
			File f3 = new File(TMP_DIR + File.separator + "variable" + File.separator + "fiz" + File.separator + "f3");
			assertThat(f3.exists()).isTrue();
			assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		}
	}

	@Test
	public void testStreaming() throws Exception {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StreamingConfig.class)) {
			StandardConfig config = ctx.getBean(StreamingConfig.class);
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			ctx.getBean(SourcePollingChannelAdapter.class).stop();
			List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
			// there's an extra getSession() with this adapter in listFiles
			assertThat(sfCalls).containsExactly(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 1, 1, 2, 2, 3);
			QueueChannel files = ctx.getBean("files", QueueChannel.class);
			assertThat(files.getQueueSize()).isEqualTo(3);
			Message<?> received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f1");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f2");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f3");
		}
	}

	@Test
	public void testFairStreaming() throws Exception {
		try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(FairStreamingConfig.class)) {
			StandardConfig config = ctx.getBean(StandardConfig.class);
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			ctx.getBean(SourcePollingChannelAdapter.class).stop();
			List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
			assertThat(sfCalls).containsExactly(1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3, 3, 1, 2, 3, 1, 2);
			QueueChannel files = ctx.getBean("files", QueueChannel.class);
			assertThat(files.getQueueSize()).isEqualTo(6);
			Message<?> received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f1");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f2");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f3");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f4");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f5");
			received = files.receive(0);
			StaticMessageHeaderAccessor.getCloseableResource(received).close();
			assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE, String.class)).isEqualTo("f6");
		}
	}

	@Configuration
	@EnableIntegration
	public static class StandardConfig {

		private final CountDownLatch latch = new CountDownLatch(17);

		final List<Integer> sessionSources = new CopyOnWriteArrayList<>();

		@Bean
		public SessionFactory<FTPFile> factory1() {
			return new CachingSessionFactory<>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					Session<FTPFile> session = super.getSession();
					StandardConfig.this.sessionSources.add(1);
					StandardConfig.this.latch.countDown();
					return session;
				}

			};
		}

		@Bean
		public SessionFactory<FTPFile> factory2() {
			return new CachingSessionFactory<>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					Session<FTPFile> session = super.getSession();
					StandardConfig.this.sessionSources.add(2);
					StandardConfig.this.latch.countDown();
					return session;
				}

			};
		}

		@Bean
		public SessionFactory<FTPFile> factory3() {
			return new CachingSessionFactory<>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					Session<FTPFile> session = super.getSession();
					StandardConfig.this.sessionSources.add(3);
					StandardConfig.this.latch.countDown();
					return session;
				}

			};
		}

		@Bean
		public SessionFactoryLocator<FTPFile> factoryLocator() {
			return new DefaultSessionFactoryLocator<>(new SessionFactoryMapBuilder<FTPFile>()
					.put("one", factory1())
					.put("two", factory2())
					.put("three", factory3())
					.get());
		}

		@Bean
		public DelegatingSessionFactory<FTPFile> sf() {
			return new DelegatingSessionFactory<>(factoryLocator());
		}

		@Bean
		public RotatingServerAdvice advice() {
			List<RotationPolicy.KeyDirectory> keyDirectories = new ArrayList<>();
			keyDirectories.add(new RotationPolicy.KeyDirectory("one", "foo"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("one", "bar"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("two", "baz"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("two", "qux"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("three", "fiz"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("three", "buz"));
			return theAdvice(keyDirectories);
		}

		protected RotatingServerAdvice theAdvice(List<RotationPolicy.KeyDirectory> keyDirectories) {
			return new RotatingServerAdvice(sf(), keyDirectories);
		}

		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Ftp.inboundAdapter(sf())
							.filter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "rotate"))
							.localDirectory(localDir())
							.remoteDirectory("."),
					e -> e.poller(Pollers.fixedDelay(1).advice(advice())))
					.channel(MessageChannels.queue("files"))
					.get();
		}

		protected File localDir() {
			return new File(TMP_DIR, "standard");
		}

	}

	@Configuration
	public static class FairConfig extends StandardConfig {

		@Override
		protected RotatingServerAdvice theAdvice(List<RotationPolicy.KeyDirectory> keyDirectories) {
			return new RotatingServerAdvice(sf(), keyDirectories, true);
		}

		@Override
		protected File localDir() {
			return new File(TMP_DIR, "fair");
		}

	}

	@Configuration
	public static class VariableLocalConfig extends StandardConfig {

		@Override
		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Ftp.inboundAdapter(sf())
							.filter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "rotate"))
							.localDirectory(new File(TMP_DIR, "variable"))
							.localFilenameExpression("#remoteDirectory + T(java.io.File).separator + #root")
							.remoteDirectory("."),
					e -> e.poller(Pollers.fixedDelay(1).advice(advice())))
					.channel(MessageChannels.queue("files"))
					.get();
		}

	}

	@Configuration
	public static class StreamingConfig extends StandardConfig {

		@Override
		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Ftp.inboundStreamingAdapter(new FtpRemoteFileTemplate(sf()))
							.filter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "rotate"))
							.remoteDirectory("."),
					e -> e.poller(Pollers.fixedDelay(1).advice(advice())))
					.channel(MessageChannels.queue("files"))
					.get();
		}

	}

	@Configuration
	public static class FairStreamingConfig extends StandardConfig {

		@Override
		@Bean
		public RotatingServerAdvice advice() {
			List<RotationPolicy.KeyDirectory> keyDirectories = new ArrayList<>();
			keyDirectories.add(new RotationPolicy.KeyDirectory("one", "foo"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("two", "baz"));
			keyDirectories.add(new RotationPolicy.KeyDirectory("three", "fiz"));
			return theAdvice(keyDirectories);
		}

		@Override
		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Ftp.inboundStreamingAdapter(new FtpRemoteFileTemplate(sf()))
							.filter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "rotate"))
							.remoteDirectory(".")
							.maxFetchSize(1),
					e -> e.poller(Pollers.fixedDelay(1).advice(advice())))
					.channel(MessageChannels.queue("files"))
					.get();
		}

		@Override
		protected RotatingServerAdvice theAdvice(List<RotationPolicy.KeyDirectory> keyDirectories) {
			return new RotatingServerAdvice(sf(), keyDirectories, true);
		}

	}

}
