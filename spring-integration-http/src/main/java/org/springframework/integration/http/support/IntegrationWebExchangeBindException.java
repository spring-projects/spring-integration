/*
 * Copyright 2019-2024 the original author or authors.
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

package org.springframework.integration.http.support;

import java.util.Objects;

import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

/**
 * A {@link WebExchangeBindException} extension for validation error with a failed
 * message context.
 * We can't rely on the default {@link WebExchangeBindException} behavior since
 * there is no POJO method invocation in Spring Integration Web endpoint implementations.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SuppressWarnings("serial")
public class IntegrationWebExchangeBindException extends WebExchangeBindException {

	private final String endpointId;

	private final Object failedPayload;

	public IntegrationWebExchangeBindException(String endpointId, Object failedPayload,
			BindingResult bindingResult) {

		super(null, bindingResult); // NOSONAR - we ignore a MethodParameter in favor of payload and endpoint context
		this.endpointId = endpointId;
		this.failedPayload = failedPayload;
	}

	@Override
	public String getMessage() {
		BindingResult bindingResult = getBindingResult();
		StringBuilder sb = new StringBuilder("Validation failed for payload ")
				.append(this.failedPayload)
				.append(" in the endpoint ")
				.append(this.endpointId)
				.append(", with ")
				.append(bindingResult.getErrorCount())
				.append(" error(s): ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof IntegrationWebExchangeBindException)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		IntegrationWebExchangeBindException that = (IntegrationWebExchangeBindException) o;
		return Objects.equals(this.endpointId, that.endpointId) &&
				Objects.equals(this.failedPayload, that.failedPayload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.endpointId, this.failedPayload);
	}

}
