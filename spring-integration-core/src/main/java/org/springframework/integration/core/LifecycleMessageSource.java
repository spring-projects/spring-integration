/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.core;

import org.springframework.context.Lifecycle;

/**
 * The {@link Lifecycle} marker interface for backward compatibility.
 * Will be deprecated in 4.2 and removed in the future releases.
 *
 * @author Artem Bilan
 * @since 4.0.6
 */
public interface LifecycleMessageSource<T> extends MessageSource<T>, Lifecycle {
}
