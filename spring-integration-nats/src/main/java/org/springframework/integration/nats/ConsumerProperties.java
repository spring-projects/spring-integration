/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.time.Duration;

import org.springframework.lang.NonNull;

/**
 * Class to set up the configuration properties for NATS consumer
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public class ConsumerProperties {

	private static final int DEFAULT_PULL_BATCH_SIZE = 50;

	@NonNull
	private static final Duration DEFAULT_CONSUMER_MAX_WAIT = Duration.ofSeconds(30);

	@NonNull
	private String stream;

	@NonNull
	private String subject;

	@NonNull
	private String consumerName;

	@NonNull
	private String queueGroup;

	private int pullBatchSize = DEFAULT_PULL_BATCH_SIZE;

	@NonNull
	private Duration consumerMaxWait = DEFAULT_CONSUMER_MAX_WAIT;

	/**
	 * Contructs Jetstream Consumer Properties with configured subject, consumerName and queueGroup
	 *
	 * @param pStream stream name
	 * @param pSubject subject name
	 * @param pConsumerName consumer name
	 * @param pQueueGroup queue group
	 */
	public ConsumerProperties(
			@NonNull final String pStream,
			@NonNull final String pSubject,
			@NonNull final String pConsumerName,
			@NonNull final String pQueueGroup) {
		this.stream = pStream;
		this.subject = pSubject;
		this.consumerName = pConsumerName;
		this.queueGroup = pQueueGroup;
	}

	/**
	 * Constructs Core Consumer Properties with configured subject
	 *
	 * @param pSubject - TODO: Add description
	 */
	public ConsumerProperties(@NonNull final String pSubject) {
		this.subject = pSubject;
	}

	@NonNull
	public String getStream() {
		return this.stream;
	}

	public void setStream(@NonNull final String pStream) {
		this.stream = pStream;
	}

	@NonNull
	public String getSubject() {
		return this.subject;
	}

	public void setSubject(@NonNull final String pSubject) {
		this.subject = pSubject;
	}

	@NonNull
	public String getConsumerName() {
		return this.consumerName;
	}

	public void setConsumerName(@NonNull final String pConsumerName) {
		this.consumerName = pConsumerName;
	}

	@NonNull
	public String getQueueGroup() {
		return this.queueGroup;
	}

	public void setQueueGroup(@NonNull final String pQueueGroup) {
		this.queueGroup = pQueueGroup;
	}

	public int getPullBatchSize() {
		return this.pullBatchSize;
	}

	public void setPullBatchSize(final int pullBatchSizeValue) {
		this.pullBatchSize = pullBatchSizeValue;
	}

	@NonNull
	public Duration getConsumerMaxWait() {
		return this.consumerMaxWait;
	}

	public void setConsumerMaxWait(@NonNull final Duration pConsumerMaxWait) {
		this.consumerMaxWait = pConsumerMaxWait;
	}
}
