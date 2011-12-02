package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.flow.Flow;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration 
public class FlowWithAutowireTests {
	@Autowired
	Flow flow;
	@Test
	public void test() {
		Foo foo = flow.getFlowContext().getBean(Foo.class); 
		assertNotNull(foo.bar);
	}
}
