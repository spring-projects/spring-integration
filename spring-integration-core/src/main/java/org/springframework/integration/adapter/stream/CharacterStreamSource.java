/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.adapter.PollableSource;

/**
 * A pollable source for text-based {@link InputStream InputStreams}.
 * 
 * @author Mark Fisher
 */
public class CharacterStreamSource implements PollableSource<String> {

	private BufferedReader reader;


	public CharacterStreamSource(InputStream stream) {
		this.reader = new BufferedReader(new InputStreamReader(stream));
	}

	public Collection<String> poll(int limit) {
		List<String> results = new ArrayList<String>();
		while (results.size() < limit) {
			try {
				boolean isReady = reader.ready();
				if (!isReady) {
					return results;
				}
				String line = reader.readLine();
				if (line == null) {
					return results;
				}
				results.add(line);
			}
			catch (IOException e) {
				throw new MessageDeliveryException("IO failure occurred in adapter", e);
			}
		}
		return results;
	}

}
