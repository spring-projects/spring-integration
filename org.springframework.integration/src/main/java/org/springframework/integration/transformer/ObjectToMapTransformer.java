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
package org.springframework.integration.transformer;

import java.util.Map;

/**
 * Will transform an object graph into a flat Map where keys are valid SpEL expressions
 * and values are of java.lang.* type. This means that this transformer will recursively navigate 
 * through the Object graph until the value could be java.lang.*
 * It supports Collections, Maps and Arrays which means it will flatten Object's attributes that are defined as such:<br>
 * 
 * private Map<String, Map<String, Object>> testMapInMapData;<br>
 * private List<String> departments;<br>
 * private String[] akaNames;<br>
 * private Map<String, List<String>> mapWithListData;<br>
 * 
 * The resulting Map structure will look similar to this:<br>
 * 
 * person.address.mapWithListData['mapWithListTestData'][1]=blah<br>
 * departments[0]=HR<br>
 * person.lname=Case
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ObjectToMapTransformer extends AbstractPayloadTransformer<Object, Map<?,?>> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.transformer.AbstractPayloadTransformer#transformPayload(java.lang.Object)
	 */
	protected Map<String, Object> transformPayload(Object payload) throws Exception {
		ObjectToSpelMapBuilder builder = new ObjectToSpelMapBuilder();
		return builder.buildSpelMap(payload);
	}

}
