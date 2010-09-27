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
package org.springframework.integration.http;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class HttpRequestExecutingMessageHandlerTests {

	@SuppressWarnings("unchecked")
	@Test
	public void validateMapWithObjectsConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		simpleMap.put("city", "Philadelphia");
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals("Philadelphia", multiValueMap.get("city").iterator().next());
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void validateMapWithArraysConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		simpleMap.put("city", new String[]{"Philadelphia", "Ambler"});
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals("Philadelphia", multiValueMap.get("city").get(0));
		assertEquals("Ambler", multiValueMap.get("city").get(1));
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void validateMapWithObjectArraysConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		City philadelphia = new City();
		City ambler = new City();
		simpleMap.put("city", new City[]{philadelphia, ambler});
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals(philadelphia, multiValueMap.get("city").get(0));
		assertEquals(ambler, multiValueMap.get("city").get(1));
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void validateMapWithObjectCollectionConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		City philadelphia = new City();
		City ambler = new City();
		List<City> cities = new LinkedList<City>();
		cities.add(philadelphia);
		cities.add(ambler);
		simpleMap.put("city", cities);
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals(philadelphia, multiValueMap.get("city").get(0));
		assertEquals(ambler, multiValueMap.get("city").get(1));
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}
	
	@Test
	public void validateMapWithNullValuesInCollectionConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		City philadelphia = new City();
		List<City> cities = new LinkedList<City>();
		cities.add(philadelphia);
		cities.add(null);
		simpleMap.put("city", cities);
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals(1, multiValueMap.get("city").size());
		assertEquals(philadelphia, multiValueMap.get("city").get(0));
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}
	@Test
	public void validateMapWithNullValuesInArrayConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<String, Object> simpleMap = new HashMap<String, Object>();
		
		City philadelphia = new City();
		City[] cities = new City[]{philadelphia, null};
		simpleMap.put("city", cities);
		simpleMap.put("state", "PA");
		MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) convertToMultipartValueMap.invoke(handler, simpleMap);
		assertEquals(1, multiValueMap.get("city").size());
		assertEquals(philadelphia, multiValueMap.get("city").get(0));
		assertEquals("PA", multiValueMap.get("state").iterator().next());
	}

	@Test(expected=InvocationTargetException.class)
	public void validateMapWithNonStrigKeysConversionToMvp() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("localhost");
		Method convertToMultipartValueMap = ReflectionUtils.findMethod(handler.getClass(), "convertToMultipartValueMap", Map.class);
		convertToMultipartValueMap.setAccessible(true);
		Map<Object, Object> simpleMap = new HashMap<Object, Object>();
		City philadelphia = new City();
		City ambler = new City();
		List<City> cities = new LinkedList<City>();
		cities.add(philadelphia);
		cities.add(ambler);
		simpleMap.put(1, cities);
		simpleMap.put(2, "PA");
		convertToMultipartValueMap.invoke(handler, simpleMap);
	}
	
	public static class City{}
}
