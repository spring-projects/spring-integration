/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.context;

/**
 * Interface for components that wish to be considered for
 * an orderly shutdown using management interfaces. beforeShutdown()
 * will be called before schedulers, executors etc, are stopped.
 * afterShutdown() is called after the shutdown delay.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface OrderlyShutdownCapable {

	/**
	 * Called before shutdown begins. Implementations should
	 * stop accepting new messages. Can optionally return the
	 * number of active messages in process.
	 * @return The number of active messages if available.
	 */
	int beforeShutdown();

	/**
	 * Called after normal shutdown of schedulers, executors etc,
	 * and after the shutdown delay has elapsed, but before any
	 * forced shutdown of any remaining active scheduler/executor
	 * threads.Can optionally return the number of active messages
	 * still in process.
	 * @return The number of active messages if available.
	 */
	int afterShutdown();

}
