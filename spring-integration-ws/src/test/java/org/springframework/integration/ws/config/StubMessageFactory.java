/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.ws.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;

/**
 * @author Mark Fisher
 */
public class StubMessageFactory implements WebServiceMessageFactory {

	public WebServiceMessage createWebServiceMessage() {
		WebServiceMessage message = mock(WebServiceMessage.class);
		Source source = mock(Source.class);
		when(message.getPayloadSource()).thenReturn(source);
		return message;
	}

	public WebServiceMessage createWebServiceMessage(InputStream inputStream) throws IOException {
		return null;
	}

}
