/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.integration.samples.inbound;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.file.FileHeaders;
/**
 * Simple BundleActivator which will register ServiceListener which will listen for 
 * ApplicationContext published event. Once event is received, HelloWorldDemo will be executed.
 * 
 * @author Oleg Zhurakousky
 */
public interface InboundGateway {

	public void sendMessage(String message, @Header(FileHeaders.FILENAME)String fileName);
}
