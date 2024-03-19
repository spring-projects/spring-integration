/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.integration.nats.exception;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/** Base class for the NATS exceptions. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsException extends NestedRuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructor that takes a message.
   *
   * @param msg the detail message
   */
  public NatsException(final String msg) {
    super(msg);
  }

  /**
   * Constructor that takes a message and a root cause.
   *
   * @param msg the detail message
   * @param cause the cause of the exception.
   */
  public NatsException(final String msg, @Nullable final Throwable cause) {
    super(msg, cause);
  }
}
