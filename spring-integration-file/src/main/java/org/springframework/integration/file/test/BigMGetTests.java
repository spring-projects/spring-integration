/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.file.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * Created this test because a customer reported hanging with large mget.
 *
 * Could not reproduce; code is checked in, but test is @Ignored.
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class BigMGetTests {

	protected static final int FILES = 600;

	@Autowired
	private MessageChannel inbound;

	@Autowired
	private PollableChannel resultChannel;

	protected Message<List<File>> mgetManyFiles() throws Exception {
		byte[] buff = new byte[150];
		for (int i = 0; i < FILES; i++) {
			File f = new File("/tmp/bigmget/file" + i);
			f.createNewFile();
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(buff);
			fos.close();
		}
		inbound.send(new GenericMessage<String>("/tmp/bigmget/f*"));
		for (int i = 0; i < FILES; i++) {
			new File("/tmp/bigmget/file" + i).delete();
			new File("/tmp/out/file" + i).delete();
		}
		@SuppressWarnings("unchecked")
		Message<List<File>> results = (Message<List<File>>) resultChannel.receive(600000);
		return results;
	}
}
