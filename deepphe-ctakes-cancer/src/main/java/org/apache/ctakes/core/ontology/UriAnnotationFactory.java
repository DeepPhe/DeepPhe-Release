package org.apache.ctakes.core.ontology;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import org.apache.ctakes.cancer.owl.UriAnnotationCache;
import org.apache.ctakes.dictionary.lookup2.concept.OwlConcept;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import javax.annotation.concurrent.Immutable;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;

/**
 * Factory class that should be used to create annotations from uris.
 * Created annotations will have a class type based upon the semantic group of the iclass with the given uri.
 * The method used to create an annotation from a uri is {@link #createIdentifiedAnnotation(JCas, int, int, String)}.
 * There is also a method that can be used to create only a UmlsConcept with a uri,
 * which can be used by external code to create a full annotation.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/25/2016
 */
@Immutable
final public class UriAnnotationFactory {

   static private final Logger LOGGER = Logger.getLogger( "UriAnnotationFactory" );

   private UriAnnotationFactory() {
   }

   /**
    * @param jcas        ye olde ...
    * @param beginOffset the offset of the first character of the annotation within text
    * @param endOffset   the offset of the last character (+1) of the annotation within text
    * @param uri         definitive uri for the annotation to be created
    * @return appropriate Semantic group subclass of IdentifiedAnnotation with a populated ontology concept array
    */
   static public IdentifiedAnnotation createIdentifiedAnnotation( final JCas jcas, final int beginOffset,
                                                                  final int endOffset, final String uri ) {
      final IClass iClass = OwlOntologyConceptUtil.getIClass( uri );
      if ( iClass == null ) {
         return new IdentifiedAnnotation( jcas, beginOffset, endOffset );
      }
      final Integer semanticGroupId = UriAnnotationCache.getInstance().getUriSemanticRoot( uri );
      final IdentifiedAnnotation annotation = createSemanticAnnotation( jcas, semanticGroupId );
      annotation.setTypeID( semanticGroupId );
      annotation.setBegin( beginOffset );
      annotation.setEnd( endOffset );
      //  As there are currently only 2 discovery techniques this is not exact, but "gold" may be better than "lookup"
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_GOLD_ANNOTATION );
      final UmlsConcept umlsConcept = createUmlsConcept( jcas, iClass, uri );
      final FSArray conceptArray = new FSArray( jcas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
//      annotation.addToIndexes();
      return annotation;
   }

   /**
    * @param jcas           ye olde ...
    * @param cTakesSemantic integer code for the ctakes semantic group to which the annotation belongs
    * @return instance of some IdentifiedAnnotation subclass for the given cTakesSemantic
    * or a generic EventMention if none can be derived
    */
   static private IdentifiedAnnotation createSemanticAnnotation( final JCas jcas, final int cTakesSemantic ) {
      switch ( cTakesSemantic ) {
         case NE_TYPE_ID_DRUG: {
            return new MedicationMention( jcas );
         }
         case NE_TYPE_ID_ANATOMICAL_SITE: {
            return new AnatomicalSiteMention( jcas );
         }
         case NE_TYPE_ID_DISORDER: {
            return new DiseaseDisorderMention( jcas );
         }
         case NE_TYPE_ID_FINDING: {
            return new SignSymptomMention( jcas );
         }
         case NE_TYPE_ID_LAB: {
            return new LabMention( jcas );
         }
         case NE_TYPE_ID_PROCEDURE: {
            return new ProcedureMention( jcas );
         }
      }
      return new EntityMention( jcas );
   }

   /**
    * @param jcas ye olde ...
    * @param uri  uri used to populate the UmlsConcept information
    * @return UmlsConcept populated with cui, tui, uri, preferred text
    */
   static public UmlsConcept createUmlsConcept( final JCas jcas, final String uri ) {
      final IClass iClass = OwlOntologyConceptUtil.getIClass( uri );
      if ( iClass == null ) {
         final UmlsConcept umlsConcept = new UmlsConcept( jcas );
         umlsConcept.setCui( "" );
         umlsConcept.setTui( "" );
         umlsConcept.setPreferredText( "" );
         umlsConcept.setCodingScheme( OwlConcept.URI_CODING_SCHEME );
         umlsConcept.setCode( uri );
         return umlsConcept;
      }
      return createUmlsConcept( jcas, iClass, uri );
   }

   /**
    * @param jcas   ye olde ...
    * @param iClass for the umls concept.  Usually from the uri.  Cui, Tui, Preferred text are from the iclass
    * @param uri    uri used to populate the UmlsConcept information
    * @return UmlsConcept populated with cui, tui, uri, preferred text
    */
   static private UmlsConcept createUmlsConcept( final JCas jcas, final IClass iClass, final String uri ) {
      final String cui = OwlParserUtil.getCui( iClass );
      final String tui = OwlParserUtil.getTui( iClass );
      final String title = OwlParserUtil.getPreferredText( iClass );
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui == null ? "" : cui );
      umlsConcept.setTui( tui == null ? "" : tui );
      umlsConcept.setPreferredText( title == null ? "" : title );
      umlsConcept.setCodingScheme( OwlConcept.URI_CODING_SCHEME );
      umlsConcept.setCode( uri );
      return umlsConcept;
   }

}
