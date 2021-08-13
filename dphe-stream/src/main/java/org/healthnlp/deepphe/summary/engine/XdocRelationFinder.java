//package org.healthnlp.deepphe.summary.engine;
//
//
//import org.apache.ctakes.typesystem.type.constants.CONST;
//import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
//import org.apache.ctakes.typesystem.type.textspan.Paragraph;
//import org.apache.ctakes.typesystem.type.textspan.Segment;
//import org.apache.log4j.Logger;
//import org.apache.uima.fit.util.JCasUtil;
//import org.apache.uima.jcas.JCas;
//import org.healthnlp.deepphe.core.document.SectionType;
//import org.healthnlp.deepphe.core.relation.RelationUtil;
//import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.Note;
//import org.healthnlp.deepphe.neo4j.util.Neo4jRelationUtil;
//import org.healthnlp.deepphe.neo4j.util.SearchUtil;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_LATERALITY;
//import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_TREATMENT;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 9/17/2020
// */
//public class XdocRelationFinder {
//
//   static private final Logger LOGGER = Logger.getLogger( "XdocRelationFinder" );
//
//
//
////   static public void addRelations( final Collection<JCas> docCases,
////                                    final Collection<Collection<ConceptAggregate>> conceptAggregateSet ) {
////      LOGGER.info( "Finding Concept Relations ..." );
////      final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs
////            = mapDocSectionParagraphs( docCases );
////      // This will only contain identified annotations that are in valid sections.
////      final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations
////            = mapDocParagraphAnnotations( docCases, docSectionParagraphs );
////      final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances
////            = mapAnnotationConceptInstances( conceptAggregateSet, docParagraphAnnotations );
////      // Map of Map of  Map of Relation name to target URIs to collection of source URIs.
////      // MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis] ;
////      final Map<Map<String, Collection<String>>, Collection<String>> ciRelationNameTargetsToSources
////            = mapCiRelationToSources( annotationConceptInstances.values() );
////
////      final Map<IdentifiedAnnotation, Collection<String>> relationsDone = new HashMap<>();
////
////      processInDocRelations( docCases,
////            docSectionParagraphs,
////            docParagraphAnnotations,
////            annotationConceptInstances,
////            ciRelationNameTargetsToSources,
////            relationsDone );
////      processXdocRelations( annotationConceptInstances,
////            ciRelationNameTargetsToSources,
////            relationsDone );
////
////      addNonSpecificRelations( docCases,
////            docSectionParagraphs,
////            docParagraphAnnotations,
////            annotationConceptInstances,
////            relationsDone );
////   }
//
//
//   static public void addRelations( final Collection<Note> notes,
//                                    final Map<Mention, String> mentionNoteIds,
//                                    final Collection<Collection<ConceptAggregate>> conceptAggregateSet,
//                                    final Map<Mention, ConceptAggregate> mentionConceptAggregateMap ) {
//      LOGGER.info( "Finding Concept Relations ..." );
//      final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs
//            = mapDocSectionParagraphs( docCases );
//      // This will only contain identified annotations that are in valid sections.
//      final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations
//            = mapDocParagraphAnnotations( docCases, docSectionParagraphs );
//      // Map of Map of  Map of Relation name to target URIs to collection of source URIs.
//      // MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis] ;
//      final Map<Map<String, Collection<String>>, Collection<String>> ciRelationNameTargetsToSources
//            = mapCiRelationToSources( mentionConceptAggregateMap.values() );
//
//      final Map<Mention, Collection<String>> relationsDone = new HashMap<>();
//
//      processInDocRelations( docCases,
//            docSectionParagraphs,
//            docParagraphAnnotations,
//            mentionConceptAggregateMap,
//            ciRelationNameTargetsToSources,
//            relationsDone );
//      processXdocRelations( mentionConceptAggregateMap,
//            ciRelationNameTargetsToSources,
//            relationsDone );
//
//      addNonSpecificRelations( docCases,
//            docSectionParagraphs,
//            docParagraphAnnotations,
//            mentionConceptAggregateMap,
//            relationsDone );
//   }
//
//
//
//
//
//
//   static private void addNonSpecificRelations(
//         final Collection<JCas> docCases,
//         final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs,
//         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations,
//         final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances,
//         final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
//      // Map of Map of  Map of Relation name to target URIs to collection of source URIs.
//      // ANY and ALL relation target uris.  e.g. (location, [organ]),[cancer, disease] ;
//      final Map<Map<String, Collection<String>>, Collection<String>> allRelationNameTargetsToSources
//            = mapMentionRelationToSources( annotationConceptInstances.keySet() );
//
//      final Map<IdentifiedAnnotation, Collection<String>> allRelationsDone
//            = mapMentionRelationsDone( annotationConceptInstances, relationsDone );
//
//      processInDocRelations( docCases,
//            docSectionParagraphs,
//            docParagraphAnnotations,
//            annotationConceptInstances,
//            allRelationNameTargetsToSources,
//            allRelationsDone );
//      processXdocRelations( annotationConceptInstances,
//            allRelationNameTargetsToSources,
//            allRelationsDone );
//   }
//
//
////   static private Map<IdentifiedAnnotation, ConceptAggregate> mapAnnotationConceptInstances(
////         final Collection<Collection<ConceptAggregate>> conceptInstanceSet,
////         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations ) {
////      final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances
////            = createAnnotationConceptInstanceMap( conceptInstanceSet, docParagraphAnnotations );
////      docParagraphAnnotations.values().stream()
////                             .map( Map::values )
////                             .flatMap( Collection::stream )
////                             .forEach( c -> c.retainAll( annotationConceptInstances.keySet() ) );
////      return annotationConceptInstances;
////   }
//
//
//   static private void processInDocRelations(
//         final Collection<JCas> docCases,
//         final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs,
//         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations,
//         final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances,
//         final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//         final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
//      for ( JCas docCas : docCases ) {
//         final Map<Segment, Collection<Paragraph>> sectionParagraphMap = docSectionParagraphs.get( docCas );
//         if ( sectionParagraphMap == null || sectionParagraphMap.isEmpty() ) {
//            continue;
//         }
//         final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap
//               = docParagraphAnnotations.get( docCas );
//         processInDocRelations( docCas,
//               sectionParagraphMap,
//               paragraphAnnotationMap,
//               annotationConceptInstances,
//               relationNameTargetsToSources,
//               relationsDone );
//      }
//   }
//
//   static private void processInDocRelations(
//         final JCas docCas,
//         final Map<Segment, Collection<Paragraph>> sectionParagraphMap,
//         final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap,
//         final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances,
//         final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//         final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
//      relationsDone.putAll( processDocRelations( docCas,
//            annotationConceptInstances,
//            relationNameTargetsToSources,
//            sectionParagraphMap,
//            paragraphAnnotationMap ) );
//   }
//
//
//   static private Collection<IdentifiedAnnotation> getValidAnnotations(
//         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations ) {
//      return docParagraphAnnotations.values().stream()
//                                    .map( Map::values )
//                                    .flatMap( Collection::stream )
//                                    .flatMap( Collection::stream )
//                                    .filter( a -> a.getPolarity() != CONST.NE_CERTAINTY_NEGATED )
//                                    .collect( Collectors.toSet() );
//   }
//
//
////   static private Map<IdentifiedAnnotation, ConceptAggregate> createAnnotationConceptInstanceMap(
////         final Collection<Collection<ConceptAggregate>> conceptInstanceSet,
////         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations ) {
////      final Collection<IdentifiedAnnotation> allValidAnnotations = getValidAnnotations( docParagraphAnnotations );
////      final Map<IdentifiedAnnotation, ConceptAggregate> map = new HashMap<>();
////      for ( Collection<ConceptAggregate> conceptInstances : conceptInstanceSet ) {
////         for ( ConceptAggregate ci : conceptInstances ) {
////            ci.getAnnotations().stream()
////              .filter( allValidAnnotations::contains )
////              .forEach( a -> map.put( a, ci ) );
////         }
////      }
////      return map;
////   }
//
//
//   static private Map<JCas, Map<Segment, Collection<Paragraph>>> mapDocSectionParagraphs(
//         final Collection<JCas> docCases ) {
//      final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs
//            = new HashMap<>( docCases.size() );
//      for ( JCas docCas : docCases ) {
//         final Map<Segment, Collection<Paragraph>> sectionParagraphs = new HashMap<>();
//         JCasUtil.indexCovered( docCas, Segment.class, Paragraph.class ).entrySet().stream()
//                 .filter( e -> isWantedSection( e.getKey() ) )
//                 .forEach( e -> sectionParagraphs.put( e.getKey(), e.getValue() ) );
//         docSectionParagraphs.put( docCas, sectionParagraphs );
//      }
//      return docSectionParagraphs;
//   }
//
//
//   static private Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> mapDocParagraphAnnotations(
//         final Collection<JCas> docCases,
//         final Map<JCas, Map<Segment, Collection<Paragraph>>> docSectionParagraphs ) {
//      final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations
//            = new HashMap<>( docCases.size() );
//
//      for ( JCas docCas : docCases ) {
//         final Map<Segment, Collection<Paragraph>> sectionParagraphMap = docSectionParagraphs.get( docCas );
//         if ( sectionParagraphMap == null || sectionParagraphMap.isEmpty() ) {
//            continue;
//         }
//         final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotations
//               = JCasUtil.indexCovered( docCas, Paragraph.class, IdentifiedAnnotation.class );
//         final Map<Paragraph, Collection<IdentifiedAnnotation>> wantedParagraphAnnotations = new HashMap<>();
//         for ( Map.Entry<Paragraph, Collection<IdentifiedAnnotation>> entry : paragraphAnnotations.entrySet() ) {
//            final Collection<IdentifiedAnnotation> wantedAnnotations
//                  = entry.getValue().stream()
//                         .filter( a -> a.getPolarity() != CONST.NE_CERTAINTY_NEGATED )
//                         .collect( Collectors.toSet() );
//            wantedParagraphAnnotations.put( entry.getKey(), wantedAnnotations );
//         }
//         docParagraphAnnotations.put( docCas, wantedParagraphAnnotations );
//      }
//      return docParagraphAnnotations;
//   }
//
//
//   static private Collection<String> getCiUris(
//         final Map<JCas, Map<Paragraph, Collection<IdentifiedAnnotation>>> docParagraphAnnotations,
//         final Map<IdentifiedAnnotation, ConceptAggregate> annotationConceptInstances ) {
//      return docParagraphAnnotations.values().stream()
//                                    .map( Map::values )
//                                    .flatMap( Collection::stream )
//                                    .flatMap( Collection::stream )
//                                    .map( annotationConceptInstances::get )
//                                    .filter( Objects::nonNull )
//                                    .map( ConceptAggregate::getUri )
//                                    .collect( Collectors.toSet() );
//   }
//
//
//   static private Collection<String> getCiUris( final Collection<ConceptAggregate> conceptAggregates ) {
//      return conceptAggregates.stream()
//                             .map( ConceptAggregate::getUri )
//                             .collect( Collectors.toSet() );
//   }
//
//   static private Collection<String> getMentionUris(
//         final Collection<Mention> mentions ) {
//      return mentions.stream()
//                     .map( Mention::getClassUri )
//                     .collect( Collectors.toSet() );
//   }
//
//   static private Map<ConceptAggregate, Collection<String>> mapCiUris(
//         final Collection<ConceptAggregate> conceptAggregates ) {
//      return conceptAggregates.stream()
//                              .collect( Collectors.toMap( Function.identity(), c -> c.getMentions().stream()
//                                                    .map( Mention::getClassUri )
//                                                    .collect( Collectors.toSet() ) ) );
//   }
//
//   static private Map<String, Collection<String>> mapMentionCiUris(
//         final Collection<ConceptAggregate> conceptAggregates ) {
//      final Map<String, Collection<String>> mentionCiUris = new HashMap<>();
//      for ( ConceptAggregate conceptAggregate : conceptAggregates ) {
//         final String ciUri = conceptAggregate.getUri();
//         conceptAggregate.getMentions().stream()
//                 .map( Mention::getClassUri )
//                 .forEach( u -> mentionCiUris.computeIfAbsent( u, s -> new HashSet<>() )
//                                                .add( ciUri ) );
//      }
//      return mentionCiUris;
//   }
//
//
//   static private Map<Mention, Collection<String>> mapMentionRelationsDone(
//         final Map<Mention, ConceptAggregate> mentionConceptAggregates,
//         final Map<Mention, Collection<String>> relationsDone ) {
//      final Map<Mention, Collection<String>> allRelationsDone = new HashMap<>( relationsDone );
//      for ( Map.Entry<Mention, ConceptAggregate> mentionConceptAggregate : mentionConceptAggregates.entrySet() ) {
//         final Collection<String> mentionRelationsDone = relationsDone.get( mentionConceptAggregate.getKey() );
//         if ( mentionRelationsDone == null ) {
//            continue;
//         }
//         mentionConceptAggregate.getValue()
//                                  .getMentions()
//                                  .forEach( u -> allRelationsDone.computeIfAbsent( u, s -> new HashSet<>() )
//                                                                 .addAll( mentionRelationsDone ) );
//      }
//      return allRelationsDone;
//   }
//
//
//   static private Map<Mention, Collection<String>> processDocRelations(
//         final JCas docCas,
//         final Map<Mention, ConceptAggregate> mentionConceptAggregates,
//         final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//         final Map<Segment, Collection<Paragraph>> sectionParagraphMap,
//         final Map<Paragraph, Collection<Mention>> paragraphMentionsMap ) {
//      final Map<String, Collection<Mention>> docUriMentionsMap = new HashMap<>();
//      final Map<Mention, Collection<String>> relationsDone = new HashMap<>();
//
//      for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {
//         final Map<String, Collection<Mention>> sectionUriMentionMap = new HashMap<>();
//
//         final Collection<Paragraph> paragraphs = sectionParagraphs.getValue();
//         for ( Paragraph paragraph : paragraphs ) {
//            final Collection<Mention> annotations = paragraphMentionsMap.get( paragraph );
//            if ( annotations == null || annotations.isEmpty() ) {
//               continue;
//            }
//            final Map<String, Collection<Mention>> uriMentionMap
//                  = mapCiUriAnnotations( mentionConceptAggregates, annotations );
//            createWindowRelations( docCas, uriMentionMap, relationNameTargetsToSources, relationsDone );
//            for ( Map.Entry<String, Collection<Mention>> uriMentions : uriMentionMap.entrySet() ) {
//               sectionUriMentionMap.computeIfAbsent( uriMentions.getKey(),
//                     a -> new HashSet<>() ).addAll( uriMentions.getValue() );
//            }
//         }
//
//         if ( paragraphs.size() > 1 ) {
//            // We now have relations within paragraphs.  Try relations within the section for any leftovers.
//            createWindowRelations( docCas, sectionUriMentionMap, relationNameTargetsToSources, relationsDone );
//         }
//
//         for ( Map.Entry<String, Collection<Mention>> uriMentions : sectionUriMentionMap.entrySet() ) {
//            docUriMentionsMap.computeIfAbsent( uriMentions.getKey(),
//                  a -> new HashSet<>() ).addAll( uriMentions.getValue() );
//         }
//      }
//
//      if ( sectionParagraphMap.size() > 1 ) {
//         // There is more than 1 section in the document.
//         createWindowRelations( docCas, docUriMentionsMap, relationNameTargetsToSources, relationsDone );
//      }
//      return relationsDone;
//   }
//
//
//   static private void processXdocRelations(
//         final Map<Mention, ConceptAggregate> mentionConceptAggregateMap,
//         final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//         final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
//
//      final Map<String, Collection<Mention>> uriMentionMap
//            = mapCiUriAnnotations( mentionConceptAggregateMap, mentionConceptAggregateMap.keySet() );
//
//      createXdocRelations( mentionConceptAggregateMap, uriMentionMap, relationNameTargetsToSources, relationsDone );
//   }
//
//
//   static private void createXdocRelations( final Map<Mention, ConceptAggregate> mentionConceptAggregateMap,
//                                            final Map<String, Collection<Mention>> uriMentionMap,
//                                            final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//                                            final Map<Mention, Collection<String>> relationsDone ) {
//      if ( uriMentionMap.isEmpty() ) {
//         return;
//      }
//      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSourcesEntry
//            : relationNameTargetsToSources.entrySet() ) {
//         final Map<String, Collection<String>> relationNameTargetsMap = relationNameTargetsToSourcesEntry.getKey();
//         // collatedSources ...  This should be ok as each set of sources is unique to relation:target entry key
//         // Is this necessary?
//         final Map<String, Collection<String>> collatedSourceUris
//               = collateUris( relationNameTargetsToSourcesEntry.getValue() );
////         LOGGER.info( "Relation CollatedSourceUris "
////                      + relationNameTargetsMap.entrySet().stream()
////                                              .map( e -> e.getKey() + " " + String.join( ",", e.getValue() ) )
////                                              .collect( Collectors.joining( ";" ) )
////                      + " to "
////                      + collatedSourceUris.entrySet().stream()
////                                          .map( e -> e.getKey() + " " + String.join( ",", e.getValue() ) )
////                                          .collect( Collectors.joining( ";" ) ) );
//         for ( Collection<String> sourceUris : collatedSourceUris.values() ) {
//            createCiRelations( mentionConceptAggregateMap, sourceUris, uriMentionMap, relationNameTargetsMap, relationsDone );
//         }
//      }
//   }
//
//
//   static public Map<String, Collection<Mention>> mapCiUriAnnotations(
//         final Map<Mention, ConceptAggregate> mentionConceptAggregateMap,
//         final Collection<Mention> mentions ) {
//      final Map<String, Collection<Mention>> uriMentionMap = new HashMap<>();
//      for ( Mention annotation : mentions ) {
//         final ConceptAggregate ci = mentionConceptAggregateMap.get( annotation );
//         // If there isn't a ci then the annotation is an event, timex, etc.
//         if ( ci != null ) {
//            uriMentionMap.computeIfAbsent( ci.getUri(), a -> new HashSet<>() ).add( annotation );
////            LOGGER.info( "Concept Instance " + ci.getUri() + " " + ci.getCoveredText() + " for " + Neo4jOntologyConceptUtil.getUri( annotation ) + " " + annotation.getCoveredText() );
////         } else {
////            LOGGER.warn( "No Concept Instance for " + Neo4jOntologyConceptUtil.getUri( annotation ) + " " + annotation.getCoveredText() );
//         }
//      }
//      return uriMentionMap;
//   }
//
//
//   /**
//    * @param jCas             -
//    * @param uriMentionsMap annotations in paragraph
//    */
//   static private void createWindowRelations( final JCas jCas,
//                                              final Map<String, Collection<Mention>> uriMentionsMap,
//                                              final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
//                                              final Map<Mention, Collection<String>> relationsDone ) {
//      if ( uriMentionsMap.isEmpty() ) {
//         return;
//      }
//      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSourcesEntry
//            : relationNameTargetsToSources.entrySet() ) {
//         final Map<String, Collection<String>> relationNameTargetsMap = relationNameTargetsToSourcesEntry.getKey();
//         // collatedSources ...  This should be ok as each set of sources is unique to relation:target entry key
//         // Is this necessary?
//         final Map<String, Collection<String>> collatedSourceUris
//               = collateUris( relationNameTargetsToSourcesEntry.getValue() );
////         LOGGER.info( "Relation CollatedSourceUris "
////                      + relationNameTargetsMap.entrySet().stream()
////                                              .map( e -> e.getKey() + " " + String.join( ",", e.getValue() ) )
////                                              .collect( Collectors.joining( ";" ) )
////                      + " to "
////                      + collatedSourceUris.entrySet().stream()
////                                          .map( e -> e.getKey() + " " + String.join( ",", e.getValue() ) )
////                                          .collect( Collectors.joining( ";" ) ) );
//         for ( Collection<String> sourceUris : collatedSourceUris.values() ) {
//            createWindowRelations( jCas, sourceUris, uriMentionsMap, relationNameTargetsMap, relationsDone );
//         }
//      }
//   }
//
//   static private void createWindowRelations( final JCas jCas,
//                                              final Collection<String> alikeSourceUris,
//                                              final Map<String, Collection<Mention>> uriMentionsMap,
//                                              final Map<String, Collection<String>> relationNameTargetsMap,
//                                              final Map<Mention, Collection<String>> relationsDone ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      for ( Map.Entry<String, Collection<String>> relationNameTargets : relationNameTargetsMap.entrySet() ) {
//         final String relationName = relationNameTargets.getKey();
//         if ( RelationConstants.isHasSiteRelation( relationName )
//              || relationName.equals( HAS_LATERALITY ) ) {
//            continue;
//         }
//         final Collection<String> relationTargetUris = relationNameTargets.getValue();
//         final Collection<String> targetUris
//               = Neo4jRelationUtil.getRelatableUris( graphDb, uriMentionsMap.keySet(), relationTargetUris );
//         if ( targetUris.isEmpty() ) {
//            // No targets are available for this relation.
//            continue;
//         }
//         final List<Mention> sourceList
//               = getSourceMentions( relationName, alikeSourceUris, uriMentionsMap, relationsDone );
//         if ( sourceList.isEmpty() ) {
//            // No sources are available for this relation.
//            continue;
//         }
////         LOGGER.info( "Relation " + relationName );
//         final List<Mention> targetList = getTargetMentions( targetUris, uriMentionsMap );
//         if ( targetList.isEmpty() ) {
//            continue;
//         }
//         createRelations( jCas, relationName, sourceList, targetList );
//         sourceList.forEach( a -> relationsDone.computeIfAbsent( a, r -> new HashSet<>() ).add( relationName ) );
//      }
//   }
//
//
//   static private void createCiRelations( final Map<Mention, ConceptAggregate> mentionConceptAggregateMap,
//                                          final Collection<String> alikeSourceUris,
//                                          final Map<String, Collection<Mention>> uriMentionMap,
//                                          final Map<String, Collection<String>> relationNameTargetsMap,
//                                          final Map<Mention, Collection<String>> relationsDone ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      for ( Map.Entry<String, Collection<String>> relationNameTargets : relationNameTargetsMap.entrySet() ) {
//         final String relationName = relationNameTargets.getKey();
//         if ( RelationConstants.isHasSiteRelation( relationName )
//              || relationName.equals( HAS_LATERALITY ) ) {
//            continue;
//         }
//         final Collection<String> relationTargetUris = relationNameTargets.getValue();
//         final Collection<String> targetUris
//               = Neo4jRelationUtil.getRelatableUris( graphDb, uriMentionMap.keySet(), relationTargetUris );
//         if ( targetUris.isEmpty() ) {
//            // No targets are available for this relation.
//            continue;
//         }
//         final List<Mention> sourceList
//               = getSourceMentions( relationName, alikeSourceUris, uriMentionMap, relationsDone );
//         if ( sourceList.isEmpty() ) {
//            // No sources are available for this relation.
//            continue;
//         }
////         LOGGER.info( "Relation " + relationName );
//         final List<Mention> targetList = getTargetMentions( targetUris, uriMentionMap );
//         if ( targetList.isEmpty() ) {
//            continue;
//         }
//         createRelations( mentionConceptAggregateMap, relationName, sourceList, targetList );
//         sourceList.forEach( a -> relationsDone.computeIfAbsent( a, r -> new HashSet<>() ).add( relationName ) );
//      }
//   }
//
//
//   static private List<Mention> getSourceMentions( final String relationName,
//                                                   final Collection<String> alikeSourceUris,
//                                                   final Map<String, Collection<Mention>> uriMentionMap,
//                                                   final Map<Mention, Collection<String>> relationsDone ) {
//      final Predicate<Mention> relationNotDone = a -> relationsDone.get( a ) == null
//                                                                   || !relationsDone.get( a ).contains( relationName );
//      return alikeSourceUris.stream()
//                            .map( uriMentionMap::get )
//                            .filter( Objects::nonNull )
//                            .flatMap( Collection::stream )
//                            .filter( relationNotDone )
//                            .sorted( Comparator.comparingInt( Mention::getBegin ).thenComparing( Mention::getEnd ) )
//                            .collect( Collectors.toList() );
//   }
//
//   static private List<Mention> getTargetMentions( final Collection<String> targetUris,
//                                                   final Map<String, Collection<Mention>> uriMentionMap ) {
//      return targetUris.stream()
//                       .map( uriMentionMap::get )
//                       .filter( Objects::nonNull )
//                       .flatMap( Collection::stream )
//                       .sorted( Comparator.comparingInt( Mention::getBegin ).thenComparing( Mention::getEnd ) )
//                       .collect( Collectors.toList() );
//   }
//
//
//   static private Map<String, Map<String, Collection<String>>> mapUriRelations(
//         final Collection<String> sourceUris ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      final Map<String, Map<String, Collection<String>>> uriRelationMap = new HashMap<>();
//      for ( String uri : sourceUris ) {
//         // Map of relation name to target URIs  This is relation name to MOST SPECIFIC target uri.
//         // For instance, cap to prostate ONLY.  brca to breast ONLY.
//         final Map<String, Collection<String>> uriRelations = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
//         final Collection<String> siteRelations
//               = uriRelations.keySet().stream()
////                             .filter( RelationUtil::isHasSiteRelation )
//                             .filter( RelationConstants::isHasSiteRelation )
//                             .collect( Collectors.toSet() );
//         siteRelations.add( HAS_LATERALITY );
//         uriRelations.keySet().removeAll( siteRelations );
//         if ( uriRelations.isEmpty() ) {
//            continue;
//         }
//         uriRelationMap.put( uri, uriRelations );
//      }
//      return uriRelationMap;
//   }
//
//
//   static private Map<Map<String, Collection<String>>, Collection<String>> mapCiRelationToSources(
//         final Collection<ConceptAggregate> conceptAggregates ) {
//      final Collection<String> instanceUris = getCiUris( conceptAggregates );
//      return mapRelationToSources( instanceUris );
//   }
//
//   static private Map<Map<String, Collection<String>>, Collection<String>> mapMentionRelationToSources(
//         final Collection<Mention> mentions ) {
//      final Collection<String> mentionUris = getMentionUris( mentions );
//      return mapRelationToSources( mentionUris );
//   }
//
//   static private Map<Map<String, Collection<String>>, Collection<String>> mapRelationToSources(
//         final Collection<String> uris ) {
//      final Map<String, Map<String, Collection<String>>> uriRelationMap = mapUriRelations( uris );
//      // Map of Map of  Map of Relation name to target URIs to collection of source URIs
//      final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap = new HashMap<>();
//      for ( Map.Entry<String, Map<String, Collection<String>>> uriRelation : uriRelationMap.entrySet() ) {
//         relationToSourceMap.computeIfAbsent( uriRelation.getValue(), u -> new ArrayList<>() )
//                            .add( uriRelation.getKey() );
//      }
//      return relationToSourceMap;
//   }
//
//
//   static private boolean isUnwantedSection( final Segment section ) {
//      // Are we sure that we don't want to include Findings ???
//      final SectionType sectionType = SectionType.getSectionType( section );
//      return sectionType == SectionType.Microscopic
//             || sectionType == SectionType.Finding
//             || sectionType == SectionType.HistologySummary;
//   }
//
//
//   static private boolean isWantedSection( final Segment section ) {
//      // Are we sure that we don't want to include Findings ???
//      final SectionType sectionType = SectionType.getSectionType( section );
//      return sectionType != SectionType.Microscopic
//             && sectionType != SectionType.Finding
//             && sectionType != SectionType.HistologySummary;
//   }
//
//
//   /**
//    * TODO move to a utility class
//    * Given any collection of uris, collates and returns those uris in related branches.
//    * cancer, brca, dcis, cap   =  cancer, [cancer, brca, dcis, cap]
//    * brca, dcis, cap  =  brca, [brca, dcis] ; cap, [cap]
//    *
//    * @param allUris some collection of uris
//    * @return a map of branch root uris and the collection of child uris under that branch root
//    */
//   static public Map<String, Collection<String>> collateUris( final Collection<String> allUris ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Map<String, Collection<String>> uriBranches
//            = allUris.stream()
//                     .distinct()
//                     .collect( Collectors.toMap( Function.identity(), u -> SearchUtil.getBranchUris( graphDb, u ) ) );
//
//      final Map<String, String> uriBestRootMap = new HashMap<>( uriBranches.size() );
//      for ( Map.Entry<String, Collection<String>> uriBranch : uriBranches.entrySet() ) {
//         final String uri = uriBranch.getKey();
//         uriBestRootMap.put( uri, uri );
//         int longestBranch = uriBranch.getValue().size();
//         for ( Map.Entry<String, Collection<String>> testUriBranch : uriBranches.entrySet() ) {
//            if ( testUriBranch.getKey().equals( uri ) ) {
//               continue;
//            }
//            final Collection<String> testBranch = testUriBranch.getValue();
//            if ( testBranch.size() > longestBranch && testBranch.contains( uri ) ) {
//               uriBestRootMap.put( uri, testUriBranch.getKey() );
//               longestBranch = testBranch.size();
//            }
//         }
//      }
//      final Map<String, Collection<String>> branchMembers = new HashMap<>();
//      for ( Map.Entry<String, String> uriBestRoot : uriBestRootMap.entrySet() ) {
//         branchMembers.computeIfAbsent( uriBestRoot.getValue(), u -> new ArrayList<>() ).add( uriBestRoot.getKey() );
//      }
//      return branchMembers;
//   }
//
//
//   static private void createRelations( final JCas jCas,
//                                        final String relationName,
//                                        final List<Mention> sources,
//                                        final List<Mention> targets ) {
//      Map<Mention, Collection<Mention>> sourceTargetMap;
//      if ( relationName.equals( HAS_TREATMENT ) ) {
//         // TODO : Should this be RelationUtil.createSourceTargetMap( sources, targets, false? )
//         sourceTargetMap = new HashMap<>( RelationUtil.createReverseAttributeMapSingle( sources, targets, true ) );
//      } else {
//         sourceTargetMap = new HashMap<>( RelationUtil.createSourceTargetMap( sources, targets, true ) );
//      }
//      for ( Map.Entry<Mention, Collection<Mention>> entry : sourceTargetMap.entrySet() ) {
//         final Mention owner = entry.getKey();
//         entry.getValue().forEach( n -> RelationUtil.createRelation( jCas, owner, n, relationName ) );
//      }
//   }
//
//
//   static private void createRelations( final Map<Mention, ConceptAggregate> annotationConceptInstances,
//                                        final String relationName,
//                                        final List<Mention> sources,
//                                        final List<Mention> targets ) {
//      for ( Mention source : sources ) {
//         final ConceptAggregate sourceCi = annotationConceptInstances.get( source );
//         if ( sourceCi == null ) {
//            continue;
//         }
//         targets.stream()
//                .map( annotationConceptInstances::get )
//                .filter( Objects::nonNull )
//                .distinct()
//                .forEach( t -> sourceCi.addRelated( relationName, t ) );
//      }
//   }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//}
