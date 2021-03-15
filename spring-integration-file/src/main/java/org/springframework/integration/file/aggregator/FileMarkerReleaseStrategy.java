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
import org.springframework.messaging.MessageHeaders;

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

	@Override
	public boolean canRelease(MessageGroup group) {
		int size = group.size();
		if (size > 1) { // Need more than only a START marker
			Collection<Message<?>> messages = group.getMessages();
			for (Message<?> message : messages) {
				if (checkForEndMarker(size, message.getHeaders())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkForEndMarker(int groupSize, MessageHeaders headers) {
		if (FileSplitter.FileMarker.Mark.END.name().equals(headers.get(FileHeaders.MARKER))) {
			Long lineCount = headers.get(FileHeaders.LINE_COUNT, Long.class);
			return lineCount != null && lineCount == groupSize - 2;
		}
		else {
			return false;
		}
	}

}
