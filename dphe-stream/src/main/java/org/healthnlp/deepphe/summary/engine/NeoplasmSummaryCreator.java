package org.healthnlp.deepphe.summary.engine;


import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.biomarker.Biomarker;
import org.healthnlp.deepphe.summary.attribute.grade.GradeCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.grade.GradeUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralityCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.M_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.N_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.T_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.TnmCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.attribute.topography.minor.BreastMinorCodifier;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorUriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.UriConstants.CLOCKFACE;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class NeoplasmSummaryCreator {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmSummaryCreator" );

   static private final StringBuilder DEBUG_SB = new StringBuilder();

   static private final Map<String,String> TOPO_MAJOR_MAP = new HashMap<>();

   static private final boolean _debug = false;

   private NeoplasmSummaryCreator() {}


   static public void addDebug( final String text ) {
      if ( _debug ) {
         DEBUG_SB.append( text );
      }
   }

   static public String getDebug() {
      return DEBUG_SB.toString();
   }

   static public void resetDebug() {
      DEBUG_SB.setLength( 0 );
   }

   static private void fillTopoMajorMap() {
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
               TOPO_MAJOR_MAP.put( splits[ 0 ], splits[ 1 ] );
               line = reader.readLine();
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static public NeoplasmSummary createNeoplasmSummaryLong( final ConceptAggregate neoplasm,
                                                        final boolean isPrimary,
                                                        final Collection<ConceptAggregate> allConcepts,
                                                        final boolean registryOnly ) {
      if ( TOPO_MAJOR_MAP.isEmpty() ) {
         fillTopoMajorMap();
      }
      addDebug( "=======================================================================\n" +
              neoplasm.getPatientId() +  "\n" );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Predicate<ConceptAggregate> isNeoplasm = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( massNeoplasmUris::contains );
      final Collection<ConceptAggregate> patientNeoplasms
            = allConcepts.stream()
                         .filter( isNeoplasm )
                         .collect( Collectors.toList() );

      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() + "_" + System.currentTimeMillis() );
      summary.setClassUri( neoplasm.getUri() );
      final List<NeoplasmAttribute> attributes = new ArrayList<>();

//      final String topoCode = addTopography( neoplasm, summary, attributes, allConcepts );
      final NeoplasmAttribute topography = addTopography( neoplasm, attributes, allConcepts );
//      copyWithUriAsValue( attributes, "topography_major", "location" );
//      String locationUri = TOPO_MAJOR_MAP.getOrDefault( topoCode, "Undetermined" );
//      final String[] topoCodes = StringUtil.fastSplit( topoCode, ';' );
//      String locationUri = Arrays.stream( topoCodes )
//                                 .map( TOPO_MAJOR_MAP::get )
//                                 .filter( Objects::nonNull )
//                                 .collect( Collectors.joining( ";" ) );
//      if ( locationUri.isEmpty() || locationUri.equals( "Undetermined" ) ) {
//         locationUri = getTopographyUri( attributes );
//      }
      addLocation( topography, attributes );
//      createWithValue( "location", locationUri, locationUri, attributes );
      final NeoplasmAttribute lateralityCode = addLateralityCode( neoplasm, summary, attributes, allConcepts,
                                                            patientNeoplasms,
                                                       topography.getValue() );
//      copyWithUriAsValue( attributes, "laterality_code", "laterality" );
      copyWithUriAsValue( attributes, lateralityCode, "laterality" );
      // tumor has topo minor attributes.
      final NeoplasmAttribute topoMinor = addTopoMinor( neoplasm, summary, attributes, allConcepts, patientNeoplasms,
                                                  topography.getValue(),
                                lateralityCode.getValue() );
      addBrCaClockface( neoplasm, topoMinor, attributes );
      addQuadrant( neoplasm, topoMinor, attributes );
      // tumor has histology, diagnosis, behavior
      final NeoplasmAttribute histology = addHistology( neoplasm, summary, attributes, allConcepts, patientNeoplasms,
            topography.getValue() );
      //  Gold uses "cancer_type" and "histologic_type" to denote something -like- histology.
      copyWithUriAsValue( attributes, histology, "cancer_type" );
      copyWithUriAsValue( attributes, histology, "histologic_type" );
      copyWithUriAsValue( attributes, histology, "diagnosis" );
      addBehavior( neoplasm, summary, attributes, allConcepts, patientNeoplasms, registryOnly );
      //  "extent" is behavior, nice name.
      addExtent( attributes );
      addTumorType( attributes, isPrimary );
      // TODO topo_morphed ?
//      createWithValue( "topo_morphed", "generated", "false", attributes );
      createWithValueOnly( "historic", "historic",
                       neoplasm.inPatientHistory() ? "historic" : "current", attributes );
      createWithValueOnly( "calcifications", "calcifications",
                       neoplasm.getRelated( HAS_CALCIFICATION ).isEmpty()
                       ? "false" : "true", attributes );
      // Calcification was removed from the last n versions.
      addDebug( "Calcifications: " + (neoplasm.getRelated( HAS_CALCIFICATION ).isEmpty()
                       ? "false" : "true" ) + " ; " + allConcepts.stream()
                                                                  .map( ConceptAggregate::getMentions )
                                                                  .flatMap( Collection::stream )
                                                                 .map( Mention::getClassUri )
                                                                 .filter( u -> u.toLowerCase().contains( "calcif" ) )
                                                                 .collect( Collectors.joining(",") ) + "\n" );
      // Cancer has grade, stage, tnm
      addGrade( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addStage( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmT( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmN( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmM( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // tumor has biomarkers
      // TODO tumor_size, tumor_size_procedure
      addBiomarkers( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // TODO treatment (hasTreatment)

      summary.setAttributes( attributes );
      return summary;
   }

   static public NeoplasmSummary createNeoplasmSummary( final ConceptAggregate neoplasm,
                                                        final boolean isPrimary,
                                                        final Collection<ConceptAggregate> allConcepts,
                                                        final boolean registryOnly ) {
      if ( TOPO_MAJOR_MAP.isEmpty() ) {
         fillTopoMajorMap();
      }
      addDebug( "=======================================================================\n" +
                neoplasm.getPatientId() +  "\n" );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Predicate<ConceptAggregate> isNeoplasm = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( massNeoplasmUris::contains );
      final Collection<ConceptAggregate> patientNeoplasms
            = allConcepts.stream()
                         .filter( isNeoplasm )
                         .collect( Collectors.toList() );

      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() + "_" + System.currentTimeMillis() );
      summary.setClassUri( neoplasm.getUri() );
      final List<NeoplasmAttribute> attributes = new ArrayList<>();

      final NeoplasmAttribute topography = getTopography( neoplasm, allConcepts );
      addLocation( topography, attributes );
//      createWithValue( "location", locationUri, locationUri, attributes );
      final NeoplasmAttribute lateralityCode = getLateralityCode( neoplasm, allConcepts,
                                                                  patientNeoplasms,
                                                                  topography.getValue() );
//      copyWithUriAsValue( attributes, "laterality_code", "laterality" );
      copyWithUriAsValue( attributes, lateralityCode, "laterality" );
      // tumor has topo minor attributes.
      final NeoplasmAttribute topoMinor = getTopoMinor( neoplasm, allConcepts, patientNeoplasms,
                                                        topography.getValue(),
                                                        lateralityCode.getValue() );
      addBrCaClockface( neoplasm, topoMinor, attributes );
      addQuadrant( neoplasm, topoMinor, attributes );
      // tumor has histology, diagnosis, behavior
      final NeoplasmAttribute histology = addHistology( neoplasm, summary, attributes, allConcepts, patientNeoplasms,
            topography.getValue() );
      //  Gold uses "cancer_type" and "histologic_type" to denote something -like- histology.
//      copyWithUriAsValue( attributes, "histology", "cancer_type" );
      copyWithUriAsValue( attributes, histology, "histologic_type" );
//      copyWithUriAsValue( attributes, "histology", "diagnosis" );
      attributes.remove( histology );
      addBehavior( neoplasm, summary, attributes, allConcepts, patientNeoplasms, registryOnly );
      //  "extent" is behavior, nice name.
      addExtent( attributes );
      addTumorType( attributes, isPrimary );
      // TODO topo_morphed ?
//      createWithValue( "topo_morphed", "generated", "false", attributes );
//      createWithValueOnly( "historic", "historic",
//                           neoplasm.inPatientHistory() ? "historic" : "current", attributes );
//      createWithValueOnly( "calcifications", "calcifications",
//                           neoplasm.getRelated( HAS_CALCIFICATION ).isEmpty()
//                           ? "false" : "true", attributes );
      // Calcification was removed from the last n versions.
      addDebug( "Calcifications: " + (neoplasm.getRelated( HAS_CALCIFICATION ).isEmpty()
                                      ? "false" : "true" ) + " ; " + allConcepts.stream()
                                                                                .map( ConceptAggregate::getMentions )
                                                                                .flatMap( Collection::stream )
                                                                                .map( Mention::getClassUri )
                                                                                .filter( u -> u.toLowerCase().contains( "calcif" ) )
                                                                                .collect( Collectors.joining(",") ) + "\n" );
      // Cancer has grade, stage, tnm
      addGrade( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addStage( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmT( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmN( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmM( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // tumor has biomarkers
      // TODO tumor_size, tumor_size_procedure
      addBiomarkers( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // TODO treatment (hasTreatment)

      summary.setAttributes( attributes );
      return summary;
   }

//   static private String addTopography( final ConceptAggregate neoplasm,
//                                      final NeoplasmSummary summary,
//                                      final List<NeoplasmAttribute> attributes,
//                                        final Collection<ConceptAggregate> allConcepts ) {
//      final Topography topography = new Topography( neoplasm, allConcepts );
//      final NeoplasmAttribute majorTopoAttr = topography.toNeoplasmAttribute();
//      attributes.add( majorTopoAttr );
//
//      // TODO as NeoplasmAttribute from DefaultAttribute
//
////      return majorTopoAttr.getValue() + "3";
//      return majorTopoAttr.getValue();
//   }

   static private NeoplasmAttribute getTopography(final ConceptAggregate neoplasm,
                                                  final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      return topography.toNeoplasmAttribute();
   }

   static private NeoplasmAttribute addTopography(final ConceptAggregate neoplasm,
                                                  final List<NeoplasmAttribute> attributes,
                                                  final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      final NeoplasmAttribute majorTopoAttr = topography.toNeoplasmAttribute();
      attributes.add( majorTopoAttr );
      return majorTopoAttr;
   }

   static private String getTopographyUri( final List<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute sourceAttribute =
            attributes.stream().filter( a -> a.getName().equals( "behavior" ) ).findFirst().orElse( null );
      if ( sourceAttribute == null ) {
         return "Undetermined";
      }
      return sourceAttribute.getClassUri();
   }

   static private NeoplasmAttribute getTopoMinor( final ConceptAggregate neoplasm,
                                                  final Collection<ConceptAggregate> allConcepts,
                                                  final Collection<ConceptAggregate> patientNeoplasms,
                                                  final String topographyMajor,
                                                  final String lateralityCode ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      dependencies.put( "laterality_code", lateralityCode );
      final DefaultAttribute<TopoMinorUriInfoVisitor, TopoMinorCodeInfoStore> topoMinor
            = new DefaultAttribute<>( "topography_minor",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      TopoMinorUriInfoVisitor::new,
                                      TopoMinorCodeInfoStore::new,
                                      dependencies );
      return topoMinor.toNeoplasmAttribute();
   }

   static private NeoplasmAttribute addTopoMinor( final ConceptAggregate neoplasm,
                                    final NeoplasmSummary summary,
                                    final List<NeoplasmAttribute> attributes,
                                    final Collection<ConceptAggregate> allConcepts,
                                    final Collection<ConceptAggregate> patientNeoplasms,
                                     final String topographyMajor,
                                     final String lateralityCode ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      dependencies.put( "laterality_code", lateralityCode );
      final DefaultAttribute<TopoMinorUriInfoVisitor, TopoMinorCodeInfoStore> topoMinor
            = new DefaultAttribute<>( "topography_minor",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      TopoMinorUriInfoVisitor::new,
                                      TopoMinorCodeInfoStore::new,
                                      dependencies );
      final NeoplasmAttribute attribute = topoMinor.toNeoplasmAttribute();
      attributes.add( attribute );
      return attribute;
   }

   static Collection<ConceptAggregate> getRelatedAboveCutoff(
         final ConceptAggregate neoplasm,
         final String relationName,
         final double cutoff_constant ) {
      final Collection<ConceptAggregate> related = neoplasm.getRelated( relationName );
      if ( related.isEmpty() ) {
         return Collections.emptyList();
      }
      if ( related.size() == 1 ) {
         return related;
      }
      final Map<String,Integer> uriMentionCounts = new HashMap<>();
      int max = 0;
      for ( ConceptAggregate concept : related ) {
         final String uri = concept.getUri();
         int mentions = concept.getMentions().size();
         final int count = uriMentionCounts.computeIfAbsent( uri, u -> 0 );
         uriMentionCounts.put( uri, count + mentions );
         max = Math.max( max, count + mentions );
      }
      double cutoff = max * cutoff_constant;
      final Collection<String> highCountCodes = getUrisAboveCutoff( uriMentionCounts, cutoff );
      return related.stream().filter( r -> highCountCodes.contains( r.getUri() ) ).collect( Collectors.toList() );
   }

   static Collection<ConceptAggregate> getLocationsAboveCutoff(
         final ConceptAggregate neoplasm,
         final String relationName,
         final Collection<String> validUris,
         final double cutoff_constant ) {
      final Collection<ConceptAggregate> related = new HashSet<>();
      related.addAll( neoplasm.getRelated( relationName ) );
      neoplasm.getRelatedSites().stream()
              .filter( c -> validUris.contains( c.getUri() ) )
              .forEach( related::add );
      if ( related.isEmpty() ) {
         return Collections.emptyList();
      }
      if ( related.size() == 1 ) {
         return related;
      }
      final Map<String,Integer> uriMentionCounts = new HashMap<>();
      int max = 0;
      for ( ConceptAggregate concept : related ) {
         final String uri = concept.getUri();
         int mentions = concept.getMentions().size();
         final int count = uriMentionCounts.computeIfAbsent( uri, u -> 0 );
         uriMentionCounts.put( uri, count + mentions );
         max = Math.max( max, count + mentions );
      }
      double cutoff = max * cutoff_constant;
      final Collection<String> highCountCodes = getUrisAboveCutoff( uriMentionCounts, cutoff );
      return related.stream().filter( r -> highCountCodes.contains( r.getUri() ) ).collect( Collectors.toList() );
   }

   static private Collection<String> getUrisAboveCutoff( final Map<String,Integer> uriMentionCounts,
                                                          final double cutoff ) {
      final Collection<String> lowCountCodes = uriMentionCounts.entrySet().stream()
                                                                .filter( e -> e.getValue() < cutoff )
                                                                .map( Map.Entry::getKey )
                                                                .collect( Collectors.toSet() );
      final Collection<String> codes = new HashSet<>( uriMentionCounts.keySet() );
      codes.removeAll( lowCountCodes );
      addDebug( "NeoplasmSummaryCreator.getUrisAboveCutoff: " + cutoff + " "
                       + uriMentionCounts.entrySet().stream()
                                         .map( e -> e.getKey() + "," + e.getValue() )
                                         .collect( Collectors.joining(";") ) + "\n");
      return codes;
   }

   static private void addLocation( final NeoplasmAttribute topography, final List<NeoplasmAttribute> attributes ) {
      final String topoCode = topography.getValue();
      final String[] topoCodes = StringUtil.fastSplit( topoCode, ';' );
      String locationUri = Arrays.stream( topoCodes )
                                 .map( NeoplasmSummaryCreator::getLocation )
                                 .filter( Objects::nonNull )
                                 .collect( Collectors.joining( ";" ) );
      createWithValueAndSource( "location", locationUri, locationUri, topography, attributes );
   }

   static private String getLocation( final String topoCode ) {
      String location = TOPO_MAJOR_MAP.getOrDefault( topoCode.substring( 0, 2 ) + "*", "" );
      if ( !location.isEmpty() ) {
         return location;
      }
      location = TOPO_MAJOR_MAP.getOrDefault( topoCode.substring( 0, 2 ) + "0", "" );
      if ( !location.isEmpty() ) {
         return location;
      }
      return TOPO_MAJOR_MAP.getOrDefault( topoCode, "Undetermined" );
   }

   static private void addBrCaClockface( final ConceptAggregate neoplasm,
                                         final NeoplasmAttribute topoMinor,
                                         final List<NeoplasmAttribute> attributes ) {
      final Collection<ConceptAggregate> bestClockfaces = getRelatedAboveCutoff( neoplasm, HAS_CLOCKFACE, 0.4 );
//      final String clockface = neoplasm.getRelated( HAS_CLOCKFACE ).stream()
      final String clockface = bestClockfaces.stream()
              .map( ConceptAggregate::getUri )
              .distinct()
            .collect( Collectors.joining( ";" ) );
      if ( !clockface.isEmpty() ) {
         createWithValueAndSource( "clockface", CLOCKFACE, clockface, topoMinor, attributes );
      }
   }

   static private Collection<String> QUADRANT_URIS;
   static private void initQuadrantUris() {
      if ( QUADRANT_URIS != null ) {
         return;
      }
      QUADRANT_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT );
   }

   static private void addQuadrant( final ConceptAggregate neoplasm,
                                    final NeoplasmAttribute topoMinor, final List<NeoplasmAttribute> attributes ) {
      initQuadrantUris();
      final Collection<ConceptAggregate> bestQuadrants = getLocationsAboveCutoff( neoplasm, HAS_QUADRANT,
                                                                                  QUADRANT_URIS, 0.4 );
//      final Collection<String> quadrants = new HashSet<>();
//      neoplasm.getRelated( HAS_QUADRANT ).stream()
//              .map( ConceptAggregate::getUri )
//              .forEach( quadrants::add );
//      neoplasm.getRelatedSites().stream()
//              .map( ConceptAggregate::getUri )
//              .filter( QUADRANT_URIS::contains )
//              .forEach( quadrants::add );
      final String quadrants = bestQuadrants.stream()
                                             .map( ConceptAggregate::getUri )
                                             .distinct()
                                             .collect( Collectors.joining( ";" ) );
      if ( !quadrants.isEmpty() ) {
         createWithValueAndSource( "quadrant", UriConstants.QUADRANT, String.join( ";", quadrants ),
                                   topoMinor, attributes );
      } else {
         addQuadrantByTopoMinor( topoMinor, attributes );
      }
   }

   static private void addQuadrantByTopoMinor( final NeoplasmAttribute topoMinor,
                                               final Collection<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute clockface = attributes.stream()
                                                    .filter( a -> a.getName().equals( "clockface" ) )
                                                    .findAny()
                                                    .orElse( null );
      addDebug( "NeoplasmSummaryCreator.addQuadrantByTopoMinor have clockface " + (clockface != null ) + "\n" );
      if ( clockface == null || clockface.getValue().isEmpty() ) {
         return;
      }
      final String topography_minor = topoMinor.getValue();
//            attributes.stream()
//                                                .filter( a -> a.getName().equals( "topography_minor" ) )
//                                                .map( NeoplasmAttribute::getValue )
//                                                .findFirst()
//                                                .orElse( "" );
      addDebug( "NeoplasmSummaryCreator.addQuadrantByTopoMinor " + topography_minor
                       + " " + BreastMinorCodifier.getQuadrant( topography_minor ) + "\n" );
      if ( topography_minor.isEmpty() ) {
         return;
      }
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = clockface.getClassUri();
      attribute.setName( "quadrant" );
      attribute.setId( "quadrant_" + uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( BreastMinorCodifier.getQuadrant( topography_minor ) );
      attribute.setConfidence( clockface.getConfidence() );
      attribute.setConfidenceFeatures( clockface.getConfidenceFeatures() );
      attribute.setDirectEvidence( clockface.getDirectEvidence() );
      attribute.setIndirectEvidence( clockface.getIndirectEvidence() );
      attribute.setNotEvidence( clockface.getNotEvidence() );
      attributes.add( attribute );
   }


//   static private void addTumorType( final ConceptAggregate neoplasm, final List<NeoplasmAttribute> attributes ) {
//      final String tumorType = neoplasm.getRelated( HAS_TUMOR_TYPE ).stream()
//                                       .map( ConceptAggregate::getUri )
//                                       .distinct()
//                                       .collect( Collectors.joining( ";" ) );
//      if ( !tumorType.isEmpty() ) {
//         createWithValue( "tumor_type", "tumor_type", tumorType, attributes );
//      }
//   }


   static private NeoplasmAttribute addHistology( final ConceptAggregate neoplasm,
                                     final NeoplasmSummary summary,
                                     final List<NeoplasmAttribute> attributes,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<ConceptAggregate> patientNeoplasms,
                                     final String topographyCode ) {
      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
            = new Histology( neoplasm,
                             allConcepts,
                             patientNeoplasms );
      final NeoplasmAttribute attribute = histology.toNeoplasmAttribute();
      attributes.add( attribute );
      return attribute;
   }


   static private void addBehavior( final ConceptAggregate neoplasm,
                                      final NeoplasmSummary summary,
                                      final List<NeoplasmAttribute> attributes,
                                      final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                    final boolean registryOnly ) {
      final DefaultAttribute<BehaviorUriInfoVisitor, BehaviorCodeInfoStore> behavior
            = new DefaultAttribute<>( "behavior",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      BehaviorUriInfoVisitor::new,
                                      BehaviorCodeInfoStore::new,
                                      Collections.emptyMap() );
      final NeoplasmAttribute attribute = behavior.toNeoplasmAttribute();
      if ( registryOnly && attribute.getValue().equals( "6" ) ) {
         attribute.setValue( "3" );
      }
      attributes.add( attribute );
   }

   // Extent is "Invasive_Lesion" or In Situ? Benign?  Seems to always be invasive in gold.
   static private void addExtent( final List<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute sourceAttribute =
            attributes.stream().filter( a -> a.getName().equals( "behavior" ) ).findFirst().orElse( null );
      if ( sourceAttribute == null ) {
         return;
      }
      final String value = sourceAttribute.getValue();
      String extent = "Invasive_Lesion";
      switch ( value ) {
         case "0" : extent = "Benign"; break;
//         case "1" : extent = "Uncertain"; break;
         case "2" : extent = "In_Situ"; break;
         case "6" : extent = "Metastatic"; break;
      }
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = sourceAttribute.getClassUri();
      attribute.setName( "extent" );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( extent );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
   }

   //      tumor_type is "PrimaryTumor", "Distant_Metastasis", "Regional_Metastasis"
   // TODO - make this a bit better.
   static private void addTumorType( final List<NeoplasmAttribute> attributes, final boolean isPrimary ) {
      final NeoplasmAttribute sourceAttribute =
            attributes.stream().filter( a -> a.getName().equals( "behavior" ) ).findFirst().orElse( null );
      if ( sourceAttribute == null ) {
         return;
      }
      final String tumorType = isPrimary ? "PrimaryTumor" : "Distant_Metastasis";
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = sourceAttribute.getClassUri();
      attribute.setName( "tumor_type" );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( tumorType );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
   }

   static private NeoplasmAttribute getLateralityCode( final ConceptAggregate neoplasm,
                                                       final Collection<ConceptAggregate> allConcepts,
                                                       final Collection<ConceptAggregate> patientNeoplasms,
                                                       final String topographyMajor ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> lateralityCode
            = new DefaultAttribute<>( "laterality_code",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      return lateralityCode.toNeoplasmAttribute();
   }


   static private NeoplasmAttribute addLateralityCode( final ConceptAggregate neoplasm,
                                            final NeoplasmSummary summary,
                                            final List<NeoplasmAttribute> attributes,
                                            final Collection<ConceptAggregate> allConcepts,
                                            final Collection<ConceptAggregate> patientNeoplasms,
                                            final String topographyMajor ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> lateralityCode
            = new DefaultAttribute<>( "laterality_code",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      final NeoplasmAttribute attribute = lateralityCode.toNeoplasmAttribute();
      attributes.add( attribute );
      return attribute;
   }


   static private void addGrade( final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<GradeUriInfoVisitor, GradeCodeInfoStore> grade
            = new DefaultAttribute<>( "grade",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      GradeUriInfoVisitor::new,
                                      GradeCodeInfoStore::new,
                                      Collections.emptyMap() );
      attributes.add( grade.toNeoplasmAttribute() );
   }

   static private void addStage( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<StageUriInfoVisitor, StageCodeInfoStore> stage
            = new DefaultAttribute<>( "stage",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      StageUriInfoVisitor::new,
                                      StageCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( stage.getBestUri().isEmpty() || stage.getBestCode().isEmpty() ) {
         return;
      }
      final NeoplasmAttribute attribute = stage.toNeoplasmAttribute();
      if ( attribute.getValue().equals( "0" ) ) {
         attribute.setValue( "" );
      }
      attributes.add( attribute );
   }

   static private void addTnmT( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<T_UriInfoVisitor, TnmCodeInfoStore> t
            = new DefaultAttribute<>( "t",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      T_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( t.getBestUri().isEmpty() || t.getBestCode().isEmpty() ) {
         return;
      }
      final NeoplasmAttribute attribute = t.toNeoplasmAttribute();
      if ( attribute.getValue().equals( "PT1s" ) ) {
         attribute.setValue( "PTis" );
      }
      attributes.add( attribute );
   }

   static private void addTnmN( final ConceptAggregate neoplasm,
                                final NeoplasmSummary summary,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<N_UriInfoVisitor, TnmCodeInfoStore> n
            = new DefaultAttribute<>( "n",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      N_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( n.getBestUri().isEmpty() || n.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( n.toNeoplasmAttribute() );
   }

   static private void addTnmM( final ConceptAggregate neoplasm,
                                final NeoplasmSummary summary,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<M_UriInfoVisitor, TnmCodeInfoStore> m
            = new DefaultAttribute<>( "m",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      M_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( m.getBestUri().isEmpty() || m.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( m.toNeoplasmAttribute() );
   }


//   static private String getT( final ConceptAggregate summary ) {
//      final Collection<String> ts = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_T, HAS_PATHOLOGIC_T ) );
//      return String.join( ";", getTnmValue( ts, 't', true ) );
//   }
//
//   static private String getN( final ConceptAggregate summary ) {
//      final Collection<String> ns = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_N, HAS_PATHOLOGIC_N ) );
//      return String.join( ";", getTnmValue( ns, 'n', true ) );
//   }
//
//   static private String getM( final ConceptAggregate summary ) {
//      final Collection<String> ms = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_M, HAS_PATHOLOGIC_M ) );
//      return String.join( ";", getTnmValue( ms, 'm', false ) );
//   }
//
//   static private String getTnmValue( final Collection<String> tnms, final char type, final boolean allowX ) {
//      final Collection<String> values = new HashSet<>();
//      for ( String tnm : tnms ) {
//         final String lower = tnm.toLowerCase().replace( "_stage", "" );
//         final int typeIndex = lower.indexOf( type );
//         if ( typeIndex < 0 || typeIndex >= tnm.length() - 1 ) {
//            continue;
//         }
//         final String value = lower.substring( typeIndex + 1 );
//         values.add( value );
//      }
//      values.remove( "x_category" );
//      if ( !allowX ) {
//         values.remove( "x" );
//         values.remove( "X" );
//      }
//      return String.join( ";", values );
//   }


   // TODO ER_amount, ER_procedure, PR_amount, PR_procedure, HER2_amount, HER2_procedure (has_method)
   static private final Collection<String> BIOMARKERS = Arrays.asList(
         "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
         "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );

   static private void addBiomarkers( final ConceptAggregate neoplasm,
                                     final NeoplasmSummary summary,
                                     final List<NeoplasmAttribute> attributes,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<ConceptAggregate> patientNeoplasms ) {
       BIOMARKERS.forEach( b -> addBiomarker( b, neoplasm, summary, attributes, allConcepts, patientNeoplasms ) );
   }

   static private void addBiomarker( final String biomarkerName,
                                       final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                        final Collection<ConceptAggregate> patientNeoplasms ) {
      final Biomarker biomarker  = new Biomarker( biomarkerName,
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms );
      if ( biomarker.getBestUri().isEmpty() || biomarker.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( biomarker.toNeoplasmAttribute() );
   }


   static private String copyWithUriAsValue( final List<NeoplasmAttribute> attributes,
                                             final NeoplasmAttribute sourceAttribute,
                                             final String targetName ) {
      if ( sourceAttribute == null ) {
         return "";
      }
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = sourceAttribute.getClassUri();
      attribute.setName( targetName );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( uri );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
      return uri;
   }

   static private String copyWithValueAsValue( final List<NeoplasmAttribute> attributes,
                                             final String sourceName,
                                             final String targetName ) {
      final NeoplasmAttribute sourceAttribute =
            attributes.stream().filter( a -> a.getName().equals( sourceName ) ).findFirst().orElse( null );
      if ( sourceAttribute == null ) {
         return "";
      }
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = sourceAttribute.getClassUri();
      attribute.setName( targetName );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( sourceAttribute.getValue() );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
      return uri;
   }

   static private String createWithValueOnly( final String name, final String uri, final String value,
                                          final List<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( value );
      attribute.setConfidence( 10 );
      attribute.setConfidenceFeatures( Collections.emptyList() );
      attribute.setDirectEvidence( Collections.emptyList() );
      attribute.setIndirectEvidence( Collections.emptyList() );
      attribute.setNotEvidence( Collections.emptyList() );
      attributes.add( attribute );
      return uri;
   }

   static private String createWithValueAndSource( final String name, final String uri, final String value,
                                          final NeoplasmAttribute sourceAttribute,
                                          final List<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( value );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
      return uri;
   }


}
