/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class Jsr223RefreshTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-1");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-0");
		assertThat(replyChannel.receive(0)).isNull();
	}

	public static class ResourceEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			super.setValue(new CycleResource());
		}

	}

	private static class CycleResource extends AbstractResource {

		private int count = -1;

		private final String[] scripts = {"\"ruby-#{payload}-0\"", "\"ruby-#{payload}-1\""};

		CycleResource() {
			super();
		}

		@Override
		public String getDescription() {
			return "CycleResource";
		}

		@Override
		public String getFilename() throws IllegalStateException {
			return "CycleResource";
		}

		@Override
		public long lastModified() {
			return -1;
		}

		@Override
		public InputStream getInputStream() {
			if (++count > scripts.length - 1) {
				count = 0;
			}
			return new ByteArrayInputStream(scripts[count].getBytes());
		}

	}

}
