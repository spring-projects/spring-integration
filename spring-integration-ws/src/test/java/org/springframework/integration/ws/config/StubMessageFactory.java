/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.ws.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.pox.dom.DomPoxMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource(new InputStreamReader(inputStream));
			Document document = builder.parse(is);
			return new DomPoxMessage(document, transformer, "text/xml");
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

}
