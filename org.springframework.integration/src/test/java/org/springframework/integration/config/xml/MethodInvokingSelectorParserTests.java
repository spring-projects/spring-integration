package org.springframework.integration.config.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MethodInvokingSelectorParserTests {
	@Autowired
	MessageSelectorChain chain;

	@SuppressWarnings("unchecked")
	@Test
	public void configOK() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(chain);
		List<MessageSelector> selectors = (List<MessageSelector>) accessor.getPropertyValue("selectors");
		assertThat(selectors.get(0), is(MethodInvokingSelector.class));
	}

	public static class SomeClass {
		public boolean accept(Message m) {
			return true;
		}
	}
}
