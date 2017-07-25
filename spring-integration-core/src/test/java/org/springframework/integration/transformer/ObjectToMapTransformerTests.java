/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Vikas Prasad
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ObjectToMapTransformerTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testObjectToSpelMapTransformer() throws IOException {
		Employee employee = this.buildEmployee();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());
		ExpressionParser parser = new SpelExpressionParser();

		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		Message<Employee> message = MessageBuilder.withPayload(employee).build();

		Message<?> transformedMessage = transformer.transform(message);
		Map<String, Object> transformedMap = (Map<String, Object>) transformedMessage.getPayload();
		assertNotNull(transformedMap);

		Object valueFromTheMap = null;
		Object valueFromExpression = null;
		Expression expression = null;

		expression = parser.parseExpression("departments[0]");
		valueFromTheMap = transformedMap.get("departments[0]");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.address.coordinates");
		valueFromTheMap = transformedMap.get("person.address.coordinates");
		valueFromExpression = expression.getValue(context, employee, Map.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.akaNames[0]");
		valueFromTheMap = transformedMap.get("person.akaNames[0]");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("testMapInMapData.internalMapA.bar");
		valueFromTheMap = transformedMap.get("testMapInMapData.internalMapA.bar");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("companyAddress.street");
		valueFromTheMap = transformedMap.get("companyAddress.street");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.lname");
		valueFromTheMap = transformedMap.get("person.lname");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.address.mapWithListData.mapWithListTestData[1]");
		valueFromTheMap = transformedMap.get("person.address.mapWithListData.mapWithListTestData[1]");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("companyAddress.city");
		valueFromTheMap = transformedMap.get("companyAddress.city");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.akaNames[2]");
		valueFromTheMap = transformedMap.get("person.akaNames[2]");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.child");
		valueFromTheMap = transformedMap.get("person.child");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertNull(valueFromTheMap);
		assertNull(valueFromExpression);

		expression = parser.parseExpression("testMapInMapData.internalMapA.foo");
		valueFromTheMap = transformedMap.get("testMapInMapData.internalMapA.foo");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.address.city");
		valueFromTheMap = transformedMap.get("person.address.city");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("companyAddress.coordinates.latitude[0]");
		valueFromTheMap = transformedMap.get("companyAddress.coordinates.latitude[0]");
		valueFromExpression = expression.getValue(context, employee, Integer.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("person.remarks[1].baz");
		valueFromTheMap = transformedMap.get("person.remarks[1].baz");
		valueFromExpression = expression.getValue(context, employee, String.class);
		assertEquals(valueFromTheMap, valueFromExpression);

		expression = parser.parseExpression("listOfDates[0][1]");
		valueFromTheMap = new Date((Long) transformedMap.get("listOfDates[0][1]"));
		valueFromExpression = expression.getValue(context, employee, Date.class);
		assertEquals(valueFromTheMap, valueFromExpression);
	}

	@Test(expected = MessageTransformationException.class)
	public void testObjectToSpelMapTransformerWithCycle() {
		Employee employee = this.buildEmployee();
		Child child = new Child();
		Person parent = employee.getPerson();
		parent.setChild(child);
		child.setParent(parent);
		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		Message<Employee> message = MessageBuilder.withPayload(employee).build();
		transformer.transform(message);
	}

	@Test
	public void testJacksonJSR310Support_PassInstantField_ReturnsMapWithOnlyOneEntryForInstantField() throws Exception {
		Person person = new Person();
		person.deathDate = Instant.now();

		Employee employee = new Employee();
		employee.setPerson(person);

		Map<String, Object> transformedMap = new ObjectToMapTransformer().transformPayload(employee);

		// If JSR310 support is enabled by calling findAndRegisterModules() on the Jackson mapper,
		// Instant field should not be broken. Thus the count should exactly be 1 here.
		assertEquals(1L, transformedMap.values().stream().filter(Objects::nonNull).count());
	}

	@Test
	public void testCustomMapperSupport_DisableTimestampFlag_SerializesDateAsString() throws Exception {
		Employee employee = buildEmployee();

		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		Map<String, Object> transformedMap =
				new ObjectToMapTransformer(new Jackson2JsonObjectMapper(customMapper))
						.transformPayload(employee);

		assertThat(transformedMap.get("listOfDates[0][0]"), instanceOf(String.class));

		assertThat(transformedMap.get("listOfDates[0][1]"), instanceOf(String.class));

		assertThat(transformedMap.get("listOfDates[1][0]"), instanceOf(String.class));

		assertThat(transformedMap.get("listOfDates[1][1]"), instanceOf(String.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Employee buildEmployee() {
		Address companyAddress = new Address();
		companyAddress.setCity("Philadelphia");
		companyAddress.setStreet("1123 Main");
		companyAddress.setZip("12345");

		Map<String, Long[]> coordinates = new HashMap<String, Long[]>();
		coordinates.put("latitude", new Long[] { (long) 1, (long) 5, (long) 13 });
		coordinates.put("longitude", new Long[] { (long) 156 });
		companyAddress.setCoordinates(coordinates);

		List<Date> datesA = new ArrayList<Date>();
		datesA.add(new Date(System.currentTimeMillis() + 10000));
		datesA.add(new Date(System.currentTimeMillis() + 20000));

		List<Date> datesB = new ArrayList<Date>();
		datesB.add(new Date(System.currentTimeMillis() + 30000));
		datesB.add(new Date(System.currentTimeMillis() + 40000));

		List<List<Date>> listOfDates = new ArrayList<List<Date>>();
		listOfDates.add(datesA);
		listOfDates.add(datesB);

		Employee employee = new Employee();
		employee.setCompanyName("ABC Inc.");
		employee.setCompanyAddress(companyAddress);
		employee.setListOfDates(listOfDates);
		ArrayList departments = new ArrayList();
		departments.add("HR");
		departments.add("IT");
		employee.setDepartments(departments);

		Person person = new Person();
		person.setFname("Justin");
		person.setLname("Case");
		person.setAkaNames("Hard", "Use", "Beer");
		person.setBirthDate(new Date());
		person.setAge(new BigDecimal(10));
		Address personAddress = new Address();
		personAddress.setCity("Philly");
		personAddress.setStreet("123 Main");
		List<String> listTestData = new ArrayList<String>();
		listTestData.add("hello");
		listTestData.add("blah");
		Map<String, List<String>> mapWithListTestData = new HashMap<String, List<String>>();
		mapWithListTestData.put("mapWithListTestData", listTestData);
		personAddress.setMapWithListData(mapWithListTestData);
		person.setAddress(personAddress);

		Map<String, Object> remarksA = new HashMap<String, Object>();
		Map<String, Object> remarksB = new HashMap<String, Object>();
		remarksA.put("foo", "foo");
		remarksA.put("bar", "bar");
		remarksB.put("baz", "baz");
		List<Map<String, Object>> remarks = new ArrayList<Map<String, Object>>();
		remarks.add(remarksA);
		remarks.add(remarksB);
		person.setRemarks(remarks);
		employee.setPerson(person);

		Map<String, Map<String, Object>> testMapData = new HashMap<String, Map<String, Object>>();

		Map<String, Object> internalMapA = new HashMap<String, Object>();
		internalMapA.put("foo", "foo");
		internalMapA.put("bar", "bar");
		Map<String, Object> internalMapB = new HashMap<String, Object>();
		internalMapB.put("baz", "baz");

		testMapData.put("internalMapA", internalMapA);
		testMapData.put("internalMapB", internalMapB);

		employee.setTestMapInMapData(testMapData);
		return employee;
	}

	public static class Employee {

		private List<String> departments;

		private List<List<Date>> listOfDates;

		private String companyName;

		private Person person;

		private Address companyAddress;

		private Map<String, Map<String, Object>> testMapInMapData;

		public List<List<Date>> getListOfDates() {
			return listOfDates;
		}

		public void setListOfDates(List<List<Date>> listOfDates) {
			this.listOfDates = listOfDates;
		}

		public Map<String, Map<String, Object>> getTestMapInMapData() {
			return testMapInMapData;
		}

		public void setTestMapInMapData(
				Map<String, Map<String, Object>> testMapInMapData) {
			this.testMapInMapData = testMapInMapData;
		}

		public String getCompanyName() {
			return companyName;
		}

		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public Address getCompanyAddress() {
			return companyAddress;
		}

		public void setCompanyAddress(Address companyAddress) {
			this.companyAddress = companyAddress;
		}

		public List<String> getDepartments() {
			return departments;
		}

		public void setDepartments(List<String> departments) {
			this.departments = departments;
		}

	}

	public static class Person {

		private String fname;

		private String lname;

		private String[] akaNames;

		private List<Map<String, Object>> remarks;

		private Child child;

		private BigDecimal age;

		private Date birthDate;

		public Instant deathDate;

		private Address address;

		public BigDecimal getAge() {
			return age;
		}

		public void setAge(BigDecimal age) {
			this.age = age;
		}

		public Date getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(Date birthDate) {
			this.birthDate = birthDate;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

		public List<Map<String, Object>> getRemarks() {
			return remarks;
		}

		public void setRemarks(List<Map<String, Object>> remarks) {
			this.remarks = remarks;
		}

		public String[] getAkaNames() {
			return akaNames;
		}

		public void setAkaNames(String... akaNames) {
			this.akaNames = akaNames;
		}

		public String getFname() {
			return fname;
		}

		public void setFname(String fname) {
			this.fname = fname;
		}

		public String getLname() {
			return lname;
		}

		public void setLname(String lname) {
			this.lname = lname;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

	}

	public static class Address {

		private String street;

		private String city;

		private String zip;

		private Map<String, List<String>> mapWithListData;

		private Map<String, Long[]> coordinates;

		public Map<String, List<String>> getMapWithListData() {
			return mapWithListData;
		}

		public void setMapWithListData(Map<String, List<String>> mapWithListData) {
			this.mapWithListData = mapWithListData;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public Map<String, Long[]> getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(Map<String, Long[]> coordinates) {
			this.coordinates = coordinates;
		}

	}

	public static class Child {

		private Person parent;

		public Person getParent() {
			return parent;
		}

		public void setParent(Person parent) {
			this.parent = parent;
		}

	}

}
