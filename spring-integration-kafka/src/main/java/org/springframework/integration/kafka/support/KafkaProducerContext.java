/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.kafka.support;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Artem Bilan
 * @author Marius Bogoevici
 * @since 0.5
 */
public class KafkaProducerContext implements SmartLifecycle, NamedComponent, BeanNameAware {

	private static final Log logger = LogFactory.getLog(KafkaProducerContext.class);

	private final AtomicBoolean running = new AtomicBoolean();

	private volatile Map<String, ProducerConfiguration<?, ?>> producerConfigurations;

	private volatile ProducerConfiguration<?, ?> theProducerConfiguration;

	private String beanName;

	private int phase = 0;

	private boolean autoStartup = true;

	public ProducerConfiguration<?, ?> getTopicConfiguration(final String topic) {
		if (this.theProducerConfiguration != null) {
			if (topic.matches(this.theProducerConfiguration.getProducerMetadata().getTopic())) {
				return this.theProducerConfiguration;
			}
		}

		Collection<ProducerConfiguration<?, ?>> topics = this.producerConfigurations.values();

		for (final ProducerConfiguration<?, ?> producerConfiguration : topics) {
			if (topic.matches(producerConfiguration.getProducerMetadata().getTopic())) {
				return producerConfiguration;
			}
		}
		return null;
	}

	public Map<String, ProducerConfiguration<?, ?>> getProducerConfigurations() {
		return this.producerConfigurations;
	}

	public void setProducerConfigurations(Map<String, ProducerConfiguration<?, ?>> producerConfigurations) {
		this.producerConfigurations = producerConfigurations;
		if (this.producerConfigurations.size() == 1) {
			this.theProducerConfiguration = this.producerConfigurations.values().iterator().next();
		}
	}

	/**
	 * @return the component type.
	 * @since 1.0
	 */
	@Override
	public String getComponentType() {
		return "kafka:producer-context";
	}

	/**
	 * @param name the bean name.
	 * @since 1.0
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * @param phase the phase to set.
	 * @see SmartLifecycle
	 * @since 1.0
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * @param autoStartup the autoStartup to set.
	 * @see SmartLifecycle
	 * @since 1.0
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * @return the component name.
	 * @since 1.0
	 */
	@Override
	public String getComponentName() {
		return this.beanName;
	}

	protected void doStart() {
	}

	protected void doStop() {
		if (this.producerConfigurations != null) {
			for (ProducerConfiguration<?, ?> producerConfiguration : this.producerConfigurations.values()) {
				producerConfiguration.stop();
			}
		}
	}

	@Override
	public final void start() {
		if (this.running.compareAndSet(false, true)) {
			doStart();
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug(getComponentType() + ":" + getComponentName() + " is already running");
			}
		}
	}

	@Override
	public final void stop() {
		if (this.running.compareAndSet(true, false)) {
			doStop();
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug(getComponentType() + ":" + getComponentName() + " is not running");
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	public Future<RecordMetadata> send(String topic, Object messageKey, Object messagePayload) {
		return send(topic, null, messageKey, messagePayload);
	}

	public Future<RecordMetadata> send(String topic, Integer partition, Object messageKey, Object messagePayload) {
		if (!this.running.get()) {
			start();
		}

		// only try to look up for a producer configuration if the topic is passed as argument
		// if no topic is configured, then we'll fall back to the default if a single
		// producer configuration is available
		ProducerConfiguration<?, ?> producerConfiguration =
				StringUtils.hasText(topic) ? getTopicConfiguration(topic) : null;

		if (producerConfiguration != null) {
			return producerConfiguration.convertAndSend(topic, partition, messageKey, messagePayload);
		}
		// if there is a single producer configuration then use that config to send message.
		else if (this.theProducerConfiguration != null) {
			return this.theProducerConfiguration.convertAndSend(topic, partition, messageKey, messagePayload);
		}
		else {
			throw new IllegalStateException("Could not send messages as there are multiple producer configurations " +
					"with no topic information found from the message header.");
		}
	}

}
