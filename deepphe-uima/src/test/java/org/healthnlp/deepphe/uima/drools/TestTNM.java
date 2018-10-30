package org.healthnlp.deepphe.uima.drools;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.healthnlp.deepphe.uima.drools.DroolsEngine;

import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieSession;

public class TestTNM {
	private DroolsEngine de = null;
	private KieSession session = null;
	
	@Before
	public void setUp() {
		try {
			de = new DroolsEngine();
			session = de.getSession();
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
	
	@Test
	public void testRuleOne() {
		try{
			session.fireAllRules();
		} catch (Throwable t) {
            t.printStackTrace();
        }
	}
	

}
