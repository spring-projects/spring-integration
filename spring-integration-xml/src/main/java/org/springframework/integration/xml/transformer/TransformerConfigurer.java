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

package org.springframework.integration.xml.transformer;

import javax.xml.transform.Transformer;

import org.springframework.integration.core.Message;

/**
 * Allows customization of the transformer based on the received message prior to transformation.
 * 
 * @author Jonas Partner
 */
public interface TransformerConfigurer {

	/**
	 * Callback method called by XSLT transformer implementations after transformer is created.
	 */
	public void configureTransformer(Message<?> message, Transformer transformer);

}
