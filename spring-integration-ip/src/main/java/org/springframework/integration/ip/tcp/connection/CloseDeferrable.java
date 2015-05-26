/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection;

/**
 * Temporary interface on the {@code CachingClientConnectionFactory} enabling the gateway
 * to defer the implicit close after onMessage so the connection is not reused until after the
 * gateway has completely finished with it. Will be removed when INT-3654 is resolved, whereby
 * the gateway will be completely responsible for the close.
 *
 * @author Gary Russell
 * @since 4.1.5
 *
 */
public interface CloseDeferrable {

	/**
	 * Enable deferred closure.
	 * @param defer true to defer.
	 */
	void enableCloseDeferral(boolean defer);

	/**
	 * Close (release) the connection if deferred.
	 * @param connectionId the connection id.
	 */
	void closeDeferred(String connectionId);

}
