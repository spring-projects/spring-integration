/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used alongside an EIP annotation (and no {@code @Bean}), specifies the bean name of
 * the consumer bean with the handler bean being {@code id.handler} (for a consuming
 * endpoint) or {@code id.source} for a message source (e.g. inbound channel adapter).
 * <p>
 * When there is also a {@code @Bean} annotation, this is the name of the consumer or
 * source polling bean (the handler or source gets the normal {@code @Bean} name). When
 * using on a {@code MessageHandler @Bean}, it is recommended to name the bean
 * {@code foo.handler} when using {@code @EndpointId("foo"}. This will align with
 * conventions in the framework. Similarly, for a message source, use
 * {@code @Bean("bar.source"} and {@code @EndpointId("bar")}.
 * <p>
 * <b>This is not allowed if there are multiple EIP annotations on the same method.</b>
 *
 * @author Gary Russell
 *
 * @since 5.0.4
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EndpointId {

	/**
	 * @return the id
	 */
	String value();

}
