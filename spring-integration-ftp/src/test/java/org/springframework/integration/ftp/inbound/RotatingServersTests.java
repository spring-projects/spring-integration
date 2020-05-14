/*
 * Copyright 2018-2020 the original author or authors.
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
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

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.7
 *
 */
public class RotatingServersTests extends FtpTestSupport {

	private static String tmpDir = getLocalTempFolder().getAbsolutePath() + File.separator + "multiSF";

	@BeforeAll
	public static void setup() {
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
	@AfterEach
	public void clean(TestInfo info) {
		recursiveDelete(new File(tmpDir), info);
	}

	@Test
	public void testStandard() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StandardConfig.class);
		StandardConfig config = ctx.getBean(StandardConfig.class);
		ctx.getBean(StandardIntegrationFlow.class).stop();
		assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
		assertThat(sfCalls).containsExactly(1, 1, 1, 2, 2, 2, 3, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1);
		File f1 = new File(tmpDir + File.separator + "standard" + File.separator + "f1");
		assertThat(f1.exists()).isTrue();
		File f2 = new File(tmpDir + File.separator + "standard" + File.separator + "f2");
		assertThat(f2.exists()).isTrue();
		File f3 = new File(tmpDir + File.separator + "standard" + File.separator + "f3");
		assertThat(f3.exists()).isTrue();
		assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		ctx.close();
	}

	@Test
	public void testFair() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FairConfig.class);
		StandardConfig config = ctx.getBean(StandardConfig.class);
		ctx.getBean(StandardIntegrationFlow.class).stop();
		assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
		assertThat(sfCalls).containsExactly(1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3);
		File f1 = new File(tmpDir + File.separator + "fair" + File.separator + "f1");
		assertThat(f1.exists()).isTrue();
		File f2 = new File(tmpDir + File.separator + "fair" + File.separator + "f2");
		assertThat(f2.exists()).isTrue();
		File f3 = new File(tmpDir + File.separator + "fair" + File.separator + "f3");
		assertThat(f3.exists()).isTrue();
		assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		ctx.close();
	}

	@Test
	public void testVariableLocalDir() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(VariableLocalConfig.class);
		StandardConfig config = ctx.getBean(StandardConfig.class);
		assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		ctx.getBean(StandardIntegrationFlow.class).stop();
		List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
		assertThat(sfCalls).containsExactly(1, 1, 1, 2, 2, 2, 3, 3, 3, 1, 1, 2, 2, 3, 3, 1, 1);
		File f1 = new File(tmpDir + File.separator + "variable" + File.separator + "foo" + File.separator + "f1");
		assertThat(f1.exists()).isTrue();
		File f2 = new File(tmpDir + File.separator + "variable" + File.separator + "baz" + File.separator + "f2");
		assertThat(f2.exists()).isTrue();
		File f3 = new File(tmpDir + File.separator + "variable" + File.separator + "fiz" + File.separator + "f3");
		assertThat(f3.exists()).isTrue();
		assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		ctx.close();
	}

	@Test
	public void testStreaming() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StreamingConfig.class);
		StandardConfig config = ctx.getBean(StandardConfig.class);
		ctx.getBean(StandardIntegrationFlow.class).stop();
		assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		List<Integer> sfCalls = config.sessionSources.stream().limit(17).collect(Collectors.toList());
		// there's an extra getSession() with this adapter in listFiles
		assertThat(sfCalls).containsExactly(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 1, 1, 2, 2, 3);
		assertThat(ctx.getBean("files", QueueChannel.class).getQueueSize()).isEqualTo(3);
		ctx.close();
	}


	@Configuration
	@EnableIntegration
	public static class StandardConfig {

		private final CountDownLatch latch = new CountDownLatch(17);

		final List<Integer> sessionSources = new CopyOnWriteArrayList<>();

		@Bean
		public SessionFactory<FTPFile> factory1() {
			return new CachingSessionFactory<FTPFile>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					StandardConfig.this.sessionSources.add(1);
					StandardConfig.this.latch.countDown();
					return super.getSession();
				}

			};
		}

		@Bean
		public SessionFactory<FTPFile> factory2() {
			return new CachingSessionFactory<FTPFile>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					StandardConfig.this.sessionSources.add(2);
					StandardConfig.this.latch.countDown();
					return super.getSession();
				}

			};
		}

		@Bean
		public SessionFactory<FTPFile> factory3() {
			return new CachingSessionFactory<FTPFile>(rawSessionFactory()) {

				@Override
				public Session<FTPFile> getSession() {
					StandardConfig.this.sessionSources.add(3);
					StandardConfig.this.latch.countDown();
					return super.getSession();
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
			return new File(tmpDir, "standard");
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
			return new File(tmpDir, "fair");
		}
	}

	@Configuration
	public static class VariableLocalConfig extends StandardConfig {

		@Override
		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Ftp.inboundAdapter(sf())
							.filter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "rotate"))
							.localDirectory(new File(tmpDir, "variable"))
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

}
