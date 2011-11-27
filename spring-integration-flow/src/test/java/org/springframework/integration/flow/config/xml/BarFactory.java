package org.springframework.integration.flow.config.xml;

import org.springframework.beans.factory.FactoryBean;

public class BarFactory implements FactoryBean<Bar> {

	public Bar getObject() throws Exception {
		// TODO Auto-generated method stub
		return new Bar();
	}

	public Class<?> getObjectType() {
		// TODO Auto-generated method stub
		return Bar.class;
	}

	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return true;
	}

}
