package org.healthnlp.deepphe.uima.drools;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * Drools Engine for AE
 * @author opm1
 *
 */

public class DroolsEngine {
	public static KieContainer kContainer = null;

	
	 public static KieContainer getKieContainer() throws Exception {
		 
		if(kContainer != null)  return kContainer;
		  
		KieServices ks = KieServices.Factory.get();
  	    kContainer = ks.getKieClasspathContainer(); 
  	    
	    return kContainer;
   
	 }

	 public KieSession getSession() {
		 KieSession session = null; 
		 try {
			 getKieContainer();
			 session = kContainer.newKieSession();
		 } catch (Throwable t) {
				t.printStackTrace();
		}
		 
		return session;
		 
	 }
	 
	 public KieSession createSessionByKBaseName(String kBaseName) {
		 KieSession session = null;
		 try {
			 session =  getKieContainer().getKieBase(kBaseName.toUpperCase()).newKieSession();
		} catch (Exception e) {
			e.printStackTrace();
		}
		 return session;
	 }

	 public KieSession getSession(String kBaseName) {
		 KieSession session = null; 
		 try {
			 getKieContainer();
			 session = getKieContainer().getKieBase(kBaseName).newKieSession();
		 } catch (Throwable t) {
				t.printStackTrace();
		}
		 
		return session;
		 
	 }

}
