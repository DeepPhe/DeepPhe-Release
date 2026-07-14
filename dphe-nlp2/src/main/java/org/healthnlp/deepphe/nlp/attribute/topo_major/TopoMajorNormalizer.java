package org.healthnlp.deepphe.nlp.attribute.topo_major;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2023}
 */
public class TopoMajorNormalizer extends DefaultXnAttributeNormalizer {


   static private final Map<String,String> URI_MAJOR_MINOR_MAP = new HashMap<>();
   static private final Map<String,Collection<String>> TOPO_MAJOR_MAP_FULL = new HashMap<>();
   static private final String UNDETERMINED = "C80";
   static private final String ILL_DEFINED = "C76";
   static private final String SKIN = "C44";
   static private final String BODY_TISSUE = "C49";

   public void init( final AttributeInfoCollector infoCollector, final Map<String,List<XnAttributeValue>> dependencies,
                     final long mentionCount ) {
      if ( URI_MAJOR_MINOR_MAP.isEmpty() ) {
         fillTopoMajorMaps();
      }
      super.init( infoCollector, dependencies, mentionCount );
//      NeoplasmSummaryCreator.addDebug( "TopoMajor best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getNormalNoValue() {
      return UNDETERMINED;
   }

//   protected Map<Double,List<UriConcept>> createConfidenceConceptsMap( final Collection<UriConceptRelation> relations ) {
//      if ( relations.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      // 2 different sites can have a mass or lesion with the same uri.  (For example)
//      // We still only want the one with the highest confidence.
//      final Map<Double,List<UriConcept>> confidenceConcepts = new HashMap<>();
//      for ( UriConceptRelation relation : relations ) {
//         final UriConcept target = relation.getTarget();
//         if ( target == null ) {
//            continue;
//         }
//         final Collection<String> codes = getTextCodes( target );
//         if ( !haveSpecificCodes( codes ) ) {
//            continue;
//         }
//         final double relationConfidence = relation.getConfidence();
////         final double targetConfidence = target.getConfidence();
//         final double targetConfidence = target.getGroupedConfidence();
////         final double confidence = ConfidenceCalculator.calculateHarmonicMean( relationConfidence, targetConfidence );
//         final double confidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//         LogFileWriter.add( "TopoMajorNormalizer relationConfidence " + relationConfidence + " targetConfidence " + targetConfidence + " end confidence = " + confidence );
//         confidenceConcepts.computeIfAbsent( confidence, c -> new ArrayList<>() ).add( target );
//      }
//      return confidenceConcepts;
//   }

//   protected Map<Double,List<UriConceptRelation>> createConfidenceRelationsMap( final Collection<UriConceptRelation> relations ) {
//      if ( relations.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      // 2 different sites can have a mass or lesion with the same uri.  (For example)
//      // We still only want the one with the highest confidence.
//      final Map<Double,List<UriConceptRelation>> confidenceRelations = new HashMap<>();
//      for ( UriConceptRelation relation : relations ) {
//         final UriConcept target = relation.getTarget();
//         if ( target == null ) {
//            continue;
//         }
//         final Collection<String> codes = getTextCodes( target );
//         if ( !haveSpecificCodes( codes ) ) {
//            continue;
//         }
//         final double relationConfidence = relation.getConfidence();
////         final double targetConfidence = target.getGroupedConfidence();
////         final double confidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//
//         final double targetConfidence = ConfidenceCalculator.getConceptConfidence( target );
//         final double confidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//
//         LogFileWriter.add( "TopoMajorNormalizer.createConfidenceRelationsMap "
//               + relation.getType() + " " + target.getUri() + " relationConfidence " + relationConfidence
//               + " , target Grouped confidence " + targetConfidence + " total confidence " + confidence );
//         confidenceRelations.computeIfAbsent( confidence, c -> new ArrayList<>() ).add( relation );
//      }
//      return confidenceRelations;
//   }

   protected List<XnAttributeValue> createAttributeValues( final Collection<UriConceptRelation> confidenceRelations,
                                                           final long mentionCount ) {
      final Map<String,List<UriConceptRelation>> normalRelationsMap = new HashMap<>();
      for ( UriConceptRelation relation : confidenceRelations ) {
         final UriConcept target = relation.getTarget();
         if ( target == null ) {
            continue;
         }
         final Collection<String> codes = getTextCodes( target );
         if ( !haveSpecificCodes( codes ) ) {
            continue;
         }
         final String normal = getNormalValue( target );
         normalRelationsMap.computeIfAbsent( normal, n -> new ArrayList<>() ).add( relation );
      }
      final List<XnAttributeValue> attributeValues = new ArrayList<>();
      for ( Map.Entry<String,List<UriConceptRelation>> normalRelations : normalRelationsMap.entrySet() ) {
         LogFileWriter.add( "TopoMajorNormalizer "
               + normalRelations.getValue().stream().map( UriConceptRelation::getTarget ).map( UriConcept::getUri ).collect(
               Collectors.joining(" ")) );
         final double confidence = ConfidenceCalculator.getAttributeConfidence( normalRelations.getValue(), mentionCount );
         final List<UriConcept> ranked
               = ConfidenceCalculator.rankConfidentConcepts( normalRelations.getValue()
                                                                            .stream()
                                                                            .map( UriConceptRelation::getTarget )
                                                                            .collect( Collectors.toSet() ), mentionCount );
         if ( ranked.isEmpty() ) {
            continue;
         }
         attributeValues.add( new XnAttributeValue( ranked.get( 0 ), normalRelations.getKey(), confidence, ranked ) );
         LogFileWriter.add( "TopoMajorNormalizer.createAttributeValues "
               + normalRelations.getKey() + " " + confidence + "\n   " + ranked.get( 0 ).toLongText() + "\n      "
               + ranked.stream().map( UriConcept::toLongText ).collect( Collectors.joining("\n     ") ) );
      }
      return attributeValues;
   }


//   public Map<String,Double> createTextCodeConfidenceMap( final Collection<UriConceptRelation> relations ) {
////      final Map<String,Double>  confidenceMap = new HashMap<>();
////      for ( ConceptAggregateRelation relation : relations ) {
////         final Collection<String> codes = getTextCodes( relation.getTarget().getUri() );
////         for ( String code : codes ) {
////            final double confidence = confidenceMap.getOrDefault( code, 0d );
////            confidenceMap.put( code, confidence + relation.getConfidence() );
////         }
////         NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer.createTextCodeConfidenceMap 1 "
////                                          + relation.getTarget().getUri() + " "
////                                          + relation.getConfidence() + " " + String.join( ",", codes )
////                                          + "\n" );
////      }
//      final Map<String,Double>  confidenceMap = super.createTextCodeConfidenceMap( relations );
//      if ( !confidenceMap.isEmpty() && haveSpecificCodes( confidenceMap ) ) {
//         return getSpecificCodes( confidenceMap );
//      }
////      confidenceMap.clear();
////      for ( ConceptAggregateRelation relation : relations ) {
////         final Collection<String> codes = relation.getTarget()
////                                                  .getAllUris()
////                                                  .stream()
////                                                  .map( this::getTextCodes )
////                                                  .flatMap( Collection::stream )
////                                                  .collect( Collectors.toSet() );
////         for ( String code : codes ) {
////            final double confidence = confidenceMap.getOrDefault( code, 0d );
////            confidenceMap.put( code, confidence + relation.getConfidence() );
////         }
////         NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer.createTextCodeConfidenceMap 2 "
////                                          + relation.getTarget().getUri() + " "
////                                          + relation.getConfidence() + " " + String.join( ",", codes )
////                                          + "\n" );
////      }
////      if ( !confidenceMap.isEmpty() && haveSpecificCodes( confidenceMap ) ) {
////         return getSpecificCodes( confidenceMap );
////      }
//      confidenceMap.clear();
//      confidenceMap.put( UNDETERMINED, 0.1 );
//      return confidenceMap;
//   }



//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         return UNDETERMINED;
//      }
//      setAllCodesCount( aggregates.size() );
//      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
//      final Map<String,Long> countMap = confidenceGroup.getBest()
//                                                           .stream()
//                                                            .map( CrConceptAggregate::getUri )
//                                                           .map( this::getCodes )
//                                                           .flatMap( Collection::stream )
//                                                           .map( c -> c.substring( 0,3 ) )
//                                                           .collect( Collectors.groupingBy( Function.identity(),
//                                                                                            Collectors.counting() ) );
//      if ( !countMap.isEmpty() ) {
//         final String code = getBestCode( countMap );
//         if ( !code.isEmpty() && isSpecificCode( code ) ) {
//            return code;
//         }
//      }
//      final Map<String,Long> countsMap = confidenceGroup.getBest()
//                                                       .stream()
//                                                       .map( CrConceptAggregate::getAllUris )
//                                                      .flatMap( Collection::stream )
//                                                       .map( this::getCodes )
//                                                       .flatMap( Collection::stream )
//                                                       .map( c -> c.substring( 0,3 ) )
//                                                       .collect( Collectors.groupingBy( Function.identity(),
//                                                                                        Collectors.counting() ) );
//      if ( !countsMap.isEmpty() ) {
//         final String code = getBestCode( countsMap );
//         if ( !code.isEmpty() && isSpecificCode( code ) ) {
//            return code;
//         }
//      }
//      final Map<String,Long> countMap2 = confidenceGroup.getNext()
//                                                       .stream()
//                                                       .map( CrConceptAggregate::getUri )
//                                                       .map( this::getCodes )
//                                                       .flatMap( Collection::stream )
//                                                       .map( c -> c.substring( 0,3 ) )
//                                                       .collect( Collectors.groupingBy( Function.identity(),
//                                                                                        Collectors.counting() ) );
//      if ( !countMap2.isEmpty() ) {
//         final String code = getBestCode( countMap2 );
//         if ( !code.isEmpty() ) {
//            return code;
//         }
//      }
//      NeoplasmSummaryCreator.addDebug( "No codes for " + confidenceGroup.getTopmost()
//                                                                        .stream()
//                                                                        .map( CrConceptAggregate::getUri )
//                                                                        .collect( Collectors.joining(",") ) + "\n" );
//      return UNDETERMINED;
//   }
//
//   private String getBestCode( final Map<String,Long> countMap ) {
////      final List<String> codeList = new ArrayList<>( countMap.keySet() );
//      final List<String> codeList = new ArrayList<>( getBestCodes( countMap ) );
//      codeList.sort( Comparator.reverseOrder() );
//      final String bestCode = codeList.get( 0 );
//      if ( bestCode.isEmpty() ) {
//         return "";
//      }
//      final long bestCount = countMap.get( bestCode );
//      setBestCodesCount( (int)bestCount );
////      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( countMap.size() );
//      NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer "
//                                       + countMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode + "\n");
//      return bestCode;
//   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      return String.join( ";", getTextCodes( uri ) );
   }

   public Collection<String> getTextCodes( final UriConcept concept ) {
//      concept.getAllUris().stream().map( this::getTextCodes ).flatMap( Collection::stream ).collect( Collectors.toList() );
      return getTextCodes( concept.getUri() );
   }

   public Collection<String> getTextCodes( final String uri ) {
      final Collection<String> codes = new HashSet<>();
      final Collection<String> allTableCodes = TOPO_MAJOR_MAP_FULL.get( uri );
      if ( allTableCodes != null ) {
         allTableCodes.forEach( c -> codes.add( c.substring( 0,3 ) ) );
      }
      final String tableCode = URI_MAJOR_MINOR_MAP.get( uri );
      if ( tableCode != null ) {
         codes.add( tableCode.substring( 0, 3 ) );
      }
      final String ontoCode = Neo4jOntologyConceptUtil.getIcdoTopoCode( uri );
      if ( !ontoCode.isEmpty() && !ontoCode.contains( "-" ) ) {
         codes.add( ontoCode.substring( 0, 3 ) );
      }
      return codes.stream().sorted().collect( Collectors.toList() );
   }


   static private void fillTopoMajorMaps() {
      try {
         final File topoMajorFile = FileLocator.getFile( "org/healthnlp/deepphe/icdo/DpheMajorSites.bsv" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( topoMajorFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               if ( line.isEmpty() || line.startsWith( "//" ) ) {
                  line = reader.readLine();
                  continue;
               }
               final String[] splits = StringUtil.fastSplit( line, '|' );
               // URI : Code
               URI_MAJOR_MINOR_MAP.put( splits[ 1 ], splits[ 0 ] );
               final String code = splits[ 0 ];
               final String uri = splits[ 1 ];
               TOPO_MAJOR_MAP_FULL.computeIfAbsent( uri, c -> new HashSet<>() ).add( code );
               UriInfoCache.getInstance().getUriBranch( uri )
//               Neo4jOntologyConceptUtil.getBranchUris( uri )
                                       .forEach( u -> TOPO_MAJOR_MAP_FULL
                                             .computeIfAbsent( uri, c -> new HashSet<>() ).add( code ) );
               line = reader.readLine();
            }
         }
      } catch ( IOException ioE ) {
         Logger.getLogger( "TopoMajorNormalizer" ).error( ioE.getMessage() );
      }
   }

   static private final Collection<String> UNSPECIFIC_CODES = new HashSet<>( Arrays.asList(
         UNDETERMINED, ILL_DEFINED, SKIN, BODY_TISSUE ) );

   static private boolean isSpecificCode( final String code ) {
      return !UNSPECIFIC_CODES.contains( code );
   }

   static private boolean haveSpecificCodes( final Map<String,Double> confidenceMap ) {
       return confidenceMap.keySet().stream().anyMatch( TopoMajorNormalizer::isSpecificCode );
   }

   static private boolean haveSpecificCodes( final Collection<String> codes ) {
      return codes.stream().anyMatch( TopoMajorNormalizer::isSpecificCode );
   }

   static private Map<String,Double> getSpecificCodes( final Map<String,Double> confidenceMap ) {
       confidenceMap.keySet().removeAll( UNSPECIFIC_CODES );
       return confidenceMap;
   }

}
