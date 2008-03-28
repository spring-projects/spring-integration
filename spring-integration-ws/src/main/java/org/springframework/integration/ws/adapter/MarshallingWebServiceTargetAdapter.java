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

package org.springframework.integration.ws.adapter;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.client.core.WebServiceMessageCallback;

/**
 * A marshalling Web Service target channel adapter.
 * 
 * @author Mark Fisher
 * @see Marshaller
 * @see Unmarshaller
 */
public class MarshallingWebServiceTargetAdapter extends AbstractWebServiceTargetAdapter {

	public MarshallingWebServiceTargetAdapter(String uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		super(uri);
		Assert.notNull(marshaller, "marshaller must not be null");
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		this.getWebServiceTemplate().setMarshaller(marshaller);
		this.getWebServiceTemplate().setUnmarshaller(unmarshaller);
	}

	public MarshallingWebServiceTargetAdapter(String uri, Marshaller marshaller) {
		super(uri);
		Assert.notNull(marshaller, "marshaller must not be null");
		Assert.isTrue(marshaller instanceof Unmarshaller,
				"Marshaller [" + marshaller + "] does not implement the Unmarshaller interface. " +
				"Please set an Unmarshaller explicitly by using the " + this.getClass().getName() +
				"(String uri, Marshaller marshaller, Unmarshaller unmarshaller) constructor.");
		this.getWebServiceTemplate().setMarshaller(marshaller);
		this.getWebServiceTemplate().setUnmarshaller((Unmarshaller) marshaller);
	}


	@Override
	protected Object doHandle(Object requestPayload, WebServiceMessageCallback requestCallback) {
		return this.getWebServiceTemplate().marshalSendAndReceive(requestPayload, requestCallback);
	}

}
