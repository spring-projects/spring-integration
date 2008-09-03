/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.stereotype.Component;

/**
 * Indicates that a method is capable of serving as a message channel.
 * If the method accepts no arguments but does define a non-void return
 * type, an inbound Channel Adapter will be created. If the method does
 * accept an argument and has a void return, an outbound Channel Adapter
 * will be created. If the method does not conform to either contract,
 * an Exception will be thrown.
 * 
 * @author Mark Fisher
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Component
public @interface ChannelAdapter {

	/**
	 * The name of the channel being adapted. If the channel
	 * name is not resolvable, a new channel will be created.
	 */
	String value();

}
