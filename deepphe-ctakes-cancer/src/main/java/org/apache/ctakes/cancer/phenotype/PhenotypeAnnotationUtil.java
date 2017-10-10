package org.apache.ctakes.cancer.phenotype;


import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Utility class with methods that can be used to get information about Neoplasm Properties.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/25/2016
 */
@Immutable
final public class PhenotypeAnnotationUtil {

   static private final Logger LOGGER = Logger.getLogger( "PhenotypeAnnotationUtil" );

   private PhenotypeAnnotationUtil() {
   }

   static public Collection<IdentifiedAnnotation> getPropertyValues( final IdentifiedAnnotation property ) {
      return getPropertyValues( getJcas( property ), property );
   }

   static public Collection<IdentifiedAnnotation> getPropertyValues( final JCas jcas,
                                                                     final IdentifiedAnnotation property ) {
      return getPropertyValues( jcas, Collections.singleton( property ) );
   }

   static public Collection<IdentifiedAnnotation> getPropertyValues( final JCas jcas,
                                                                     final Collection<IdentifiedAnnotation> properties ) {
      if ( jcas == null ) {
         return Collections.emptyList();
      }
      final Collection<DegreeOfTextRelation> relations = JCasUtil.select( jcas, DegreeOfTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      return properties.stream()
            .map( p -> RelationUtil.getSecondArguments( relations, p ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }


   static public JCas getJcas( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
            .map( PhenotypeAnnotationUtil::getJcas )
            .filter( Objects::nonNull )
            .findFirst().orElse( null );
   }

   /**
    * @param annotation any type of annotation
    * @return jcas containing the annotation or null if there is none
    */
   static public JCas getJcas( final TOP annotation ) {
      try {
         return annotation.getCAS().getJCas();
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return null;
   }


}
