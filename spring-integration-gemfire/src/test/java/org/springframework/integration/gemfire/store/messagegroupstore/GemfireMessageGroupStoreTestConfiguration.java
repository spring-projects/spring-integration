/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store.messagegroupstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.gemfire.store.KeyValueMessageGroup;
import org.springframework.integration.gemfire.store.KeyValueMessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * Our aggregator needs a
 * {@link org.springframework.integration.gemfire.store.KeyValueMessageGroupStore}
 * . This handles configuration of the ancillary objects.
 * 
 * @author Josh Long
 * @since 2.1
 */
@Configuration
public class GemfireMessageGroupStoreTestConfiguration {

	public static List<String> LIST_OF_STRINGS = Arrays.asList("1,2,3,4,5".split(","));

	static private Log log = LogFactory.getLog(GemfireMessageGroupStoreTestConfiguration.class);

	@Value("${correlation-header}")
	private String correlationHeader;

	@Bean
	public Cache cache() throws Throwable {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		cacheFactoryBean.afterPropertiesSet();
		return cacheFactoryBean.getObject();
	}

	@Bean
	public Region<Object, KeyValueMessageGroup> messageGroupRegion() throws Throwable {
		RegionFactoryBean<Object, KeyValueMessageGroup> regionFactoryBean = new RegionFactoryBean<Object, KeyValueMessageGroup>();
		regionFactoryBean.setName("messageGroupRegion");
		regionFactoryBean.setCache(cache());
		regionFactoryBean.afterPropertiesSet();
		return regionFactoryBean.getObject();
	}

	@Bean
	public Region<String, Message<?>> unmarkedRegion() throws Throwable {
		RegionFactoryBean<String, Message<?>> regionFactoryBean = new RegionFactoryBean<String, Message<?>>();
		regionFactoryBean.setName("unmarkedRegion");
		regionFactoryBean.setCache(cache());
		regionFactoryBean.afterPropertiesSet();
		return regionFactoryBean.getObject();
	}

	@Bean
	public Region<String, Message<?>> markedRegion() throws Throwable {
		RegionFactoryBean<String, Message<?>> regionFactoryBean = new RegionFactoryBean<String, Message<?>>();
		regionFactoryBean.setName("markedRegion");
		regionFactoryBean.setCache(cache());
		regionFactoryBean.afterPropertiesSet();
		return regionFactoryBean.getObject();
	}

	@Bean(name = "messageGroupStoreActivator")
	public FakeMessageConsumer serviceActivator() {
		return new FakeMessageConsumer();
	}

	@Bean
	public ReleaseStrategy releaseStrategy() {
		return new SequenceSizeReleaseStrategy(false);
	}

	@Bean
	public CorrelationStrategy correlationStrategy() {
		return new HeaderAttributeCorrelationStrategy(this.correlationHeader);
	}

	@Bean
	public KeyValueMessageGroupStore gemfireMessageGroupStore() throws Throwable {
		return new KeyValueMessageGroupStore(messageGroupRegion(), markedRegion(), unmarkedRegion());
	}

	@Bean
	public FakeMessageProducer producer() {
		return new FakeMessageProducer();
	}

	static public class FakeMessageConsumer {

		private List<Collection<Object>> batches = new ArrayList<Collection<Object>>();

		public List<Collection<Object>> getBatches() {
			return this.batches;
		}

		@ServiceActivator
		public void activateAsMessagesArriveInBatches(Message<Collection<Object>> msg) throws Throwable {
			Collection<Object> payloads = msg.getPayload();
			batches.add(payloads);

			if (log.isDebugEnabled()) {
				log.debug(payloads);
			}

		}

	}

	static public class FakeMessageProducer implements InitializingBean, SmartLifecycle {
		public boolean isAutoStartup() {
			return false;
		}

		public void stop(Runnable callback) {
			stop();
			callback.run();
		}

		public int getPhase() {
			return 0;
		}

		@Autowired
		@Qualifier("i")
		private MessageChannel messageChannel;

		private MessagingTemplate messagingTemplate = new MessagingTemplate();

		private volatile boolean running = false;

		@Value("${correlation-header}")
		private String correlationHeader;

		public void sendManyMessages(int correlationValue, Collection<String> lines) throws Throwable {
			Assert.notNull(lines, "the collection must be non-null");
			Assert.notEmpty(lines, "the collection must not be empty");
			int ctr = 0;
			int size = lines.size();
			for (String l : lines) {
				Message<?> msg = MessageBuilder.withPayload(l).setCorrelationId(this.correlationHeader)
						.setHeader(this.correlationHeader, correlationValue).setSequenceNumber(++ctr)
						.setSequenceSize(size).build();
				this.messagingTemplate.send(msg);
			}
		}

		public void afterPropertiesSet() throws Exception {
			this.messagingTemplate.setDefaultChannel(this.messageChannel);
		}

		public void start() {
			running = true;
			for (int i = 0; i < 10; i++) {
				try {
					sendManyMessages(i, LIST_OF_STRINGS);
				}
				catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			}

		}

		public void stop() {
			running = false;
		}

		public boolean isRunning() {
			return running;
		}

	}
}