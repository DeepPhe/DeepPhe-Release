package org.healthnlp.deepphe.nlp.summary;


import org.apache.ctakes.core.util.IdCounter;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.neo4j.node.Codification;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.util.IdCreator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_VALUE_SCHEME;

/**
 * @author SPF , chip-nlp
 * @since {10/23/2023}
 */
final public class MentionCreator {

   private MentionCreator() {}

   static private final IdCounter ID_COUNTER = new IdCounter();

   static public void resetCounter() {
      ID_COUNTER.reset();
   }

   static public Mention createMention( final IdentifiedAnnotation annotation,
                                        final String patientId, final String docId, final String patientTime ) {
      final Mention mention = new Mention();
      final String uri = IdentifiedAnnotationUtil.getCodes( annotation, Neo4jConstants.DPHE_CODING_SCHEME )
                                                 .stream().findFirst().orElse( "" );
      mention.setClassUri( uri );
      DpheGroup group = UriInfoCache.getInstance().getDpheGroup( uri );
      if ( group == DpheGroup.UNKNOWN ) {
         LogFileWriter.add( "MentionCreator no group for " + uri );
         group = DpheGroup.getBestAnnotationGroup( annotation );
      }
      double confidence = annotation.getConfidence();
      if ( Double.isNaN( confidence ) || confidence <= 0 ) {
         confidence = 0;
      }
      mention.setDpheGroup( group.getName() );
//      mention.setPreferredText( UriInfoCache.INSTANCE.getPrefText( uri ) );
      mention.setdConfidence( confidence );
      mention.setNegated( IdentifiedAnnotationUtil.isNegated( annotation ) );
      mention.setUncertain( IdentifiedAnnotationUtil.isUncertain( annotation ) );
      mention.setHistoric( IdentifiedAnnotationUtil.isHistoric( annotation ) );
//      mention.setBegin( annotation.getBegin() );
//      mention.setEnd( annotation.getEnd() );
      mention.setSpan( annotation.getBegin(), annotation.getEnd() );
      mention.setText( annotation.getCoveredText() );
//      String temporality = "";
//      if ( annotation instanceof EventMention ) {
//         final Event event = ((EventMention)annotation).getEvent();
//         if ( event != null ) {
//            final EventProperties eventProperties = event.getProperties();
//            if ( eventProperties != null ) {
//               final String dtr = eventProperties.getDocTimeRel();
//               if ( dtr != null && !dtr.isEmpty() ) {
//                  temporality = dtr;
//               }
//            }
//         }
//      }
//      mention.setTemporality( temporality );
      final String fullMentionId = IdCreator.createId( patientId, patientTime, "M", ID_COUNTER );
      mention.setId( fullMentionId );
      final List<Codification> codifications = new ArrayList<>();
      // for dphe cuis are stored in the concept array.
      for ( String scheme : IdentifiedAnnotationUtil.getCodeSchemes( annotation ) ) {
         if ( scheme.equals( Neo4jConstants.DPHE_CODING_SCHEME )
               || scheme.equals( DpheGroup.DPHE_GROUPING_SCHEME )
               || scheme.equals( DPHE_VALUE_SCHEME ) ) {
            continue;
         }
         codifications.add( createCodification( scheme, IdentifiedAnnotationUtil.getCodes( annotation, scheme ) ) );
      }
      mention.setCodifications( codifications );
      final String value = IdentifiedAnnotationUtil.getCodes( annotation, DPHE_VALUE_SCHEME )
                                                   .stream()
                                                   .filter( Objects::nonNull )
            .distinct()
                                                   .collect( Collectors.joining(";") );
      mention.setValue( value );
      return mention;
   }

   static private Codification createCodification( final String source, final Collection<String> codes ) {
      final Codification codification = new Codification();
      codification.setSource( source );
      codification.setCodes( new ArrayList<>( codes ) );
      return codification;
   }


   static public Map<IdentifiedAnnotation, Mention> createAnnotationMentionMap( final JCas docCas,
                                                                                final String patientId,
                                                                                final String docId,
                                                                                final String patientTime ) {
      return JCasUtil.select( docCas, IdentifiedAnnotation.class ).stream()
                      .filter( a -> DpheGroup.getBestAnnotationGroup( a ) != DpheGroup.UNKNOWN )
                     .filter( a -> !CONST.ATTR_SUBJECT_FAMILY_MEMBER.equals( a.getSubject() ) )
                     .collect( Collectors.toMap( Function.identity(), a -> MentionCreator.createMention( a, patientId, docId, patientTime ) ) );

   }

   static public List<Mention> sortMentions(final Collection<Mention> mentions ) {
      return mentions.stream().sorted( MENTION_COMPARATOR ).collect( Collectors.toList() );
   }
//   static public Map<IdentifiedAnnotation, Mention> createAnnotationMentionMap(
//         final Collection<IdentifiedAnnotation> annotations ) {
//      return annotations.stream()
//                        .collect( Collectors.toMap( Function.identity(), MentionCreator::createMention ) );
//   }

   static public List<String> sortMentionIds( final Collection<Mention> mentions ) {
      return mentions.stream().sorted( MENTION_COMPARATOR ).map( Mention::getId ).collect(Collectors.toList());
   }


   static private final Comparator<Mention> MENTION_COMPARATOR = (a1, a2) -> {
//      final int typeCompare = String.CASE_INSENSITIVE_ORDER.compare( a1.getDpheGroup(), a2.getDpheGroup() );
//      if ( typeCompare != 0 ) {
//         return typeCompare;
//      }
      final int beginCompare = Integer.compare( a1.getBegin(), a2.getBegin() );
      if ( beginCompare != 0 ) {
         return beginCompare;
      }
      return Integer.compare( a1.getEnd(), a2.getEnd() );
   };

}
