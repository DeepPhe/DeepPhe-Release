package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * A convenience class created for the second pipeline.  Should no longer be required.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/15/2016
 */
@Deprecated
final public class ConceptInstanceFetcher {

   static private final Logger LOGGER = Logger.getLogger( "ConceptInstanceFetcher" );


   private ConceptInstanceFetcher() {
   }


   /**
    * @param jcas ye olde ...
    * @return all diseases / disorders in the cas
    */
   static public Collection<ConceptInstance> getDiseaseDisorders( final JCas jcas ) {
      return ConceptInstanceFactory.createBranchConceptInstances( jcas, OwlConstants.DISEASE_DISORDER_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all signs / symptoms in the cas
    */
   static public Collection<ConceptInstance> getSignSymptoms( final JCas jcas ) {
      return ConceptInstanceFactory.createBranchConceptInstances( jcas, OwlConstants.SIGN_SYMPTOM_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all findings in the cas. stage, tnm, and others
    */
   static public Collection<ConceptInstance> getFindings( final JCas jcas ) {
      return ConceptInstanceFactory.createBranchConceptInstances( jcas, OwlConstants.FINDING_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all diagnostic tests in the cas
    */
   static public Collection<ConceptInstance> getDiagnosticTests( final JCas jcas ) {
      return ConceptInstanceFactory.createBranchConceptInstances( jcas, OwlConstants.TEST_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all procedures int the cas.  This includes diagnostic tests
    */
   static public Collection<ConceptInstance> getNonTestProcedures( final JCas jcas ) {
      final Collection<String> testUris = OwlOntologyConceptUtil.getUriBranchStream( OwlConstants.TEST_URI )
            .collect( Collectors.toSet() );
      return OwlOntologyConceptUtil.getUriBranchStream( OwlConstants.PROCEDURE_URI )
            .filter( u -> !testUris.contains( u ) )
            .map( u -> ConceptInstanceFactory.createExactConceptInstances( jcas, u ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   /**
    * @param jcas ye olde ...
    * @return all medications in the cas
    */
   static public Collection<ConceptInstance> getMedications( final JCas jcas ) {
      return ConceptInstanceFactory.createBranchConceptInstances( jcas, OwlConstants.MEDICATION_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all neoplasms in the cas
    */
   static public Collection<ConceptInstance> getNeoplasms( final JCas jcas ) {
      return getConceptInstances( jcas, OwlConstants.NEOPLASM_URIS );
   }

   /**
    * @param jcas ye olde ...
    * @return all primary neoplasms in the cas
    */
   static public Collection<ConceptInstance> getPrimaries( final JCas jcas ) {
      final Collection<String> metastasisUris = OwlOntologyConceptUtil.getUriBranchStream( OwlConstants.METASTASIS_URI )
            .collect( Collectors.toSet() );
      return Arrays.stream( OwlConstants.NEOPLASM_URIS )
            .map( OwlOntologyConceptUtil::getUriBranchStream )
            .flatMap( Function.identity() )
            .filter( u -> !metastasisUris.contains( u ) )
            .map( u -> ConceptInstanceFactory.createExactConceptInstances( jcas, u ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   /**
    * @param jcas ye olde ...
    * @return all metastatic neoplasms in the cas
    */
   static public Collection<ConceptInstance> getMetastases( final JCas jcas ) {
      return getConceptInstances( jcas, OwlConstants.METASTASIS_URI );
   }

   /**
    * @param jcas ye olde ...
    * @return all observations in the cas.  Receptor Status, Size
    */
   static public Collection<ConceptInstance> getObservations( final JCas jcas ) {
      return getConceptInstances( jcas, OwlConstants.OBSERVATION_URIS );
   }


   /**
    * @param jcas ye olde ...
    * @param uris parent uris
    * @return Concept Instances for every branch under the given parent uris
    */
   static public Collection<ConceptInstance> getConceptInstances( final JCas jcas, final String... uris ) {
      return Arrays.stream( uris )
            .map( u -> ConceptInstanceFactory.createBranchConceptInstances( jcas, u ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }


}
