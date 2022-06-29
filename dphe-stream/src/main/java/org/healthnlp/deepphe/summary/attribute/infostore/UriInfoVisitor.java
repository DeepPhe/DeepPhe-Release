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
               NeoplasmSummaryCreator.addDebug( "No Note for attribute " + attribute.getUri()
                                                + " note " + noteMentions.getKey() + "\n" );
               continue;
            }
            final List<Section> sections = note.getSections();
            if ( sections == null || sections.isEmpty() ) {
               NeoplasmSummaryCreator.addDebug( "No  Sections for attribute " + attribute.getUri() 
                                                + " note " + noteMentions.getKey() + "\n" );
               continue;
            }
            for ( Section section : sections ) {
               final String type = section.getType();
               if ( type == null || type.isEmpty() ) {
                  NeoplasmSummaryCreator.addDebug( "No section type for attribute " + attribute.getUri() 
                                                   + " note "+ noteMentions.getKey() + " section " + section.getId()  
                                                   + "\n" );
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
                  NeoplasmSummaryCreator.addDebug( "Other section for attribute "  +  attribute.getUri()
                                                 +  " note "
                                                 +  noteMentions.getKey()
                                                 +  " section "
                                                 +  type
                                                 +  "\n" );
               }
            }
         }
      }
      for ( Map.Entry<String,Integer> uriCounts : diagnosisUris.entrySet() ) {
         final Integer strength = strengths.get( uriCounts.getKey() );
         if ( strength == null || strength < 25 ) {
            NeoplasmSummaryCreator.addDebug( "No or low strength for uri "
                                           +  uriCounts.getKey()
                                           +  " strength "
                                           +  strength
                                           +  "\n" );
            continue;
         }
         NeoplasmSummaryCreator.addDebug( "    Adding "
                                        +  (uriCounts.getValue()*10)
                                        +  " to diagnosis "
                                        +  uriCounts.getKey()
                                        +  " "
                                        +  strength
                                        +  "\n" );
         strengths.put( uriCounts.getKey(), strength + (uriCounts.getValue()*10) );
      }
      for ( Map.Entry<String,Integer> uriCounts : historyUris.entrySet() ) {
         final Integer strength = strengths.get( uriCounts.getKey() );
         NeoplasmSummaryCreator.addDebug( "    Adding 0 to history "
                                        +  uriCounts.getKey()
                                        +  " "
                                        +  strength
                                        +  "\n" );
      }
      for ( Map.Entry<String,Integer> uriCounts : microscopicUris.entrySet() ) {
         final String uri = uriCounts.getKey();;
         final Integer strength = strengths.get( uri );
         if ( strength == null ) {
            continue;
         }
         if ( diagnosisUris.containsKey( uri ) || historyUris.containsKey( uri ) ) {
            NeoplasmSummaryCreator.addDebug( "    Ignoring "
                                           +  uriCounts.getValue()
                                           +  " microscopic mentions of "
                                           +  uri
                                           +  " as it is in diagnosis or history.\n" );
            continue;
         }
         NeoplasmSummaryCreator.addDebug( "    Subtracting "
                                        +  (uriCounts.getValue()*5)
                                        +  " to microscopic "
                                        +  uri
                                        +  " "
                                        +  strength
                                        +  "\n" );
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
            NeoplasmSummaryCreator.addDebug( "    Subtracting "
                                           +  ( uriCounts.getValue() * 5 )
                                           +  " to negated "
                                           +  uriCounts.getKey()
                                           +  " "
                                           +  strength
                                           +  "\n" );
            strengths.put( uriCounts.getKey(), strength - ( uriCounts.getValue() * 5 ) );
         }
      }
      for ( Map.Entry<String,Integer> uriCounts : historicUris.entrySet() ) {
         if ( nonHistoricUris.containsKey( uriCounts.getKey() ) ) {
            NeoplasmSummaryCreator.addDebug( "Uri "
                                           +  uriCounts.getKey()
                                           +  " has "
                                           +  nonHistoricUris.get( uriCounts.getKey() )
                                           +  " non-historic, "
                                           +  historicUris.get( uriCounts.getKey() )
                                           +  " historic\n" );
            continue;
         }
         final Integer strength = strengths.get( uriCounts.getKey() );
         if ( strength == null || strength < 25 ) {
            NeoplasmSummaryCreator.addDebug( "No or low strength for historic uri "
                                           +  uriCounts.getKey()
                                           +  " strength "
                                           +  strength
                                           +  "\n" );
            continue;
         }
         NeoplasmSummaryCreator.addDebug( "    Subtracting "
                                        +  (uriCounts.getValue()*5)
                                        +  " to historic "
                                        +  uriCounts.getKey()
                                        +  " "
                                        +  strength
                                        +  "\n" );
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
