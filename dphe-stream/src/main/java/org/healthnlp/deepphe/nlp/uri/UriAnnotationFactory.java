package org.healthnlp.deepphe.nlp.uri;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.dictionary.lookup2.consumer.AllTuiTermConsumer;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.MultipleFoundException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_DISCOVERY_TECH_EXPLICIT_AE;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/9/2018
 */
public class UriAnnotationFactory {

   static private final Logger LOGGER = Logger.getLogger( "UriAnnotationFactory" );

   /**
    * @param jcas          ye olde ...
    * @param beginOffset   the offset of the first character of the annotation within text
    * @param endOffset     the offset of the last character (+1) of the annotation within text
    * @param uri           definitive uri for the annotation to be created
    * @param semanticGroup -
    * @param tui           -
    * @return an annotation with the given semantic information
    */
   static public Collection<IdentifiedAnnotation> createIdentifiedAnnotations( final JCas jcas, final int beginOffset,
                                                                               final int endOffset, final String uri,
                                                                               final SemanticGroup semanticGroup,
                                                                               final String tui ) {
      final String cui;
      final String prefText;
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri ) );
         }
         cui = (String)graphNode.getProperty( CUI_KEY );
         prefText = (String)graphNode.getProperty( PREF_TEXT_KEY );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
         return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri ) );
      }
      final IdentifiedAnnotation annotation = semanticGroup.getCreator().apply( jcas );
      annotation.setBegin( beginOffset );
      annotation.setEnd( endOffset );
      annotation.setSegmentID( getSectionId( jcas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jcas, beginOffset, endOffset ) );
      annotation.setTypeID( CONST.NE_TYPE_ID_UNKNOWN );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final UmlsConcept umlsConcept = createUmlsConcept( jcas, cui, tui, prefText, uri );
      final FSArray conceptArray = new FSArray( jcas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jcas );
      return Collections.singletonList( annotation );
   }


   /**
    * @param jcas          ye olde ...
    * @param beginOffset   the offset of the first character of the annotation within text
    * @param endOffset     the offset of the last character (+1) of the annotation within text
    * @param uri           definitive uri for the annotation to be created
    * @param preferredText -
    * @param semanticGroup -
    * @param tui           -
    * @return an annotation with the given semantic information
    */
   static public Collection<IdentifiedAnnotation> createIdentifiedAnnotations( final JCas jcas, final int beginOffset,
                                                                               final int endOffset, final String uri,
                                                                               final String preferredText,
                                                                               final SemanticGroup semanticGroup,
                                                                               final String tui ) {
      final String cui;
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri, preferredText ) );
         }
         cui = (String)graphNode.getProperty( CUI_KEY );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
         return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri, preferredText ) );
      }
      final IdentifiedAnnotation annotation = semanticGroup.getCreator().apply( jcas );
      annotation.setBegin( beginOffset );
      annotation.setEnd( endOffset );
      annotation.setSegmentID( getSectionId( jcas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jcas, beginOffset, endOffset ) );
      annotation.setTypeID( CONST.NE_TYPE_ID_UNKNOWN );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final UmlsConcept umlsConcept = createUmlsConcept( jcas, cui, tui, preferredText, uri );
      final FSArray conceptArray = new FSArray( jcas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jcas );
      return Collections.singletonList( annotation );
   }

   static private IdentifiedAnnotation createUnknownAnnotation( final JCas jCas, final int beginOffset,
                                                                final int endOffset, final String uri ) {
      final IdentifiedAnnotation annotation = new IdentifiedAnnotation( jCas, beginOffset, endOffset );
      annotation.setSegmentID( getSectionId( jCas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jCas, beginOffset, endOffset ) );
      annotation.setTypeID( CONST.NE_TYPE_ID_UNKNOWN );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final UmlsConcept umlsConcept = createUmlsConcept( jCas, uri );
      final FSArray conceptArray = new FSArray( jCas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jCas );
      return annotation;
   }

   static private IdentifiedAnnotation createUnknownAnnotation( final JCas jCas,
                                                                final int beginOffset, final int endOffset,
                                                                final String uri, final String preferredText ) {
      final IdentifiedAnnotation annotation = new IdentifiedAnnotation( jCas, beginOffset, endOffset );
      annotation.setSegmentID( getSectionId( jCas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jCas, beginOffset, endOffset ) );
      annotation.setTypeID( CONST.NE_TYPE_ID_UNKNOWN );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final UmlsConcept umlsConcept = createUmlsConcept( jCas, "", "", preferredText, uri );
      final FSArray conceptArray = new FSArray( jCas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jCas );
      return annotation;
   }

   static private IdentifiedAnnotation createAnnotation( final JCas jCas,
                                                         final int beginOffset, final int endOffset,
                                                         final int semanticId, final String cui,
                                                         final String uri, final String preferredText ) {
      final IdentifiedAnnotation annotation = AllTuiTermConsumer.createSemanticAnnotation( jCas, semanticId );
      annotation.setTypeID( semanticId );
      annotation.setBegin( beginOffset );
      annotation.setEnd( endOffset );
      annotation.setSegmentID( getSectionId( jCas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jCas, beginOffset, endOffset ) );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final UmlsConcept umlsConcept = createUmlsConcept( jCas, cui, "", preferredText, uri );
      final FSArray conceptArray = new FSArray( jCas, 1 );
      conceptArray.set( 0, umlsConcept );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jCas );
      return annotation;
   }

   static private IdentifiedAnnotation createAnnotation( final JCas jCas, final int beginOffset,
                                                         final int endOffset, final int semanticId,
                                                         final String cui, final Collection<String> tuis,
                                                         final String uri, final String prefText ) {
      final IdentifiedAnnotation annotation = AllTuiTermConsumer.createSemanticAnnotation( jCas, semanticId );
      annotation.setTypeID( semanticId );
      annotation.setBegin( beginOffset );
      annotation.setEnd( endOffset );
      annotation.setSegmentID( getSectionId( jCas, beginOffset, endOffset ) );
      annotation.setSentenceID( getSentenceId( jCas, beginOffset, endOffset ) );
      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final FSArray conceptArray = new FSArray( jCas, tuis.size() );
      int i = 0;
      for ( String tui : tuis ) {
         final UmlsConcept umlsConcept = createUmlsConcept( jCas, cui, tui, prefText, uri );
         conceptArray.set( i, umlsConcept );
         i++;
      }
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes( jCas );
      return annotation;
   }


      /**
       *
       * @param jcas ye olde ...
       * @param uri uri used to populate the UmlsConcept information
       * @return UmlsConcept populated with cui, tui, uri, preferred text
       */
      static public UmlsConcept createUmlsConcept( final JCas jcas, final String uri ) {
         String prefText = UriUtil.getExtension( uri );
      prefText = prefText.replaceAll( "_", " " );
      return createUmlsConcept( jcas, "", "", prefText, uri );
   }

   /**
    *
    * @param jcas ye olde ...
    * @param cui -
    * @param tui -
    * @param prefText -
    * @param uri uri used to populate the UmlsConcept information
    * @return UmlsConcept populated with cui, tui, uri, preferred text
    */
   static private UmlsConcept createUmlsConcept( final JCas jcas, final String cui, final String tui, final String prefText, final String uri ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui == null ? "" : cui );
      umlsConcept.setTui( tui == null ? "" : tui );
      umlsConcept.setPreferredText( prefText == null ? "" : prefText );
      umlsConcept.setCodingScheme( DPHE_CODING_SCHEME );
      umlsConcept.setCode( uri );
      return umlsConcept;
   }

   static private String getSectionId( final JCas jCas, final int beginOffset, final int endOffset ) {
      final List<Segment> sections = JCasUtil.selectCovering( jCas, Segment.class, beginOffset, endOffset );
      return sections.isEmpty()
             ? "UNKNOWN_SECTION"
             : sections.get( 0 )
                       .getId();
   }

   static private String getSentenceId( final JCas jCas, final int beginOffset, final int endOffset ) {
      final List<Sentence> sentences = JCasUtil.selectCovering( jCas, Sentence.class, beginOffset, endOffset );
      return sentences.isEmpty()
                          ? "UNKNOWN_SENTENCE"
                          : "Sentence_" + sentences.get( 0 ).getSentenceNumber();
   }

}
