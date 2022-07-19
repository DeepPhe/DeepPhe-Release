package org.healthnlp.deepphe.node;


import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Section;
import org.healthnlp.deepphe.summary.engine.MentionCreator;
import org.healthnlp.deepphe.summary.engine.MentionRelationCreator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public class NoteNodeCreator {

   public static final String NO_CATEGORY = "unknown";

   static private final String SECTION_ID = "_S_";
   static private final String MENTION_ID = "_M_";
   static private final String RELATION_ID = "_R_";
//   static private final String COREF_ID = "_C_";


//   static private final Comparator<MentionCoref> COREF_COMPARATOR = ( c1, c2 ) -> {
//      final String[] id1 = c1.getIdChain();
//      final String[] id2 = c2.getIdChain();
//      for ( int i = 0; i < Math.min( id1.length, id2.length ); i++ ) {
//         final int idCompare = SUFFIX_COMPARATOR.compare( id1[ i ], id2[ i ] );
//         if ( idCompare != 0 ) {
//            return idCompare;
//         }
//      }
//      return id1.length - id2.length;
//   };

   static public Note createNote( final JCas jCas ) {
      final Note note = new Note();
      final NoteSpecs noteSpecs = new NoteSpecs( jCas );
      final String docId = noteSpecs.getDocumentId();
      final String fullDocId = docId + "_" + noteSpecs.getDocumentText().hashCode();
      note.setId( fullDocId );
      note.setName( docId );
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

      final Map<IdentifiedAnnotation, Mention> mentionMap = MentionCreator.createMentionMap( fullDocId,
                                                                                             requiredAnnotations );

//      final List<Mention> mentionList = createMentionList( docId, mentionMap.values() );
//      note.setMentions( mentionList );
      note.setMentions( new ArrayList<>( mentionMap.values() ) );
      final List<MentionRelation> relationList
            = MentionRelationCreator.createRelationList( docId,
                                                         mentionMap,
                                                         JCasUtil.select( jCas, BinaryTextRelation.class ) );
      note.setRelations( relationList );

//      final List<MentionCoref> corefList = createCorefList( docId, mentionMap, corefs );
//      note.setCorefs( corefList );

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
                                                .map( NoteNodeCreator::createSection )
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


//   static private List<Mention> createMentionList( final String docId, final Collection<Mention> mentions ) {
//      final List<Mention> mentionList
//            = mentions.stream()
//                      .sorted( Comparator.comparingInt( Mention::getBegin ) )
//                      .collect( Collectors.toList() );
//      int id = 1;
//      for ( Mention mention : mentionList ) {
//         mention.setNoteId( docId );
//         mention.setId( docId + MENTION_ID + id );
//         id++;
//      }
//      return mentionList;
//   }



   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            RELATION
   //
   /////////////////////////////////////////////////////////////////////////////////////////




   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            COREF
   //
   /////////////////////////////////////////////////////////////////////////////////////////


//   static private List<MentionCoref> createCorefList( final String docId,
//                                                      final Map<IdentifiedAnnotation, Mention> mentionMap,
//                                                      final Map<IdentifiedAnnotation, Collection<Integer>> corefs ) {
//      final Collection<List<IdentifiedAnnotation>> corefChains = createCorefChains( corefs );
//
//      final List<MentionCoref> corefList
//            = corefChains.stream()
//                         .map( c -> createMentionCoref( c, mentionMap ) )
//                         .filter( Objects::nonNull )
//                         .sorted( COREF_COMPARATOR )
//                         .collect( Collectors.toList() );
//      int id = 1;
//      for ( MentionCoref coref : corefList ) {
//         coref.setId( docId + COREF_ID + id );
//         id++;
//      }
//      return corefList;
//   }
//
//
//   static private MentionCoref createMentionCoref( final List<IdentifiedAnnotation> chain,
//                                                   final Map<IdentifiedAnnotation, Mention> mentionMap ) {
//      final String[] idChain = chain.stream()
//                                    .sorted( Comparator.comparingInt( Annotation::getBegin ) )
//                                    .map( mentionMap::get )
//                                    .filter( Objects::nonNull )
//                                    .map( Mention::getId )
//                                    .toArray( String[]::new );
//      if ( idChain.length <= 1 ) {
//         return null;
//      }
//      final MentionCoref coref = new MentionCoref();
//      coref.setIdChain( idChain );
//      return coref;
//   }
//
//
//   static private Collection<List<IdentifiedAnnotation>> createCorefChains(
//         final Map<IdentifiedAnnotation, Collection<Integer>> corefs ) {
//      final Map<Integer, List<IdentifiedAnnotation>> reconfig = new HashMap<>();
//      for ( Map.Entry<IdentifiedAnnotation, Collection<Integer>> corefEntry : corefs.entrySet() ) {
//         for ( Integer chain : corefEntry.getValue() ) {
//            reconfig.computeIfAbsent( chain, c -> new ArrayList<>() ).add( corefEntry.getKey() );
//         }
//      }
//      return reconfig.values();
//   }


}
