package org.healthnlp.deepphe.util;

import java.io.File;
import java.util.*;

import org.healthnlp.deepphe.util.OntologyUtils;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;

public class OntologyUtilsTest {
   static {
      try {
         OntologyUtils.getInstance( OOntology.loadOntology( "http://ontologies.dbmi.pitt.edu/deepphe/nlpBreastCancer.owl" ) );
      } catch ( IOntologyException e ) {
         e.printStackTrace();
      }
   }

   public void testRlatedClassMap() throws IOntologyException {
      OntologyUtils ou = OntologyUtils.getInstance();

      Map<String, List<IClass>> map = ou.getPatientRelatedClassMap();
      System.out.println( "Patient Related Classes" );
      for ( String prop : map.keySet() ) {
         System.out.println( "\t" + prop + ":\t" + map.get( prop ) );
      }
      map = ou.getCancerRelatedClassMap();
      System.out.println( "Cancer Related Classes" );
      for ( String prop : map.keySet() ) {
         System.out.println( "\t" + prop + ":\t" + map.get( prop ) );
      }

      map = ou.getTumorRelatedClassMap();
      System.out.println( "Tumor Related Classes" );
      for ( String prop : map.keySet() ) {
         System.out.println( "\t" + prop + ":\t" + map.get( prop ) );
      }
      System.out.println( "Related: " + ou.getRelatedClases( map ) );

   }


   public static void main( String[] args ) throws IOntologyException {
      OntologyUtilsTest test = new OntologyUtilsTest();
      test.testRlatedClassMap();


   }

}
