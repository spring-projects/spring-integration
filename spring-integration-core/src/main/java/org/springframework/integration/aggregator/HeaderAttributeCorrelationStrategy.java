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

package org.springframework.integration.aggregator;

import org.springframework.messaging.Message;

/**
 * Default implementation of {@link CorrelationStrategy}. Uses a header
 * attribute to determine the correlation key value.
 *
 * @author Marius Bogoevici
 */
public class HeaderAttributeCorrelationStrategy implements CorrelationStrategy {

    private String attributeName;


    public HeaderAttributeCorrelationStrategy(String attributeName) {
        this.attributeName = attributeName;
    }


    public Object getCorrelationKey(Message<?> message) {
        return message.getHeaders().get(this.attributeName);
    }

}
