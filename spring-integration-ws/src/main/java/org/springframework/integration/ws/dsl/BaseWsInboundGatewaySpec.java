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

import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.ws.AbstractWebServiceInboundGateway;
import org.springframework.integration.ws.SoapHeaderMapper;

/**
 * Base {@link MessagingGatewaySpec} for web services.
 *
 * @param <S> the target {@link BaseWsInboundGatewaySpec} implementation type.
 * @param <E> the target {@link AbstractWebServiceInboundGateway} implementation type.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public abstract class BaseWsInboundGatewaySpec<
		S extends BaseWsInboundGatewaySpec<S, E>, E extends AbstractWebServiceInboundGateway>
	extends MessagingGatewaySpec<S, E> {

	/**
	 * Construct an instance.
	 */
	protected BaseWsInboundGatewaySpec() {
		super(null);
	}

	/**
	 * Configure the header mapper.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public S headerMapper(SoapHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	@Override
	protected E doGet() {
		return assemble(create());
	}

	protected abstract E create();

	protected E assemble(E gateway) {
		return gateway;
	}

}
