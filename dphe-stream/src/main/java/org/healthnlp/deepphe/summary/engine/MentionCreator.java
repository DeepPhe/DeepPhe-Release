package org.healthnlp.deepphe.summary.engine;

import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {5/6/2021}
 */
final public class MentionCreator {

   private MentionCreator() {}

   static public Map<IdentifiedAnnotation, Mention> createMentionMap( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .filter( a -> !CONST.ATTR_SUBJECT_FAMILY_MEMBER.equals( a.getSubject() ) )
                        .collect( Collectors.toMap( Function.identity(),
                                                    MentionCreator::createMention ) );
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


}
