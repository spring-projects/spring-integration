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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
	private volatile boolean flaten = true;
	
	public void setFlaten(boolean flaten) {
		this.flaten = flaten;
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> transformPayload(Object payload) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> result =
		        new ObjectMapper().readValue(mapper.writeValueAsString(payload), Map.class);
		
		if (flaten){
			result = this.flatenMap(result);
		}
		
		return result;
	}
	
	private Map<String, Object> flatenMap(Map<String,Object> result){
		Map<String,Object> resultMap = new HashMap<String, Object>();
		this.doFlaten("", result, resultMap);
		return resultMap;
	}
	
	@SuppressWarnings("unchecked")
	private void doFlaten(String propertyPrefix, Map<String,Object> inputMap, Map<String,Object> resultMap){
		if (StringUtils.hasText(propertyPrefix)){
			propertyPrefix = propertyPrefix + ".";
		}
		for (String key : inputMap.keySet()) {
			Object value = inputMap.get(key);
			if (value instanceof Map){
				this.doFlaten(propertyPrefix + key, (Map<String, Object>) value, resultMap);
			}
			else if (value instanceof List){
				this.doProcessList(propertyPrefix + key, (List<?>) value, resultMap);
			} 
			else if (value != null && value.getClass().isArray()){
				List<?> list =  CollectionUtils.arrayToList(value); 
				this.doProcessList(propertyPrefix + key, list, resultMap);
			}
			else {
				resultMap.put(propertyPrefix + key, value);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doProcessList(String propertyPrefix,  List<?> list, Map<String, Object> resultMap) {
		int counter = 0;
		for (Object element : list) {
			if (element instanceof Map){
				this.doFlaten(propertyPrefix + "[" + counter + "]", (Map<String, Object>) element, resultMap);
			} 
			else {
				resultMap.put(propertyPrefix + "[" + counter + "]", list.get(counter));
			}
			counter ++;
		}
	}

}
