package org.healthnlp.deepphe.nlp.summary;

import org.apache.ctakes.core.util.IdCounter;
import org.apache.ctakes.core.util.StringAndNumberComparator;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {10/24/2023}
 */
final public class MentionRelationCreator {

   private MentionRelationCreator() {}

   static private final IdCounter ID_COUNTER = new IdCounter();

   static public void resetCounter() {
      ID_COUNTER.reset();
   }

   static public MentionRelation createRelation( final BinaryTextRelation relation,
                                                 final Map<IdentifiedAnnotation, Mention> annotationMentionMap ) {
      final Mention source = getMention( relation.getArg1(), annotationMentionMap );
      if ( source == null ) {
         LogFileWriter.add( "MentionRelationCreator No Source Mention to create relation " + relation.getArg1().getArgument().getCoveredText() );
         return null;
      }
      final Mention target = getMention( relation.getArg2(), annotationMentionMap );
      if ( target == null ) {
         LogFileWriter.add( "MentionRelationCreator No Target Mention to create relation " + relation.getArg2().getArgument().getCoveredText() );
         return null;
      }
      final MentionRelation mentionRelation = new MentionRelation();
      mentionRelation.setType( relation.getCategory() );
      mentionRelation.setSourceId( source.getId() );
      mentionRelation.setTargetId( target.getId() );
      mentionRelation.setdConfidence( relation.getConfidence() );
//      LogFileWriter.add( "MentionRelationCreator New MentionRelation " + mentionRelation.getType() + " "
////            + mentionRelation.getSourceId() + " " + mentionRelation.getTargetId() + " "
//            + source.getPreferredText() + " " + target.getPreferredText() + " "
//            + mentionRelation.getConfidence() );
      return mentionRelation;
   }

   static private Mention getMention( final RelationArgument argument,
                                      final Map<IdentifiedAnnotation, Mention> annotationMentionMap ) {
      if ( argument == null ) {
         return null;
      }
      final Annotation annotation = argument.getArgument();
      if ( !(annotation instanceof IdentifiedAnnotation) ) {
         return null;
      }
      return annotationMentionMap.get( annotation );
   }

   static public List<MentionRelation> createRelationList( final Map<IdentifiedAnnotation, Mention> annotationMentionMap,
                                                           final JCas docCas ) {
      return JCasUtil.select( docCas, BinaryTextRelation.class ).stream()
                     .map( r -> MentionRelationCreator.createRelation( r, annotationMentionMap ) )
                     .filter( Objects::nonNull )
                     .filter( r -> r.getType() != null )
                     .filter( r -> !r.getType().isEmpty() )
                     .sorted( RELATION_COMPARATOR )
                     .collect( Collectors.toList() );
   }

//   static public List<MentionRelation> createRelationList( final Map<IdentifiedAnnotation, Mention> mentionMap,
//                                                           final Collection<BinaryTextRelation> relations ) {
//      return relations.stream()
//                      .map( r -> MentionRelationCreator.createRelation( r, mentionMap ) )
//                      .filter( Objects::nonNull )
//                      .filter( r -> r.getType() != null )
//                      .filter( r -> !r.getType().isEmpty() )
//                      .sorted( RELATION_COMPARATOR )
//                      .collect( Collectors.toList() );
//   }

   static private final StringAndNumberComparator SUFFIX_COMPARATOR = new StringAndNumberComparator();

   static private final Comparator<MentionRelation> RELATION_COMPARATOR = ( r1, r2 ) -> {
      final int typeCompare = String.CASE_INSENSITIVE_ORDER.compare( r1.getType(), r2.getType() );
      if ( typeCompare != 0 ) {
         return typeCompare;
      }
      final int sourceCompare = SUFFIX_COMPARATOR.compare( r1.getSourceId(), r2.getSourceId() );
      if ( sourceCompare != 0 ) {
         return sourceCompare;
      }
      return SUFFIX_COMPARATOR.compare( r1.getTargetId(), r2.getTargetId() );
   };

}
