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

package org.springframework.integration.ws.handler;

import java.net.URI;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceMessageCallback;

/**
 * A marshalling Web Service MessageHandler adapter.
 * 
 * @author Mark Fisher
 * @see Marshaller
 * @see Unmarshaller
 */
public class MarshallingWebServiceHandler extends AbstractWebServiceHandler {

	public MarshallingWebServiceHandler(URI uri, Marshaller marshaller, Unmarshaller unmarshaller, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
		Assert.notNull(marshaller, "marshaller must not be null");
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		this.getWebServiceTemplate().setMarshaller(marshaller);
		this.getWebServiceTemplate().setUnmarshaller(unmarshaller);
	}

	public MarshallingWebServiceHandler(URI uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(uri, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceHandler(URI uri, Marshaller marshaller, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
		Assert.notNull(marshaller, "marshaller must not be null");
		Assert.isInstanceOf(Unmarshaller.class, marshaller,
				"Marshaller [" + marshaller + "] does not implement the Unmarshaller interface. " +
				"Please set an Unmarshaller explicitly by using the " + this.getClass().getName() +
				"(String uri, Marshaller marshaller, Unmarshaller unmarshaller) constructor.");
		this.getWebServiceTemplate().setMarshaller(marshaller);
		this.getWebServiceTemplate().setUnmarshaller((Unmarshaller) marshaller);		
	}

	public MarshallingWebServiceHandler(URI uri, Marshaller marshaller) {
		this(uri, marshaller, (WebServiceMessageFactory) null);
	}


	@Override
	protected Object doHandle(Object requestPayload, WebServiceMessageCallback requestCallback) {
		return this.getWebServiceTemplate().marshalSendAndReceive(requestPayload, requestCallback);
	}

}
