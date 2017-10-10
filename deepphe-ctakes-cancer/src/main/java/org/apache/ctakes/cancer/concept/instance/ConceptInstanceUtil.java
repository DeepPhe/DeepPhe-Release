package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.cancer.phenotype.PhenotypeAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/17/2016
 */
final public class ConceptInstanceUtil {

   static private final Logger LOGGER = Logger.getLogger( "ConceptInstanceUtil" );

   private ConceptInstanceUtil() {
   }

   static public Collection<ConceptInstance> getPropertyValues( final ConceptInstance property ) {
      return getPropertyValues( PhenotypeAnnotationUtil.getJcas( property.getAnnotations() ), property );
   }

   static public Collection<ConceptInstance> getPropertyValues( final JCas jcas,
                                                                final ConceptInstance property ) {
      return ConceptInstanceFactory.createConceptInstances( jcas,
            PhenotypeAnnotationUtil.getPropertyValues( jcas, property.getAnnotations() ) );
   }

   /**
    * @param instance any type of concept instance
    * @return jcas containing the annotation or null if there is none
    */
   static public JCas getJcas( final ConceptInstance instance ) {
      return PhenotypeAnnotationUtil.getJcas( instance.getAnnotations() );
   }

}
