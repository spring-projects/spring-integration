/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.groovy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyRefreshTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Test
	public void referencedScript() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertEquals("groovy-test-1", replyChannel.receive(0).getPayload());
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertEquals("groovy-test-0", replyChannel.receive(0).getPayload());
		assertNull(replyChannel.receive(0));
	}

	public static class ResourceEditor extends PropertyEditorSupport {
		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			super.setValue(new CycleResource());
		}
	}

	private static class CycleResource extends AbstractResource {

		private int count = -1;
		private String[] scripts = {"\"groovy-$payload-0\"", "\"groovy-$payload-1\""};

		public String getDescription() {
			return "CycleResource";
		}

		@Override
		public String getFilename() throws IllegalStateException {
			return "CycleResource";
		}

		@Override
		public long lastModified() throws IOException {
			return -1;
		}

		public InputStream getInputStream() throws IOException {
			if (++count>scripts.length-1) {
				count = 0;
			}
			return new ByteArrayInputStream(scripts[count].getBytes());
		}

	}
}
