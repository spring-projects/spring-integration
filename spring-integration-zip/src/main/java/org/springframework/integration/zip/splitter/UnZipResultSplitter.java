/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.zip.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.zip.ZipHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 * @author Andriy Kryvtsun
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class UnZipResultSplitter extends AbstractMessageSplitter {

	@Override
	@SuppressWarnings("unchecked")
	protected Object splitMessage(Message<?> message) {
		Assert.state(message.getPayload() instanceof Map,
				"The UnZipResultSplitter supports only Map<String, Object> payload");
		Map<String, Object> unzippedEntries = (Map<String, Object>) message.getPayload();
		MessageHeaders headers = message.getHeaders();

		List<MessageBuilder<Object>> messageBuilders = new ArrayList<>(unzippedEntries.size());

		for (Map.Entry<String, Object> entry : unzippedEntries.entrySet()) {
			String path = FilenameUtils.getPath(entry.getKey());
			String filename = FilenameUtils.getName(entry.getKey());
			MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(entry.getValue())
					.setHeader(FileHeaders.FILENAME, filename)
					.setHeader(ZipHeaders.ZIP_ENTRY_PATH, path)
					.copyHeadersIfAbsent(headers);
			messageBuilders.add(messageBuilder);
		}

		return messageBuilders;
	}

}
