package org.healthnlp.deepphe.core.json;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.node.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/14/2020
 */
final public class JsonNoteWriter {

   static private final Logger LOGGER = Logger.getLogger( "JsonNoteWriter" );

   public static final String NO_CATEGORY = "unknown";

   static private final String SECTION_ID = "_S_";
   static private final String MENTION_ID = "_M_";
   static private final String RELATION_ID = "_R_";
   static private final String COREF_ID = "_C_";

   static private final NumberedSuffixComparator SUFFIX_COMPARATOR = new NumberedSuffixComparator();

   static private final Comparator<MentionRelation> RELATION_COMPARATOR = ( r1, r2 ) -> {
      final int typeCompare = SUFFIX_COMPARATOR.compare( r1.getType(), r2.getType() );
      if ( typeCompare != 0 ) {
         return typeCompare;
      }
      final int sourceCompare = SUFFIX_COMPARATOR.compare( r1.getSourceId(), r2.getSourceId() );
      if ( sourceCompare != 0 ) {
         return sourceCompare;
      }
      return SUFFIX_COMPARATOR.compare( r1.getTargetId(), r2.getTargetId() );
   };

   static private final Comparator<MentionCoref> COREF_COMPARATOR = ( c1, c2 ) -> {
      final String[] id1 = c1.getIdChain();
      final String[] id2 = c2.getIdChain();
      for ( int i = 0; i < Math.min( id1.length, id2.length ); i++ ) {
         final int idCompare = SUFFIX_COMPARATOR.compare( id1[ i ], id2[ i ] );
         if ( idCompare != 0 ) {
            return idCompare;
         }
      }
      return id1.length - id2.length;
   };


   private JsonNoteWriter() {}


   static public String createPatientJson( final JCas jCas ) {
      final Note note = createNote( jCas );
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      final Patient patient = new Patient();
      patient.setId( patientId );
      patient.setNotes( Collections.singletonList( note ) );
      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson( patient );
   }

   static public String createNoteJson( final JCas jCas ) {
      final Note note = createNote( jCas );
      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson( note );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NOTE
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   static public Note createNote( final JCas jCas ) {
      final Note note = new Note();
      final NoteSpecs noteSpecs = new NoteSpecs( jCas );
      final String docId = noteSpecs.getDocumentId();
      note.setId( docId );
      note.setDate( noteSpecs.getNoteTime() );
      note.setType( getDocType( noteSpecs.getDocumentType() ) );
      note.setText( noteSpecs.getDocumentText() );
      final String episodeType = JCasUtil.select( jCas, Episode.class ).stream()
                                         .map( Episode::getEpisodeType )
                                         .distinct()
                                         .sorted()
                                         .findFirst()
                                         .orElse( NO_CATEGORY );
      note.setEpisode( episodeType );

      final List<Section> sectionList
            = createSectionList( docId, JCasUtil.select( jCas, Segment.class ) );
      note.setSections( sectionList );

      final Map<IdentifiedAnnotation, Collection<Integer>> corefs
            = EssentialAnnotationUtil.createMarkableCorefs( jCas );
//      final Collection<IdentifiedAnnotation> requiredAnnotations
//            = EssentialAnnotationUtil.getRequiredAnnotations( jCas, corefs );
      final Collection<IdentifiedAnnotation> requiredAnnotations
            = EssentialAnnotationUtil.getRequiredAnnotations( jCas, corefs ).stream()
                                     .filter( a -> !Neo4jOntologyConceptUtil.getUri( a ).isEmpty() )
                                     .collect( Collectors.toList() );

      final Map<IdentifiedAnnotation, Mention> mentionMap = createMentionMap( requiredAnnotations );

      final List<Mention> mentionList = createMentionList( docId, mentionMap.values() );
      note.setMentions( mentionList );

      final List<MentionRelation> relationList
            = createRelationList( docId, mentionMap, JCasUtil.select( jCas, BinaryTextRelation.class ) );
      note.setRelations( relationList );

      final List<MentionCoref> corefList = createCorefList( docId, mentionMap, corefs );
      note.setCorefs( corefList );

      return note;
   }

   static private String getDocType( final String docType ) {
      switch ( docType ) {
         case "RAD":
            return "Radiology Report";
         case "PATH":
            return "Pathology Report";
         case "SP":
            return "Surgical Pathology Report";
         case "DS":
            return "Discharge Summary";
         case "PGN":
            return "Progress Note";
         case "NOTE":
            return "Clinical Note";
         case NoteSpecs.ID_NAME_CLINICAL_NOTE:
            return "Clinical Note";
      }
      return docType.replace( '_', ' ' );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SECTION
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   static private List<Section> createSectionList( final String docId, final Collection<Segment> segments ) {
      final List<Section> sectionList = segments.stream()
                                                .map( JsonNoteWriter::createSection )
                                                .sorted( Comparator.comparingInt( Section::getBegin ) )
                                                .collect( Collectors.toList() );
      int id = 1;
      for ( Section section : sectionList ) {
         section.setId( docId + SECTION_ID + id );
         id++;
      }
      return sectionList;
   }

   static private Section createSection( final Segment segment ) {
      final Section section = new Section();
      section.setId( segment.getId() );
      section.setType( segment.getPreferredText() );
      section.setBegin( segment.getBegin() );
      section.setEnd( segment.getEnd() );
      return section;
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   static private Map<IdentifiedAnnotation, Mention> createMentionMap(
         final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .collect( Collectors.toMap(
                              Function.identity(), JsonNoteWriter::createMention ) );
   }


   static private List<Mention> createMentionList( final String docId, final Collection<Mention> mentions ) {
      final List<Mention> mentionList
            = mentions.stream()
                      .sorted( Comparator.comparingInt( Mention::getBegin ) )
                      .collect( Collectors.toList() );
      int id = 1;
      for ( Mention mention : mentionList ) {
         mention.setId( docId + MENTION_ID + id );
         id++;
      }
      return mentionList;
   }


   static private Mention createMention( final IdentifiedAnnotation annotation ) {
      final Mention mention = new Mention();
      mention.setClassUri( Neo4jOntologyConceptUtil.getUri( annotation ) );
      mention.setBegin( annotation.getBegin() );
      mention.setEnd( annotation.getEnd() );
      mention.setNegated( IdentifiedAnnotationUtil.isNegated( annotation ) );
      mention.setUncertain( IdentifiedAnnotationUtil.isUncertain( annotation ) );
      mention.setGeneric( IdentifiedAnnotationUtil.isGeneric( annotation ) );
      mention.setConditional( IdentifiedAnnotationUtil.isConditional( annotation ) );
      mention.setHistoric( IdentifiedAnnotationUtil.isHistoric( annotation ) );
      String temporality = "";
      if ( annotation instanceof EventMention ) {
         final Event event = ((EventMention)annotation).getEvent();
         if ( event != null ) {
            final EventProperties eventProperties = event.getProperties();
            if ( eventProperties != null ) {
               final String dtr = eventProperties.getDocTimeRel();
               if ( dtr != null && !dtr.isEmpty() ) {
                  temporality = dtr;
               }
            }
         }
      }
      mention.setTemporality( temporality );
      return mention;
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            RELATION
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   static private List<MentionRelation> createRelationList( final String docId,
                                                            final Map<IdentifiedAnnotation, Mention> mentionMap,
                                                            final Collection<BinaryTextRelation> relations ) {
      return relations.stream()
                      .map( r -> createMentionRelation( r, mentionMap ) )
                      .filter( Objects::nonNull )
                      .sorted( RELATION_COMPARATOR )
                      .collect( Collectors.toList() );
   }


   static private MentionRelation createMentionRelation( final BinaryTextRelation relation,
                                                         final Map<IdentifiedAnnotation, Mention> mentionMap ) {
      final Mention source = getMention( relation.getArg1(), mentionMap );
      if ( source == null ) {
         return null;
      }
      final Mention target = getMention( relation.getArg2(), mentionMap );
      if ( target == null ) {
         return null;
      }
      final MentionRelation mentionRelation = new MentionRelation();
      mentionRelation.setType( relation.getCategory() );
      mentionRelation.setSourceId( source.getId() );
      mentionRelation.setTargetId( target.getId() );
      return mentionRelation;
   }


   static private Mention getMention( final RelationArgument argument,
                                      final Map<IdentifiedAnnotation, Mention> mentionMap ) {
      if ( argument == null ) {
         return null;
      }
      final Annotation annotation = argument.getArgument();
      if ( !(annotation instanceof IdentifiedAnnotation) ) {
         return null;
      }
      return mentionMap.get( annotation );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            COREF
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   static private List<MentionCoref> createCorefList( final String docId,
                                                      final Map<IdentifiedAnnotation, Mention> mentionMap,
                                                      final Map<IdentifiedAnnotation, Collection<Integer>> corefs ) {
      final Collection<List<IdentifiedAnnotation>> corefChains = createCorefChains( corefs );

      final List<MentionCoref> corefList
            = corefChains.stream()
                         .map( c -> createMentionCoref( c, mentionMap ) )
                         .filter( Objects::nonNull )
                         .sorted( COREF_COMPARATOR )
                         .collect( Collectors.toList() );
      int id = 1;
      for ( MentionCoref coref : corefList ) {
         coref.setId( docId + COREF_ID + id );
         id++;
      }
      return corefList;
   }


   static private MentionCoref createMentionCoref( final List<IdentifiedAnnotation> chain,
                                                   final Map<IdentifiedAnnotation, Mention> mentionMap ) {
      final String[] idChain = chain.stream()
                                    .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                    .map( mentionMap::get )
                                    .filter( Objects::nonNull )
                                    .map( Mention::getId )
                                    .toArray( String[]::new );
      if ( idChain.length <= 1 ) {
         return null;
      }
      final MentionCoref coref = new MentionCoref();
      coref.setIdChain( idChain );
      return coref;
   }


   static private Collection<List<IdentifiedAnnotation>> createCorefChains(
         final Map<IdentifiedAnnotation, Collection<Integer>> corefs ) {
      final Map<Integer, List<IdentifiedAnnotation>> reconfig = new HashMap<>();
      for ( Map.Entry<IdentifiedAnnotation, Collection<Integer>> corefEntry : corefs.entrySet() ) {
         for ( Integer chain : corefEntry.getValue() ) {
            reconfig.computeIfAbsent( chain, c -> new ArrayList<>() ).add( corefEntry.getKey() );
         }
      }
      return reconfig.values();
   }




}
