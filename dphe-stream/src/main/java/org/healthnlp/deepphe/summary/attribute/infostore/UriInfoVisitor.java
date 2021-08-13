package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.core.document.SectionType;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Section;
import org.healthnlp.deepphe.nlp.ae.HistoryAdjuster;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.stream.Collectors;

public interface UriInfoVisitor {

   Collection<ConceptAggregate> getAttributeConcepts( Collection<ConceptAggregate> neoplasms );

   // Topography Minor,  Histology and Grade

//   default boolean applySectionStrengths() {
//      return false;
//   }

   default Collection<String> getAllAttributeUris( final Collection<ConceptAggregate> attributes ) {
      return getAttributeConcepts( attributes )
            .stream()
            .map( ConceptAggregate::getAllUris )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   // Allows for uris that have tied quotients
   default Collection<String> getMainAttributeUris( final Collection<ConceptAggregate> attributes ) {
      return getAttributeConcepts( attributes )
            .stream()
            .map( ConceptAggregate::getUri )
            .collect( Collectors.toSet() );
   }

   default Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> attributes = getAttributeConcepts( neoplasms );
      if ( attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<String> allUris = attributes.stream()
//                                                 .map( ConceptAggregate::getAllUris )
                                                   .map( ConceptAggregate::getMentions )
                                                   .flatMap( Collection::stream )
                                                   .filter( m -> !m.isNegated() )
                                                   .map( Mention::getClassUri )
                                                 .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = attributes.stream()
                                                                 .map( ConceptAggregate::getUriRootsMap )
                                                                 .map( Map::entrySet )
                                                                 .flatMap( Collection::stream )
                                                                 .distinct()
                                                                 .collect( Collectors.toMap( Map.Entry::getKey,
                                                                                             Map.Entry::getValue ) );
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<Mention> allMentions = attributes.stream()
                                                      .map( ConceptAggregate::getMentions )
                                                      .flatMap( Collection::stream )
                                                        .filter( m -> !m.isNegated() )
                                                        .collect( Collectors.toSet() );
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue() * 100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      applySectionAttributeUriStrengths( attributes, uriStrengths );
      applyHistoryAttributeUriStrengths( attributes, uriStrengths );
      return uriStrengths;
   }

   default Map<String,Integer> getAttributeUriMaxStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      if ( concepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final List<KeyValue<String, Double>> uriQuotients
            = concepts.stream()
                      .map( ConceptAggregate::getUriQuotients )
                      .flatMap( Collection::stream )
                          .collect( Collectors.toList() );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue() * 100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      return uriStrengths;
   }

   default Map<String,Integer> getAttributeUriAveStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      if ( concepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final List<KeyValue<String, Double>> uriQuotients
            = concepts.stream()
                      .map( ConceptAggregate::getUriQuotients )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toList() );
      final Map<String,Collection<Double>> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         uriStrengths.computeIfAbsent( quotients.getKey(), s -> new ArrayList<>() )
                     .add( quotients.getValue() );
      }
      return uriStrengths.entrySet()
                  .stream()
                  .collect( Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (int)Math.ceil( e.getValue()
                              .stream()
                              .mapToDouble( d -> d )
                              .average()
                              .orElse( 0 ) ) * 100 ) );
   }


   static public Map<String,Integer> applySectionAttributeUriStrengths( final Collection<ConceptAggregate> attributes,
                                                                  final Map<String,Integer> strengths ) {
//      if ( !applySectionStrengths() || strengths.isEmpty() ) {
      if ( strengths.isEmpty() ) {
         return strengths;
      }
      final Map<String,Integer> diagnosisUris = new HashMap<>();
      final Map<String,Integer> historyUris = new HashMap<>();
      final Map<String,Integer> microscopicUris = new HashMap<>();
      for ( ConceptAggregate attribute : attributes ) {
         final Map<String, Collection<Mention>> noteMentionMap = attribute.getNoteMentions();
         for ( Map.Entry<String, Collection<Mention>> noteMentions : noteMentionMap.entrySet() ) {
            final Note note = NoteNodeStore.getInstance()
                                           .get( noteMentions.getKey() );
            if ( note == null ) {
               NeoplasmSummaryCreator.DEBUG_SB.append( "No Note for attribute " )
                                              .append( attribute.getUri() )
                                              .append( " note " )
                                              .append( noteMentions.getKey() )
                                              .append( "\n" );
               continue;
            }
            final List<Section> sections = note.getSections();
            if ( sections == null || sections.isEmpty() ) {
               NeoplasmSummaryCreator.DEBUG_SB.append( "No  Sections for attribute " )
                                              .append( attribute.getUri() )
                                              .append( " note " )
                                              .append( noteMentions.getKey() )
                                              .append( "\n" );
               continue;
            }
            for ( Section section : sections ) {
               final String type = section.getType();
               if ( type == null || type.isEmpty() ) {
                  NeoplasmSummaryCreator.DEBUG_SB.append( "No section type for attribute " )
                                                 .append( attribute.getUri() )
                                                 .append( " note " )
                                                 .append( noteMentions.getKey() )
                                                 .append( " section " )
                                                 .append( section.getId()  )
                                                 .append( "\n" );
                  continue;
               }
               if ( type.equals( SectionType.FinalDiagnosis.getName() )
                    || type.equals( "Clinical Diagnosis and History" ) ) {
                  final int sBegin = section.getBegin();
                  final int sEnd = section.getEnd();
                  noteMentions.getValue()
                              .stream()
                              .filter( m -> !m.isNegated() )
                              .filter( m -> m.getBegin() >= sBegin && m.getEnd() <= sEnd )
                              .map( Mention::getClassUri )
                              .forEach( u -> diagnosisUris.compute( u, ( k, v ) -> ( v == null )
                                                                                   ? 1
                                                                                   : v + 1 ) );
               } else if ( HistoryAdjuster.isHistoric( type ) ) {
                  final int sBegin = section.getBegin();
                  final int sEnd = section.getEnd();
                  noteMentions.getValue()
                              .stream()
                              .filter( m -> !m.isNegated() )
                              .filter( m -> m.getBegin() >= sBegin && m.getEnd() <= sEnd )
                              .map( Mention::getClassUri )
                              .forEach( u -> historyUris.compute( u, ( k, v ) -> ( v == null )
                                                                                 ? 1
                                                                                 : v + 1 ) );
               } else if ( type.startsWith( "Microscopic" )
                           || type.equals( "Nature of Specimen" )
                           || type.equals( "Gross Description" ) ) {
                  final int sBegin = section.getBegin();
                  final int sEnd = section.getEnd();
                  noteMentions.getValue()
                              .stream()
                              .filter( m -> !m.isNegated() )
                              .filter( m -> m.getBegin() >= sBegin && m.getEnd() <= sEnd )
                              .map( Mention::getClassUri )
                              .forEach( u -> microscopicUris.compute( u, (k,v) -> (v == null) ? 1 : v+1) );
               } else {
                  NeoplasmSummaryCreator.DEBUG_SB.append( "Other section for attribute " )
                                                 .append( attribute.getUri() )
                                                 .append( " note " )
                                                 .append( noteMentions.getKey() )
                                                 .append( " section " )
                                                 .append( type )
                                                 .append( "\n" );
               }
            }
         }
      }
      for ( Map.Entry<String,Integer> uriCounts : diagnosisUris.entrySet() ) {
         final Integer strength = strengths.get( uriCounts.getKey() );
         if ( strength == null || strength < 25 ) {
            NeoplasmSummaryCreator.DEBUG_SB.append( "No or low strength for uri " )
                                           .append( uriCounts.getKey() )
                                           .append( " strength " )
                                           .append( strength )
                                           .append( "\n" );
            continue;
         }
         NeoplasmSummaryCreator.DEBUG_SB.append( "    Adding " )
                                        .append( (uriCounts.getValue()*10) )
                                        .append( " to diagnosis " )
                                        .append( uriCounts.getKey() )
                                        .append( " " )
                                        .append( strength )
                                        .append( "\n" );
         strengths.put( uriCounts.getKey(), strength + (uriCounts.getValue()*10) );
      }
      for ( Map.Entry<String,Integer> uriCounts : historyUris.entrySet() ) {
         final Integer strength = strengths.get( uriCounts.getKey() );
         NeoplasmSummaryCreator.DEBUG_SB.append( "    Adding 0 to history " )
                                        .append( uriCounts.getKey() )
                                        .append( " " )
                                        .append( strength )
                                        .append( "\n" );
      }
      for ( Map.Entry<String,Integer> uriCounts : microscopicUris.entrySet() ) {
         final String uri = uriCounts.getKey();;
         final Integer strength = strengths.get( uri );
         if ( strength == null ) {
            continue;
         }
         if ( diagnosisUris.containsKey( uri ) || historyUris.containsKey( uri ) ) {
            NeoplasmSummaryCreator.DEBUG_SB.append( "    Ignoring " )
                                           .append( uriCounts.getValue() )
                                           .append( " microscopic mentions of " )
                                           .append( uri )
                                           .append( " as it is in diagnosis or history.\n" );
            continue;
         }
         NeoplasmSummaryCreator.DEBUG_SB.append( "    Subtracting " )
                                        .append( (uriCounts.getValue()*5) )
                                        .append( " to microscopic " )
                                        .append( uri )
                                        .append( " " )
                                        .append( strength )
                                        .append( "\n" );
         strengths.put( uri, strength - (uriCounts.getValue()*5) );
      }
      return strengths;
   }

   static public Map<String,Integer> applyHistoryAttributeUriStrengths( final Collection<ConceptAggregate> attributes,
                                                                  final Map<String,Integer> strengths ) {
//      if ( !applySectionStrengths() || strengths.isEmpty() ) {
      if ( strengths.isEmpty() ) {
         return strengths;
      }
      final Map<String,Integer> historicUris = new HashMap<>();
      final Map<String,Integer> nonHistoricUris = new HashMap<>();
      final Map<String,Integer> negatedUris = new HashMap<>();
      for ( ConceptAggregate attribute : attributes ) {
         for ( Mention mention : attribute.getMentions() ) {
            if ( mention.isNegated() ) {
               negatedUris.compute( mention.getClassUri(), ( k, v ) -> ( v == null )
                                                                        ? 1
                                                                        : v + 1 );
            }
            if ( mention.isHistoric() ) {
               historicUris.compute( mention.getClassUri(), ( k, v ) -> ( v == null )
                                                                        ? 1
                                                                        : v + 1 );
            } else {
               nonHistoricUris.compute( mention.getClassUri(), ( k, v ) -> ( v == null )
                                                                        ? 1
                                                                        : v + 1 );
            }
         }
      }
      for ( Map.Entry<String,Integer> uriCounts : negatedUris.entrySet() ) {
         final Integer strength = strengths.get( uriCounts.getKey() );
         if ( strength != null ) {
            NeoplasmSummaryCreator.DEBUG_SB.append( "    Subtracting " )
                                           .append( ( uriCounts.getValue() * 5 ) )
                                           .append( " to negated " )
                                           .append( uriCounts.getKey() )
                                           .append( " " )
                                           .append( strength )
                                           .append( "\n" );
            strengths.put( uriCounts.getKey(), strength - ( uriCounts.getValue() * 5 ) );
         }
      }
      for ( Map.Entry<String,Integer> uriCounts : historicUris.entrySet() ) {
         if ( nonHistoricUris.containsKey( uriCounts.getKey() ) ) {
            NeoplasmSummaryCreator.DEBUG_SB.append( "Uri " )
                                           .append( uriCounts.getKey() )
                                           .append( " has " )
                                           .append( nonHistoricUris.get( uriCounts.getKey() ) )
                                           .append( " non-historic, " )
                                           .append( historicUris.get( uriCounts.getKey() ) )
                                           .append( " historic\n" );
            continue;
         }
         final Integer strength = strengths.get( uriCounts.getKey() );
         if ( strength == null || strength < 25 ) {
            NeoplasmSummaryCreator.DEBUG_SB.append( "No or low strength for historic uri " )
                                           .append( uriCounts.getKey() )
                                           .append( " strength " )
                                           .append( strength )
                                           .append( "\n" );
            continue;
         }
         NeoplasmSummaryCreator.DEBUG_SB.append( "    Subtracting " )
                                        .append( (uriCounts.getValue()*5) )
                                        .append( " to historic " )
                                        .append( uriCounts.getKey() )
                                        .append( " " )
                                        .append( strength )
                                        .append( "\n" );
         strengths.put( uriCounts.getKey(), strength - (uriCounts.getValue()*5) );
      }
       return strengths;
   }


//   final class UriStrength {
//      final public String _uri;
//      final public int _sumStrength;
//      final public int _maxStrength;
//      final public int _aveStrength;
//      private UriStrength( final String uri,
//                           final double sumQuotient,
//                           final List<KeyValue<String, Double>> uriQuotients ) {
//         _uri = uri;
//         _sumStrength = (int)Math.ceil( sumQuotient * 100 );
//         _maxStrength = (int)Math.ceil( uriQuotients.stream()
//                                                    .filter( k -> k.getKey().equals( uri ) )
//                                                    .mapToDouble( KeyValue::getValue )
//                                                    .max()
//                                                    .orElse( 0 ) ) * 100;
//         _aveStrength = (int)Math.ceil( uriQuotients.stream()
//                     .filter( k -> k.getKey().equals( uri ) )
//                     .mapToDouble( KeyValue::getValue )
//                                               .average()
//                                               .orElse( 0 ) ) * 100;
//      }
//   }


}
