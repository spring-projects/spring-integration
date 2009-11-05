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
package org.springframework.intergration.osgi;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public interface IntegrationOSGiConstants {
	//(&(org.springframework.osgi.bean.name=controlBus-DEFAULT_CONTROL_GROUP))
	public final String DEFAULT_BUS_GROUP_NAME = "DEFAULT_CONTROL_GROUP";
	public final String OSGI_BEAN_NAME = "org.springframework.osgi.bean.name";
}
