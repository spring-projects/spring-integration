/*
 * Copyright 2002-2014 the original author or authors.
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is capable of mapping its parameters to a message
 * or message payload. These method-level annotations are detected by the
 * {@link org.springframework.integration.gateway.GatewayProxyFactoryBean}
 * where the annotation attributes can override the default channel settings.
 *
 * <p>A method annotated with @Gateway may accept a single non-annotated
 * parameter of type {@link org.springframework.messaging.Message}
 * or of the intended Message payload type. Method parameters may be mapped
 * to individual Message header values by using the {@link Header @Header}
 * parameter annotation. Alternatively, to pass the entire Message headers
 * map, a Map-typed parameter may be annotated with {@link Headers}.
 *
 * <p>Return values from the annotated method may be of any type. If the
 * declared return value is not a Message, the reply Message's payload will be
 * returned and any type conversion as supported by Spring's
 * {@link org.springframework.beans.SimpleTypeConverter} will be applied to
 * the return value if necessary.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Gateway {

	String requestChannel() default "";

	String replyChannel() default "";

	long requestTimeout() default Long.MIN_VALUE;

	long replyTimeout() default Long.MIN_VALUE;

	String payloadExpression() default "";

	GatewayHeader[] headers() default {};

}
