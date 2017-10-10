package org.apache.ctakes.cancer.phenotype;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.dictionary.lookup2.concept.OwlConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/26/2017
 */
final public class PhenotypeUtil {

   static private final Logger LOGGER = Logger.getLogger( "PhenotypeUtil" );

   private PhenotypeUtil() {
   }


   /**
    * populate a map with synonyms keys and {@link PhenotypeConcept} values
    *
    * @param rootUri         uri for the root of a branch
    * @param synonymConcepts map to be filled
    */
   static public void buildUriSynonyms( final String rootUri, final Map<String, PhenotypeConcept> synonymConcepts ) {
      final Collection<IClass> iClasses = OwlOntologyConceptUtil.getUriBranchClasses( rootUri );
      for ( IClass iClass : iClasses ) {
         final Concept iConcept = iClass.getConcept();
         if ( iConcept != null ) {
            final PhenotypeConcept phenotypeConcept = new PhenotypeConcept( iClass, iConcept );
            Arrays.stream( iConcept.getSynonyms() )
                  .filter( Objects::nonNull )
//                  .filter( s -> !s.isEmpty() )
                  .filter( s -> s.length() > 1 )
                  .map( String::toLowerCase )
                  .distinct()
                  .forEach( s -> synonymConcepts.put( s, phenotypeConcept ) );
         }
      }
   }

   /**
    * populate a map with synonyms keys and {@link PhenotypeConcept} values
    *
    * @param rootUri         uri for the root of a branch
    * @param synonymConcepts map to be filled
    */
   static public void buildUriSynonyms( final String rootUri,
                                        final Map<String, PhenotypeConcept> synonymConcepts,
                                        final int maxWords ) {
      final Collection<IClass> iClasses = OwlOntologyConceptUtil.getUriBranchClasses( rootUri );
      for ( IClass iClass : iClasses ) {
         final Concept iConcept = iClass.getConcept();
         if ( iConcept != null ) {
            final PhenotypeConcept phenotypeConcept = new PhenotypeConcept( iClass, iConcept );
            Arrays.stream( iConcept.getSynonyms() )
                  .filter( Objects::nonNull )
                  .filter( s -> s.split( "\\s+" ).length <= maxWords )
                  .filter( s -> s.length() > 1 )
                  .map( String::toLowerCase )
                  .distinct()
                  .forEach( s -> synonymConcepts.put( s, phenotypeConcept ) );
         }
      }
   }

   /**
    * @param jCas              -
    * @param synonymConcepts   map with synonyms keys and {@link PhenotypeConcept} values
    * @param matchWindow       -
    * @param matchWindowOffset -
    */
   static public SignSymptomMention findPhenotype( final JCas jCas, final Map<String, PhenotypeConcept> synonymConcepts,
                                                   final String matchWindow, final int matchWindowOffset ) {
      final String lowerMatchWindow = matchWindow.toLowerCase();
      int bestIndex = -1;
      String bestSynonym = null;
      PhenotypeConcept bestConcept = null;
      for ( Map.Entry<String, PhenotypeConcept> synonymUri : synonymConcepts.entrySet() ) {
         final String synonym = synonymUri.getKey();
         final int index = lowerMatchWindow.indexOf( synonym );
         if ( index > -1 ) {
            if ( bestIndex == -1 ) {
               bestIndex = index;
               bestSynonym = synonym;
               bestConcept = synonymUri.getValue();
            } else if ( synonym.length() > bestSynonym.length() ) {
               bestIndex = index;
               bestSynonym = synonym;
               bestConcept = synonymUri.getValue();
            }
         }
      }
      if ( bestIndex == -1 ) {
         return null;
      }
      final int begin = matchWindowOffset + bestIndex;
      final SignSymptomMention phenotype = new SignSymptomMention( jCas, begin, begin + bestSynonym.length() );
      UmlsConcept umlsConcept = new UmlsConcept( jCas );
      umlsConcept.setCui( bestConcept.__cui == null ? "" : bestConcept.__cui );
      umlsConcept.setTui( bestConcept.__tui == null ? "" : bestConcept.__tui );
      umlsConcept.setPreferredText( bestConcept.__preferredText == null ? "" : bestConcept.__preferredText );
      umlsConcept.setCodingScheme( OwlConcept.URI_CODING_SCHEME );
      umlsConcept.setCode( bestConcept.__uri );
      final FSArray ontologyConcepts = new FSArray( jCas, 1 );
      ontologyConcepts.set( 0, umlsConcept );
      phenotype.setOntologyConceptArr( ontologyConcepts );
      phenotype.addToIndexes();
      return phenotype;
   }


}
