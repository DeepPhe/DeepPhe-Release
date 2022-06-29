package org.healthnlp.deepphe.summary.engine;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {5/6/2021}
 */
final public class MentionRelationCreator {

   static private final LongNumSuffixComparator SUFFIX_COMPARATOR = new LongNumSuffixComparator();

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

   static public List<MentionRelation> createRelationList( final String docId,
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


}
