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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author Oleg Zhurakousky
 *
 */
public class CycleDetectorTests {
	@Test
	public void testWithNoIgnoredPackages(){
		Parent parent = this.prepare(false);
		CycleDetector builder = new CycleDetector();
		builder.setIgnoreDefaultPackages(false);
		builder.detectCycle(parent);
		// should not throw an exception
	}
	@Test
	public void testWithNoIgnoredPackagesAndClassProperty(){
		Parent parent = this.prepare(false);
		CycleDetector builder = new CycleDetector();
		builder.setIgnoreDefaultPackages(false);
		builder.setIgnoreClassAttribute(false);
		builder.detectCycle(parent);
		// should not throw an exception
	}
	@Test
	public void testWithAditionalIgnoredPackages(){
		Parent parent = this.prepare(true);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent, "org.springframework.integration.transformer");
		// should not throw an exception, however if you remove additional package from
		// the above method there is a cycle in the domain core
	}

	@Test
	public void testObjectReferenceMapWithoutCycle(){
		Parent parent = this.prepare(false);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
		// should not throw an exception
	}
	
	@Test(expected=MessageTransformationException.class)
	public void testObjectReferenceMapWithCycle(){
		Parent parent = this.prepare(true);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
	}
	@Test
	public void testWithNoCyclesInArray(){
		Parent parent = this.prepare(false);
		Foo[] foos = new Foo[]{new Foo(), new Foo()};
		parent.getAddress().setFoos(foos);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
	}
	@Test(expected=MessageTransformationException.class)
	public void testWithCyclesInArray(){
		Parent parent = this.prepare(false);
		Foo foo = new Foo();
		Bar bar = new Bar();
		foo.setBar(bar);
		bar.setFoo(foo);
		Foo[] foos = new Foo[]{foo, new Foo()};
		parent.getAddress().setFoos(foos);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
	}
	@Test
	public void testWithNoCyclesInMapWithList(){
		Parent parent = this.prepare(false);
		Foo fooA = new Foo();
		Foo fooB = new Foo();
		Foo[] foos = new Foo[]{fooA, fooB};
		parent.getAddress().setFoos(foos);
		List<Foo> fooList = new ArrayList<Foo>();
		fooList.add(fooA);
		fooList.add(fooB);
		Map<Object, List<Foo>> mapOfFoos = new HashMap<Object, List<Foo>>();
		mapOfFoos.put("listOfFoos", fooList);
		parent.getChild().setMapOfFoos(mapOfFoos);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
	}
	@Test(expected=MessageTransformationException.class)
	public void testWithCyclesInMapWithList(){
		Parent parent = this.prepare(false);
		Foo fooA = new Foo();
		Bar bar = new Bar();
		bar.setFoo(fooA);
		fooA.setBar(bar);
		Foo fooB = new Foo();
		Foo[] foos = new Foo[]{new Foo(), new Foo()};
		parent.getAddress().setFoos(foos);
		List<Foo> fooList = new ArrayList<Foo>();
		fooList.add(fooA);
		fooList.add(fooB);
		Map<Object, List<Foo>> mapOfFoos = new HashMap<Object, List<Foo>>();
		mapOfFoos.put("listOfFoos", fooList);
		parent.getChild().setMapOfFoos(mapOfFoos);
		CycleDetector builder = new CycleDetector();
		builder.detectCycle(parent);
	}
	
	//################# Test Classes ###################
	public Parent prepare(boolean cycle){
		Parent parent = new Parent();
		Child child = new Child();
		Address address = new Address();
		address.setStreet("123 Main st");
		child.setAddress(address);
		List<String> nickNames = new ArrayList<String>();
		nickNames.add("spanky");
		nickNames.add("goofy");
		child.setNickNames(nickNames);
		child.setName("Seva");
		if (cycle){
			child.setParent(parent);
		}
		
		parent.setAddress(address);
		parent.setChild(child);
		parent.setName("Oleg");
		return parent;
	}
	/*
	 * 
	 */
	public static class Parent{
		private Address address;
		private Child child;
		private String name;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
		public Child getChild() {
			return child;
		}
		public void setChild(Child child) {
			this.child = child;
		}
	}
	/*
	 * 
	 */
	public static class Child{
		private String name;
		private List<String> nickNames;
		private Map<Object, List<Foo>> mapOfFoos;
		private Address address;
		private Parent parent;
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public List<String> getNickNames() {
			return nickNames;
		}

		public void setNickNames(List<String> nickNames) {
			this.nickNames = nickNames;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		public Map<Object, List<Foo>> getMapOfFoos() {
			return mapOfFoos;
		}

		public void setMapOfFoos(Map<Object, List<Foo>> mapOfFoos) {
			this.mapOfFoos = mapOfFoos;
		}
	}
	/*
	 * 
	 */
	public static class Address{
		private String street;
		private Foo[] foos;

		public Foo[] getFoos() {
			return foos;
		}

		public void setFoos(Foo[] foos) {
			this.foos = foos;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}
	
	public static class Foo{
		private Bar bar;

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}
	}
	public static class Bar{
		private Foo foo;

		public Foo getFoo() {
			return foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}
	}
}
