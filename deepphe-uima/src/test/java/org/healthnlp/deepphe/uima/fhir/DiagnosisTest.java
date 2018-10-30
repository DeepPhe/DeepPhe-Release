package org.healthnlp.deepphe.uima.fhir;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import static org.junit.Assert.fail;


public class DiagnosisTest {

	static private final Logger LOGGER = Logger.getLogger( "DiagnosisTest" );

	/**
	 * create mention
	 * @param dd
	 * @param st
	 * @param en
	 * @param code
	 * @return
	 */
	private IdentifiedAnnotation createMention(IdentifiedAnnotation dd, int st, int en, String code){
		JCas jcas = null;
		try {
			jcas = dd.getCAS().getJCas();
		} catch (CASException e) {
			e.printStackTrace();
			fail("Unable to init AE"+e.getMessage());
		}
		OntologyConcept o = new OntologyConcept(jcas);
		o.setCode(code);
		o.setCodingScheme("UMLS");
		dd.setBegin(23);
		dd.setEnd(36);
		dd.setOntologyConceptArr(new FSArray(jcas,1));
		dd.setOntologyConceptArr(0,o);
		dd.addToIndexes();
		return dd;
	}
	
	private RelationArgument createArgument(IdentifiedAnnotation a){
		JCas jcas = null;
		try {
			jcas = a.getCAS().getJCas();
		} catch (CASException e) {
			e.printStackTrace();
			fail("Unable to init AE"+e.getMessage());
		}
		RelationArgument ra = new RelationArgument(jcas);
		ra.setArgument(a);
		ra.addToIndexes();
		return ra;
	}
	

}
