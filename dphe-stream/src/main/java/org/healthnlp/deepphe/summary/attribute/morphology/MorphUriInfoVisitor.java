//package org.healthnlp.deepphe.summary.attribute.morphology;
//
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.Note;
//import org.healthnlp.deepphe.neo4j.node.Section;
//import org.healthnlp.deepphe.node.NoteNodeStore;
//import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//final public class MorphUriInfoVisitor implements UriInfoVisitor {
//
//   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
//      return neoplasms.stream()
//                      .filter( c -> !c.isNegated() )
//                      .collect( Collectors.toSet() );
//   }
//
//   // More weight if the uri is in a diagnosis section
//   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
//      final Map<String,Integer> strengths = UriInfoVisitor.super.getAttributeUriStrengths( neoplasms );
//      if ( strengths.isEmpty() ) {
//         return strengths;
//      }
//      final Collection<String> diagnosisUris = new HashSet<>();
//      for ( ConceptAggregate neoplasm : neoplasms ) {
//         final Map<String, Collection<Mention>> noteMentionMap = neoplasm.getNoteMentions();
//         for ( Map.Entry<String, Collection<Mention>> noteMentions : noteMentionMap.entrySet() ) {
//            final Note note = NoteNodeStore.getInstance()
//                                           .get( noteMentions.getKey() );
//            if ( note == null ) {
//               continue;
//            }
//            final List<Section> sections = note.getSections();
//            if ( sections == null || sections.isEmpty() ) {
//               continue;
//            }
//            for ( Section section : sections ) {
//               if ( section.getType() != null && section.getType()
//                                                        .equals( "Final Diagnosis" ) ) {
//                  final int sBegin = section.getBegin();
//                  final int sEnd = section.getEnd();
//                  noteMentions.getValue()
//                              .stream()
//                              .filter( m -> m.getBegin() >= sBegin && m.getEnd() <= sEnd )
//                              .map( Mention::getClassUri )
//                              .forEach( diagnosisUris::add );
//               }
//            }
//         }
//      }
//      if ( diagnosisUris.isEmpty() ) {
//         return strengths;
//      }
//      for ( String uri : diagnosisUris ) {
//         final Integer strength = strengths.get( uri );
//         if ( strength == null || strength < 25 ) {
//            continue;
//         }
//         NeoplasmSummaryCreator.addDebug( "Adding 50 to diagnosis " )
//                                        .append( uri )
//                                        .append( " " )
//                                        .append( strength )
//                                        .append( "\n" );
//         strengths.put( uri, Math.min( 100, strength + 50 ) );
//      }
//      return strengths;
//   }
//
//}
