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

import java.util.function.BiFunction;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.GroupConditionProvider;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * A convenient component to utilize
 * a {@link org.springframework.integration.file.splitter.FileSplitter.FileMarker}-based aggregation logic.
 * Implements all three {@link CorrelationStrategy}, {@link ReleaseStrategy} and {@link MessageGroupProcessor}
 * for runtime optimization.
 * Delegates to {@link HeaderAttributeCorrelationStrategy} with {@link FileHeaders#FILENAME} attribute,
 * {@link FileMarkerReleaseStrategy} and {@link FileAggregatingMessageGroupProcessor}, respectively.
 * <p>
 * The default {@link org.springframework.integration.file.splitter.FileSplitter} behavior
 * with markers enabled is about do not provide a sequence details
 * headers, therefore correlation in this aggregator implementation is done by the {@link FileHeaders#FILENAME}
 * header which is still populated by the {@link org.springframework.integration.file.splitter.FileSplitter}
 * for each line emitted, including {@link org.springframework.integration.file.splitter.FileSplitter.FileMarker} messages.
 * <p>
 * If default behavior of this component does not satisfy the target logic, it is recommended to
 * configure an aggregator with individual strategies.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public class FileAggregator implements CorrelationStrategy, ReleaseStrategy, GroupConditionProvider,
		MessageGroupProcessor, BeanFactoryAware {

	private final CorrelationStrategy correlationStrategy = new HeaderAttributeCorrelationStrategy(FileHeaders.FILENAME);

	private final FileMarkerReleaseStrategy releaseStrategy = new FileMarkerReleaseStrategy();

	private final FileAggregatingMessageGroupProcessor groupProcessor = new FileAggregatingMessageGroupProcessor();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.groupProcessor.setBeanFactory(beanFactory);
	}

	@Override
	public Object getCorrelationKey(Message<?> message) {
		return this.correlationStrategy.getCorrelationKey(message);
	}

	@Override
	public boolean canRelease(MessageGroup group) {
		return this.releaseStrategy.canRelease(group);
	}

	@Override
	public BiFunction<Message<?>, String, String> getGroupConditionSupplier() {
		return this.releaseStrategy.getGroupConditionSupplier();
	}

	@Override
	public Object processMessageGroup(MessageGroup group) {
		return this.groupProcessor.processMessageGroup(group);
	}

}
