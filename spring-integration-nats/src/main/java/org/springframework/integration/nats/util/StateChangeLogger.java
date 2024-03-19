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
package org.springframework.integration.nats.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logger wrapper is designed to reduce amount of logs in case when some value should be logged out.
 * The value should be logged only if it changes. Custom object need provide implementation of
 * equals() method.
 */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class StateChangeLogger<T> {

  private static final Log LOG = LogFactory.getLog(StateChangeLogger.class);
  // Value to be observed
  private T observable;
  // Last known state of variable
  private T lastState;

  public StateChangeLogger(T value) {
    this.observable = value;
    this.lastState = this.observable;
  }

  public boolean warnByStateChange(T newState, String msg) {
    boolean isChanged = false;
    if (newState != null && !newState.equals(lastState)) {
      this.lastState = newState;
      LOG.warn(msg);
      isChanged = true;
    }
    this.observable = newState;
    return isChanged;
  }
}
