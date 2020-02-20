/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.ws.dsl;

import org.springframework.integration.ws.SimpleWebServiceInboundGateway;

/**
 * The spec for a {@link SimpleWebServiceInboundGateway}.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class SimpleWsInboundGatewaySpec extends BaseWsInboundGatewaySpec<SimpleWsInboundGatewaySpec,
	SimpleWebServiceInboundGateway> {

	protected boolean extractPayload = true; // NOSONAR

	/**
	 * Specify true to extract the payloadSource from the request or use
	 * the entire request as the payload; default true.
	 * @param extract true to extract.
	 * @return the spec.
	 */
	public SimpleWsInboundGatewaySpec extractPayload(boolean extract) {
		this.extractPayload = extract;
		return this;
	}

	@Override
	protected SimpleWebServiceInboundGateway create() {
		SimpleWebServiceInboundGateway gateway = new SimpleWebServiceInboundGateway();
		gateway.setExtractPayload(this.extractPayload);
		return gateway;
	}

}
