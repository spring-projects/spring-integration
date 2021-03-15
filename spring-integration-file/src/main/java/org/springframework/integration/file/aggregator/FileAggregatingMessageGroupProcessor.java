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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.integration.aggregator.AbstractAggregatingMessageGroupProcessor;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * An {@link AbstractAggregatingMessageGroupProcessor} implementation for file content collecting
 * previously splitted by the {@link org.springframework.integration.file.splitter.FileSplitter}
 * with the {@code markers} option turned on.
 * <p>
 * If no file markers present in the {@link MessageGroup}, then behavior of this processor is
 * similar to the {@link org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor}.
 * <p>
 * When no file content (only file markers are grouped), this processor emits an empty {@link ArrayList}.
 * Note: with no file content and markers turned off,
 * the {@link org.springframework.integration.file.splitter.FileSplitter} doesn't emit any messages
 * for possible aggregation downstream.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public class FileAggregatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	@Override
	protected Object aggregatePayloads(MessageGroup group, Map<String, Object> defaultHeaders) {
		Collection<Message<?>> messages = group.getMessages();
		List<Object> payloads = new ArrayList<>(messages.size() - 2);
		for (Message<?> message : messages) {
			if (!message.getHeaders().containsKey(FileHeaders.MARKER)) {
				payloads.add(message.getPayload());
			}
		}
		return payloads;
	}

}
