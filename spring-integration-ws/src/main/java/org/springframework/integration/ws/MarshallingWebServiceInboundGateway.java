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

package org.springframework.integration.ws;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 1.0.2
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ws.inbound.MarshallingWebServiceInboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MarshallingWebServiceInboundGateway
		extends org.springframework.integration.ws.inbound.MarshallingWebServiceInboundGateway {

	/**
	 * Create a new <code>MarshallingWebServiceInboundGateway</code>.
	 * The {@link Marshaller} and {@link Unmarshaller} must be injected using properties.
	 */
	public MarshallingWebServiceInboundGateway() {
		super();
	}

	/**
	 * Create a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller.
	 * The Marshaller must also implement {@link Unmarshaller}, since it is used for both marshalling and
	 * unmarshalling.
	 * <p>
	 * Note that all {@link Marshaller} implementations in Spring-OXM also implement the {@link Unmarshaller}
	 * interface, so you can safely use this constructor for any of those implementations.
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when <code>marshaller</code> does not implement {@link Unmarshaller}
	 * @see #MarshallingWebServiceInboundGateway(Marshaller, Unmarshaller)
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller) {
		super(marshaller);
	}

	/**
	 * Create a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller and unmarshaller.
	 * @param marshaller The marshaller.
	 * @param unmarshaller The unmarshaller.
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller, Unmarshaller unmarshaller) {
		super(marshaller, unmarshaller);
	}

}
