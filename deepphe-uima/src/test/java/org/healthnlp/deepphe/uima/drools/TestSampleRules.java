package org.healthnlp.deepphe.uima.drools;


import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;


public class TestSampleRules {
	private KieSession session = null;

	@Before
	public void setUp() {
		try {
			
			KieServices ks = KieServices.Factory.get();
    	    KieContainer kContainer = ks.getKieClasspathContainer();
        	KieSession session = kContainer.newKieSession("ksession-rules");
			
			session.addEventListener( new DebugAgendaEventListener() );

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@After
	public void tearDown() {
		if (session != null) {
			session.dispose();
		}
	}

	@Ignore
	@Test
	public void testRuleOne() {
		try{
			session.fireAllRules();
		} catch (Throwable t) {
            t.printStackTrace();
        }
	}
}
