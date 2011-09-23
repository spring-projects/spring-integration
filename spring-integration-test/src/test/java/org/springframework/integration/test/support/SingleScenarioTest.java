package org.springframework.integration.test.support;

import static org.junit.Assert.assertEquals;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("MessageScenariosTest-context.xml") 
public class SingleScenarioTest extends SingleRequestResponseScenarioTest {
   
	/* (non-Javadoc)
	 * @see org.springframework.integration.test.support.SingleRequestResponseScenarioTest#defineRequestResponseScenario()
	 */
	@Override
	protected RequestResponseScenario defineRequestResponseScenario() {
		RequestResponseScenario scenario = new RequestResponseScenario(
                "inputChannel","outputChannel")
            .setPayload("hello")
            .setResponseValidator(new PayloadValidator<String>() {    
                @Override
                protected void validateResponse(String response) {
                    assertEquals("HELLO",response);
                }
            });
		return scenario;
	}
}
