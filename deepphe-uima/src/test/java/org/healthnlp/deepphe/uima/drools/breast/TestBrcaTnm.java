package org.healthnlp.deepphe.uima.drools.breast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.healthnlp.deepphe.uima.drools.Domain;
import org.healthnlp.deepphe.uima.drools.DroolsEngine;
import org.healthnlp.deepphe.util.FHIRConstants;
//import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieSession;

public class TestBrcaTnm {
	private DroolsEngine de = null;
	private KieSession session = null;
	
	
	@Before
	public void setUp() {
		try {
			de = new DroolsEngine();
			session = de.createSessionByKBaseName("BREAST");
			
			//session.addEventListener( new DebugAgendaEventListener() );
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
	
	
	@Test
	public void testM1() {
		try{
			session.fireAllRules();
		} catch (Throwable t) {
            t.printStackTrace();
        }
	}
	

}
