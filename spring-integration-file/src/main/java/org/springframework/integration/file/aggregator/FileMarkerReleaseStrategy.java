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

import java.util.Collection;

import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * A {@link ReleaseStrategy} which makes a decision based on the presence of
 * {@link org.springframework.integration.file.splitter.FileSplitter.FileMarker.Mark#END}
 * message in the group and its {@link org.springframework.integration.file.FileHeaders#LINE_COUNT} header.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public class FileMarkerReleaseStrategy implements ReleaseStrategy {

	@Override public boolean canRelease(MessageGroup group) {
		Collection<Message<?>> messages = group.getMessages();

		return messages
				.stream()
				.filter((message) ->
						FileSplitter.FileMarker.Mark.END.name()
								.equals(message.getHeaders().get(FileHeaders.MARKER)))
				.findAny()
				.map((message) -> message.getHeaders().get(FileHeaders.LINE_COUNT, Long.class))
				.map((lineCount) -> lineCount == messages.size() - 2)
				.orElse(false);

	}

}
