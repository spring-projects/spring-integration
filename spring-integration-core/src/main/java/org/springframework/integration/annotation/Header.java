/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a method parameter's value should be
 * retrieved from the message headers. The value of the annotation
 * can either be a header name (e.g., 'foo') or SpEL expression
 * (e.g., 'payload.getCustomerId()') which is quite useful when
 * the name of the header has to be dynamically computed. It also
 * provides an optional 'required' property which
 * specifies whether the attribute value must be available within
 * the header. The default value for 'required' is <code>true</code>.
 *
 * @author Mark Fisher
 *
 * @deprecated since 4.1 in favor of {@link org.springframework.messaging.handler.annotation.Header}.
 * Will be removed in a future release.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface Header {

	String value() default "";

	boolean required() default true;

}
