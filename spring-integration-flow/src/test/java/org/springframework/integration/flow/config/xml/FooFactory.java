package org.springframework.integration.flow.config.xml;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
public class FooFactory implements FactoryBean<Foo>{

	@Autowired 
	Bar bar;
	 
	public Foo getObject() throws Exception {
		// TODO Auto-generated method stub
		Foo foo = new Foo();
		foo.bar = bar;
		return foo;
	}

 
	public Class<?> getObjectType() {
		// TODO Auto-generated method stub
		return Foo.class;
	}

 
	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return true;
	}

}
