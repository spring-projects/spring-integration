	package org.springframework.integration.activiti.signup;

	import org.activiti.engine.impl.bpmn.BpmnActivityBehavior;
	import org.activiti.pvm.activity.ActivityBehavior;
	import org.activiti.pvm.activity.ActivityExecution;
	import org.springframework.stereotype.Component;

	@Component
	public class CheckForm extends BpmnActivityBehavior implements ActivityBehavior {

		public void execute(ActivityExecution activityExecution) throws Exception {
			System.out.println( getClass()+" : checking form: ");

			boolean formOK = Math.random() > .7 ;

			activityExecution.setVariable( "formOK",  formOK);
			System.out.println( getClass()+" : form OK? " + formOK);


			performDefaultOutgoingBehavior( activityExecution );

		}
	}


