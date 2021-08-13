package org.healthnlp.deepphe.core.json;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.node.*;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_CODING_SCHEME;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/14/2020
 */
final public class JsonNoteReader {

   static private final Logger LOGGER = Logger.getLogger( "JsonNoteReader" );

   private JsonNoteReader() {}


   static public Patient createPatient( final String patientJson ) {
      final Gson gson = new GsonBuilder().create();
      return gson.fromJson( patientJson, Patient.class );
   }

   static public void populatePatientNoteJCas( final JCas jCas, final Patient patient, final Note note ) {
      new JCasBuilder().setPatientId( patient.getId() )
                       .setDocId( note.getId() )
                       .setDocTime( note.getDate() )
                       .setDocType( note.getType() )
                       .setDocIdPrefix( patient.getId() )
                       .setDocText( note.getText() )
                       .rebuild( jCas );
      addEpisode( jCas, note.getEpisode() );
      addSections( jCas, note.getSections() );
      final Map<String, IdentifiedAnnotation> idAnnotations = addMentions( jCas, note.getMentions() );
      addRelations( jCas, idAnnotations, note.getRelations() );
      addCorefs( jCas, idAnnotations, note.getCorefs() );
   }


   static private void addSections( final JCas jCas, final Collection<Section> sections ) {
      for ( Section section : sections ) {
         final Segment segment = new Segment( jCas, section.getBegin(), section.getEnd() );
         segment.setId( section.getType() );
         segment.addToIndexes();
      }
   }

   static private void addEpisode( final JCas jCas, final String episodeType ) {
      if ( episodeType == null || episodeType.isEmpty() ) {
         return;
      }
      final Episode episode = new Episode( jCas );
      episode.setEpisodeType( episodeType );
      episode.addToIndexes();
   }


   static private Map<String, IdentifiedAnnotation> addMentions( final JCas jCas,
                                                                 final Collection<Mention> mentions ) {
      final Map<String, IdentifiedAnnotation> idAnnotations = new HashMap<>( mentions.size() );
      for ( Mention mention : mentions ) {
         final String uri = mention.getClassUri();
         final SemanticGroup semanticGroup = Neo4jOntologyConceptUtil.getBestSemanticGroup( uri );
         final IdentifiedAnnotation annotation = semanticGroup.getCreator().apply( jCas );
         annotation.setBegin( mention.getBegin() );
         annotation.setEnd( mention.getEnd() );
         annotation.setPolarity(
               mention.isNegated() ? CONST.NE_POLARITY_NEGATION_PRESENT : CONST.NE_POLARITY_NEGATION_ABSENT );
         annotation.setUncertainty(
               mention.isUncertain() ? CONST.NE_UNCERTAINTY_PRESENT : CONST.NE_UNCERTAINTY_ABSENT );
         annotation.setGeneric( mention.isGeneric() );
         annotation.setConditional( mention.isConditional() );
         annotation.setHistoryOf( mention.isHistoric() ? CONST.NE_HISTORY_OF_PRESENT : CONST.NE_HISTORY_OF_ABSENT );
         if ( annotation instanceof EventMention ) {
            final EventProperties properties = new EventProperties( jCas );
            properties.setDocTimeRel( mention.getTemporality() );
            final Event event = new Event( jCas );
            event.setProperties( properties );
            ((EventMention)annotation).setEvent( event );
         }
         annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
         final FSArray conceptArr = new FSArray( jCas, 1 );
         conceptArr.set( 0, createUmlsConcept( jCas, uri ) );
         annotation.setOntologyConceptArr( conceptArr );
         annotation.addToIndexes();
         idAnnotations.put( mention.getId(), annotation );
      }
      return idAnnotations;
   }

   static private UmlsConcept createUmlsConcept( final JCas jcas, final String uri ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( DPHE_CODING_SCHEME );
      // cui not necessary.  Can be fetched using Neo4jOntologyConceptUtil, but isn't used for summarization.
//      umlsConcept.setCui( cui );
      umlsConcept.setCode( uri );
      return umlsConcept;
   }


   static private void addRelations( final JCas jCas,
                                     final Map<String, IdentifiedAnnotation> idAnnotations,
                                     final Collection<MentionRelation> relations ) {
      for ( MentionRelation mentionRelation : relations ) {
         final BinaryTextRelation relation = new BinaryTextRelation( jCas );
         relation.setCategory( mentionRelation.getType() );
         relation.setArg1( createArgument( jCas, idAnnotations, mentionRelation.getSourceId() ) );
         relation.setArg2( createArgument( jCas, idAnnotations, mentionRelation.getTargetId() ) );
         relation.addToIndexes();
      }
   }

   static private RelationArgument createArgument( final JCas jCas,
                                                   final Map<String, IdentifiedAnnotation> idAnnotations,
                                                   final String id ) {
      final IdentifiedAnnotation annotation = idAnnotations.get( id );
      if ( annotation == null ) {
         return null;
      }
      return createArgument( jCas, annotation );
   }

   static private RelationArgument createArgument( final JCas jCas, IdentifiedAnnotation annotation ) {
      final RelationArgument argument = new RelationArgument( jCas );
      argument.setArgument( annotation );
      return argument;
   }


   static private void addCorefs( final JCas jCas,
                                  final Map<String, IdentifiedAnnotation> idAnnotations,
                                  final Collection<MentionCoref> mentionCorefs ) {
      for ( MentionCoref mentionCoref : mentionCorefs ) {
         final CollectionTextRelation coref = new CollectionTextRelation( jCas );
         final String[] idChain = mentionCoref.getIdChain();
         final List<IdentifiedAnnotation> chain = new ArrayList<>( idChain.length );
         for ( String id : idChain ) {
            chain.add( idAnnotations.get( id ) );
         }
         coref.setMembers( ListFactory.buildList( jCas, chain ) );
         coref.addToIndexes();
      }
   }


}
