package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.cancer.ae.DocEpisodeTagger;
import org.apache.ctakes.cancer.concept.instance.*;
import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.DpheEssentialAnnotationUtil;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

import static org.apache.ctakes.cancer.summary.NeoplasmCiContainer.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/5/2018
 */
final public class CiContainerFactory {

   static private final Logger LOGGER = Logger.getLogger( "CiContainerFactory" );

   static private final boolean DEBUG = true;


   private CiContainerFactory() {
   }



   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            Create Summaries Entry Point
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////




   static public Collection<CiContainer> createPatientSummaries( final JCas patientCas, final Collection<NoteSpecs> noteSpecs ) {
      String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
      final Map<IdentifiedAnnotation,String> patientAnnotations = new HashMap<>();
      final Collection<BinaryTextRelation> patientRelations = new ArrayList<>();
      final Collection<JCas> docJcases = PatientViewUtil.getAllViews( patientCas );
      for ( JCas docCas : docJcases ) {
         final Collection<BinaryTextRelation> relations = JCasUtil.select( docCas, BinaryTextRelation.class );
         patientRelations.addAll( relations );
         if ( patientId == null || patientId.isEmpty() || patientId.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
            patientId = SourceMetadataUtil.getPatientIdentifier( docCas );
         }
         final String docId = DocumentIDAnnotationUtil.getDocumentID( docCas );
         JCasUtil.select( docCas, IdentifiedAnnotation.class ).forEach( a -> patientAnnotations.put( a, docId ) );
      }
      return createCiSummaries( noteSpecs, patientId, patientAnnotations, patientRelations );
   }

   static public Collection<CiContainer> createDocSummaries( final JCas docCas, final NoteSpecs noteSpecs ) {
      String patientId = SourceMetadataUtil.getPatientIdentifier( docCas );
      final Map<IdentifiedAnnotation,String> annotations = new HashMap<>();
      final Collection<BinaryTextRelation> relations = JCasUtil.select( docCas, BinaryTextRelation.class );
      final String docId = DocumentIDAnnotationUtil.getDocumentID( docCas );
      JCasUtil.select( docCas, IdentifiedAnnotation.class ).forEach( a -> annotations.put( a, docId ) );
      return createCiSummaries( Collections.singletonList( noteSpecs ), patientId, annotations, relations );
   }

   /**
    *
    * @param patientId -
    * @param annotations map of all annotations to their containing document
    * @param relations -
    * @return all summaries
    */
   static private Collection<CiContainer> createCiSummaries( final Collection<NoteSpecs> noteSpecs,
                                                             final String patientId,
                                                             final Map<IdentifiedAnnotation,String>  annotations,
                                                             final Collection<BinaryTextRelation> relations ) {
      LOGGER.info( "createCiSummaries calling ConceptInstanceFactory.createUriConceptInstanceMap with " + annotations.size() + " annotations." );
      final Map<String,Collection<ConceptInstance>> conceptInstances
            = ConceptInstanceFactory.createUriConceptInstanceMap( patientId, annotations, relations );
      LOGGER.info( "createCiSummaries using " + conceptInstances.size() + " concept instances" );
      return createCiSummaries( noteSpecs, conceptInstances );
   }

   static private Collection<CiContainer> createCiSummaries( final Collection<NoteSpecs> noteSpecs,
                                                             final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
      final Collection<CiContainer> summaries = new ArrayList<>();
      return summaries;
   }












































   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Create Cancer -to- Tumor Diagnosis Summaries Entry Point
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


////   static public Map<CiSummary,Collection<CiSummary>> createPatientDiagnoses( final JCas patientCas ) {
////   static public Collection<CancerCiSummary> createPatientDiagnoses( final JCas patientCas ) {
//static public PatientCiSummary createPatientDiagnoses( final JCas patientCas ) {
//   String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
//      final Map<IdentifiedAnnotation,String> patientAnnotations = new HashMap<>();
//      final Collection<BinaryTextRelation> patientRelations = new ArrayList<>();
//      final Collection<JCas> docJcases = PatientViewUtil.getAllViews( patientCas );
//      final Collection<NoteSpecs> noteSpecs = new ArrayList<>( docJcases.size() );
//      for ( JCas docCas : docJcases ) {
//         noteSpecs.add( new NoteSpecs( docCas ) );
//         final Collection<BinaryTextRelation> relations = JCasUtil.select( docCas, BinaryTextRelation.class );
//         patientRelations.addAll( relations );
//         if ( patientId == null || patientId.isEmpty() || patientId.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
//            patientId = SourceMetadataUtil.getPatientIdentifier( docCas );
//         }
//         final String docId = DocumentIDAnnotationUtil.getDocumentID( docCas );
//         JCasUtil.select( docCas, IdentifiedAnnotation.class ).forEach( a -> patientAnnotations.put( a, docId ) );
//      }
//      return createDiagnosisSummaries( patientId, noteSpecs, patientAnnotations, patientRelations );
//   }
//
////   static public Map<CiSummary,Collection<CiSummary>> createDocDiagnoses( final JCas docCas, final NoteSpecs noteSpecs ) {
////   static public Collection<CancerCiSummary> createDocDiagnoses( final JCas docCas, final NoteSpecs noteSpecs ) {
//   static public PatientCiSummary createDocDiagnoses( final JCas docCas, final NoteSpecs noteSpecs ) {
//      String patientId = SourceMetadataUtil.getPatientIdentifier( docCas );
//      final Map<IdentifiedAnnotation,String> annotations = new HashMap<>();
//      final Collection<BinaryTextRelation> relations = JCasUtil.select( docCas, BinaryTextRelation.class );
//      final String docId = DocumentIDAnnotationUtil.getDocumentID( docCas );
//      JCasUtil.select( docCas, IdentifiedAnnotation.class ).forEach( a -> annotations.put( a, docId ) );
//      return createDiagnosisSummaries( patientId, Collections.singletonList( noteSpecs ), annotations, relations );
//   }

//   /**
//    * Entry point for new multi-cancer, drools-free summary creation.
//    * @param patientId -
//    * @param annotations -
//    * @param relations -
//    * @return map of cancer summary to tumor summaries
//    */
////   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,  final String patientId,
////   static private Collection<CancerCiSummary> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,  final String patientId,
////                                                                         final Map<IdentifiedAnnotation,String>  annotations,
////                                                                         final Collection<BinaryTextRelation> relations ) {
//   static private PatientCiSummary createDiagnosisSummaries( final String patientId, final Collection<NoteSpecs> noteSpecs,
//                                                                        final Map<IdentifiedAnnotation,String>  annotations,
//                                                                        final Collection<BinaryTextRelation> relations ) {
//      LOGGER.info( "createCiDiagnoses calling ConceptInstanceFactory.createUriConceptInstanceMap with " + annotations.size() + " annotations." );
//      final Map<String,Collection<ConceptInstance>> uriConceptInstances
//            = ConceptInstanceFactory.createUriConceptInstanceMap( patientId, annotations, relations );
//      LOGGER.info( "createDiagnosisSummaries using " + uriConceptInstances.size() + " uri concept instances" );
//      return createDiagnosisSummaries( patientId, noteSpecs, uriConceptInstances );
//   }


//   static public Map<CiSummary,Collection<CiSummary>> createPatientDiagnoses2( final JCas patientCas ) {
//   static public Collection<CancerCiSummary> createPatientDiagnoses2( final JCas patientCas ) {
   static public PatientCiContainer createPatientSummary( final JCas patientCas ) {
      String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
      final Map<IdentifiedAnnotation,String> patientAnnotations = new HashMap<>();
      final Collection<BinaryTextRelation> patientRelations = new ArrayList<>();
      final Collection<CollectionTextRelation> patientCorefs = new ArrayList<>();
      final Map<Markable, Collection<IdentifiedAnnotation>> patientMarkables = new HashMap<>();
      final Collection<JCas> docJcases = PatientViewUtil.getDocumentViews( patientCas );
      final Map<NoteSpecs,String> noteSpecs = new HashMap<>( docJcases.size() );
      for ( JCas docCas : docJcases ) {
         final String episodeType = JCasUtil.select( docCas, Episode.class ).stream()
                                      .map( Episode::getEpisodeType )
                                      .distinct()
                                      .sorted()
                                      .findFirst()
                                      .orElse( DocEpisodeTagger.NO_CATEGORY );
         noteSpecs.put( new NoteSpecs( docCas ), episodeType );
         final Collection<BinaryTextRelation> relations = JCasUtil.select( docCas, BinaryTextRelation.class );
         patientRelations.addAll( relations );
         if ( patientId == null || patientId.isEmpty() || patientId.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
            patientId = SourceMetadataUtil.getPatientIdentifier( docCas );
         }
         final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( docCas, IdentifiedAnnotation.class );
         final Collection<CollectionTextRelation> corefs = JCasUtil.select( docCas, CollectionTextRelation.class );

         final String docId = DocumentIDAnnotationUtil.getDocumentID( docCas );
         annotations.forEach( a -> patientAnnotations.put( a, docId ) );
         patientCorefs.addAll( corefs );
         patientMarkables.putAll( DpheEssentialAnnotationUtil.mapMarkableAnnotations( docCas, corefs ) );
      }


      return createPatientSummary( patientId, noteSpecs, patientAnnotations, patientRelations, patientCorefs, patientMarkables );
   }



   /**
    * Entry point for new multi-cancer, drools-free summary creation.
    * @param patientId -
    * @param annotations -
    * @param relations -
    * @return map of cancer summary to tumor summaries
    */
//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries2( final Collection<NoteSpecs> noteSpecs,
//   static private Collection<CancerCiSummary> createDiagnosisSummaries2( final Collection<NoteSpecs> noteSpecs,
//                                                                                  final String patientId,
//         final Map<IdentifiedAnnotation,String>  annotations,
//         final Collection<BinaryTextRelation> relations,
//         final Collection<CollectionTextRelation> patientCorefs,
//         final Map<Markable, Collection<IdentifiedAnnotation>> patientMarkables ) {
   static private PatientCiContainer createPatientSummary( final String patientId,
                                                           final Map<NoteSpecs,String> noteSpecs,
                                                           final Map<IdentifiedAnnotation,String>  annotations,
                                                           final Collection<BinaryTextRelation> relations,
                                                           final Collection<CollectionTextRelation> patientCorefs,
                                                           final Map<Markable, Collection<IdentifiedAnnotation>> patientMarkables ) {
//      LOGGER.info( "createCiDiagnoses calling ConceptInstanceFactory.createUriConceptInstanceMap with " + annotations.size() + " annotations." );
      final Map<String,Collection<ConceptInstance>> uriConceptInstances
            = ConceptInstanceFactory.createUriConceptInstanceMap2( patientId, annotations, relations, patientCorefs, patientMarkables );
//      LOGGER.info( "createDiagnosisSummaries2 using " + uriConceptInstances.size() + " uri concept instances" );
      return createPatientSummary( patientId, noteSpecs,  uriConceptInstances );
   }


//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,  final String patientId,
//static private Collection<CancerCiSummary> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,  final String patientId,
//                                                                                  final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
   static private PatientCiContainer createPatientSummary( final String patientId,
                                                           final Map<NoteSpecs,String> noteSpecs,
                                                           final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
      final Collection<ConceptInstance> allInstances = new HashSet<>();
      final Collection<ConceptInstance> neoplasmInstances = new HashSet<>();
      for ( Map.Entry<String, Collection<ConceptInstance>> instances : uriConceptInstances.entrySet() ) {
         allInstances.addAll( instances.getValue() );
         final String uri = instances.getKey();
         if ( !CANCERY_URIS.contains( uri ) ) {
            continue;
         }
         for ( ConceptInstance instance : instances.getValue() ) {
            if ( wantedForSummary( instance ) && hasWantedRelations( instance ) ) {
               neoplasmInstances.add( instance );
            }
         }
      }
//      neoplasmInstances.forEach( CiContainerFactory::writeRawConceptInstanceDebug );

//      return createDiagnosisSummaries( patientId, neoplasmInstances, allInstances );
      return createPatientSummary( patientId, noteSpecs, neoplasmInstances, allInstances );
   }

//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,
//   static private Collection<CancerCiSummary> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,
   static private PatientCiContainer createPatientSummary( final String patientId,
                                                           final Map<NoteSpecs,String> noteSpecs,
                                                           final Collection<ConceptInstance> neoplasmInstances,
                                                           final Collection<ConceptInstance> allInstances ) {
      final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap = NeoplasmMerger.mergeNeoplasms( neoplasmInstances, allInstances );
      return summarizeConceptInstanceMap( patientId, noteSpecs, diagnosisMap, allInstances );
   }



//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries(
//         final String patientId,
//         final Collection<ConceptInstance> neoplasmInstances,
//         final Collection<ConceptInstance> allInstances ) {
//      final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap = new HashMap<>();
//      final Collection<ConceptInstance> unLocatedCancers = new ArrayList<>();
//      final Collection<ConceptInstance> unLocatedMetastases = new ArrayList<>();
//      // Fill diagnosisMap with primary CI to tumor CIs.  Also fill the unLocated lists
//      DiagnosisUtil.createDiagnoses( neoplasmInstances, diagnosisMap, unLocatedCancers, unLocatedMetastases );
//      return createDiagnosisSummaries( patientId, diagnosisMap, unLocatedCancers, unLocatedMetastases, allInstances );
//   }





//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,
//   static private Collection<CancerCiSummary> createDiagnosisSummaries( final Collection<NoteSpecs> noteSpecs,
   static private PatientCiContainer summarizeConceptInstanceMap( final String patientId,
                                                                  final Map<NoteSpecs,String> noteSpecs,
                                                                  final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap,
                                                                  final Collection<ConceptInstance> allInstances ) {

      final Collection<CancerCiContainer> cancers = new ArrayList<>();
//      final Map<CiSummary,Collection<CiSummary>> summaryMap = new HashMap<>( diagnosisMap.size() );
      for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
         final ConceptInstance cancer = cancerTumors.getKey();
//         final CiSummary cancerSummary
//               = new CiSummary( TYPE_CANCER, noteSpecs, cancer.getUri(), Collections.singletonList( cancer ) );
         final NeoplasmCiContainer cancerSummary
               = new NeoplasmCiContainer( TYPE_CANCER, cancer );
         final Collection<NeoplasmCiContainer> tumorSummaries = new ArrayList<>( cancerTumors.getValue().size() );
         for ( ConceptInstance tumor : cancerTumors.getValue() ) {
            final String type = getCiSummaryType( tumor );
//            final CiSummary tumorSummary = new CiSummary( type, noteSpecs, tumor.getUri(), Collections.singletonList( tumor ) );
            final NeoplasmCiContainer tumorSummary = new NeoplasmCiContainer( type, tumor );
            tumorSummaries.add( tumorSummary );
         }
//         writeConceptSummaryDebug( cancerSummary );
//         tumorSummaries.forEach( CiContainerFactory::writeConceptSummaryDebug );
         cancers.add( new CancerCiContainer( cancerSummary, tumorSummaries ) );
//         summaryMap.put( cancerSummary, tumorSummaries );
      }
//      return summaryMap;
//      return cancers;
      final Collection<NoteCiContainer> noteCiSummaries = new ArrayList<>( noteSpecs.size() );
      for ( Map.Entry<NoteSpecs,String> noteSpecEntry : noteSpecs.entrySet() ) {
         final NoteCiContainer noteCiContainer = new NoteCiContainer( noteSpecEntry.getKey(), noteSpecEntry.getValue() );
         noteCiSummaries.add( noteCiContainer );
      }
      return new PatientCiContainer( patientId, noteCiSummaries, cancers, allInstances );
   }








//   static private Map<CiSummary,Collection<CiSummary>> createDiagnosisSummaries( final String patientId,
//                                                                                 final Collection<NoteSpecs> noteSpecs,
//
////         final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//         final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap,
//         final Collection<ConceptInstance> unLocatedCancers,
//         final Collection<ConceptInstance> unLocatedMetastases,
//         final Collection<ConceptInstance> allInstances ) {
//
//      final Map<CiSummary,Collection<CiSummary>> summaryMap = new HashMap<>( diagnosisMap.size() );
//
//      final Map<ConceptInstance,Collection<ConceptInstance>> locatedCancers = locateCancers( diagnosisMap, unLocatedCancers );
//
//      for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
//         final ConceptInstance cancer = cancerTumors.getKey();
//         final Collection<ConceptInstance> cancerList = new ArrayList<>();
//         cancerList.add( cancer );
//         final Collection<ConceptInstance> located = locatedCancers.get( cancer );
//         if ( located != null ) {
//            cancerList.addAll( located );
//         }
//         cancerList.addAll( unLocatedCancers );
//         final CiSummary cancerSummary = new CiSummary( TYPE_CANCER, noteSpecs, cancer.getUri(), cancerList );
//         final Collection<CiSummary> tumorSummaries = new ArrayList<>();
//         for ( ConceptInstance tumor : cancerTumors.getValue() ) {
////            final String type = getCiSummaryType( tumor, neoplasmTypes );
//            final String type = getCiSummaryType( tumor );
//            final CiSummary tumorSummary = new CiSummary( type, noteSpecs, tumor.getUri(), Collections.singletonList( tumor ) );
//            tumorSummaries.add( tumorSummary );
//         }
//         if ( diagnosisMap.size() == 1 ) {
//            // Assume that unlocated metastases are for the single cancer
//            for ( ConceptInstance metastasis : unLocatedMetastases ) {
//               tumorSummaries.add( new CiSummary( TYPE_METASTASIS, noteSpecs, metastasis.getUri(), Collections.singletonList( metastasis ) ) );
//            }
//         }
//         summaryMap.put( cancerSummary, tumorSummaries );
//         writeConceptSummaryDebug( cancerSummary );
//         tumorSummaries.forEach( CiSummaryFactory::writeConceptSummaryDebug );
//      }
//
//      if ( diagnosisMap.size() != 1 && !unLocatedMetastases.isEmpty() ) {
//         // create some unknown cancer and put all unlocated metastases in it
//         final CiSummary unknownCancer = createUnknownCancer( noteSpecs, patientId, unLocatedMetastases );
//         final Collection<CiSummary> metastasisSummaries = new ArrayList<>();
//         for ( ConceptInstance metastasis : unLocatedMetastases ) {
//            metastasisSummaries.add( new CiSummary( TYPE_METASTASIS, noteSpecs, metastasis.getUri(), Collections.singletonList( metastasis ) ) );
//         }
//         summaryMap.put( unknownCancer, metastasisSummaries );
//      }
//      return summaryMap;
//   }



   static private String getCiSummaryType( final ConceptInstance neoplasm ) {
      switch ( ConceptInstanceUtil.getNeoplasmType( neoplasm ) ) {
         case CANCER: return TYPE_PRIMARY; // TYPE_CANCER;
         case NON_CANCER: return TYPE_BENIGN;
         case PRIMARY: return TYPE_PRIMARY;
         case SECONDARY: return TYPE_METASTASIS;
      }
      return TYPE_GENERIC;
   }


//   static private CiSummary createUnknownCancer( final String patientId,
//                                                 final Collection<NoteSpecs> noteSpecs,
//                                                 final Collection<ConceptInstance> unLocatedMetastases ) {
//      final String metastasisDocs = unLocatedMetastases.stream()
//                                                       .map( ConceptInstance::getDocumentIds )
//                                                       .flatMap( Collection::stream )
//                                                       .distinct()
//                                                       .sorted()
//                                                       .collect( Collectors.joining( "_" ) );
//
//      return new CiSummary( TYPE_CANCER, noteSpecs, UriConstants.NEOPLASM, Collections.emptyList() )  {
//         public String getId() {
//            return patientId + "_" + metastasisDocs + "_" + TYPE_CANCER + "_" + UriConstants.NEOPLASM + "_" + Math.abs( unLocatedMetastases.hashCode() );
//         }
//      };
//   }


//   static private Map<ConceptInstance,Collection<ConceptInstance>> locateCancers(
//         final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap,
//         final Collection<ConceptInstance> unLocatedCancers ) {
//      if ( unLocatedCancers.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      final Map<ConceptInstance,Collection<ConceptInstance>> locatedCancers = new HashMap<>();
//      for ( ConceptInstance cancer : diagnosisMap.keySet() ) {
//         final Collection<String> cancerBranch = Neo4jOntologyConceptUtil.getBranchUris( cancer.getUri() );
//         for ( ConceptInstance unLocatedCancer : unLocatedCancers ) {
//            if ( UriUtil.isUriInBranch( unLocatedCancer.getUri(), cancerBranch ) ) {
//               locatedCancers.computeIfAbsent( cancer, c -> new ArrayList<>() ).add( unLocatedCancer );
//               // A bit of a hack, but try to trust it.
//               cancer.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE )
//                     .forEach( l -> unLocatedCancer.addRelated( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE, l ) );
//            }
//         }
//      }
//      locatedCancers.values().forEach( unLocatedCancers::removeAll );
//      return locatedCancers;
//   }

//   static private Map<String,Collection<ConceptInstance>> getDiagnosisMap( final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
//      final Map<String,Collection<ConceptInstance>> diagnosisMap = new HashMap<>();
//      for ( Collection<ConceptInstance> instances : uriConceptInstances.values() ) {
//         for ( ConceptInstance instance : instances ) {
//            final Collection<ConceptInstance> diagnoses = instance.getRelatedUris().get( RelationConstants.HAS_DIAGNOSIS );
//            if ( diagnoses != null ) {
//               diagnoses.forEach( d ->
//                     diagnosisMap.computeIfAbsent( d.getUri(), u -> new HashSet<>() ).add( instance ) );
//            }
//         }
//      }
//      return diagnosisMap;
//   }

//   static private Collection<ConceptInstance> getDiagnoses( final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
//      return uriConceptInstances.values().stream()
//                         .flatMap( Collection::stream )
//                         .map( ConceptInstance::getRelatedUris )
//                         .map( Map::entrySet )
//                         .flatMap( Collection::stream )
//                         .filter( e -> e.getKey().equals( RelationConstants.HAS_DIAGNOSIS ) )
//                         .filter( Objects::nonNull )
//                         .map( Map.Entry::getValue )
//                         .flatMap( Collection::stream )
//                         .collect( Collectors.toSet() );
//   }












   static private final Collection<String> UNNECESSARY_RELATIONS
         = Arrays.asList( HAS_TUMOR_EXTENT, HAS_TUMOR_TYPE );

   static private boolean hasWantedRelations( final ConceptInstance neoplasm ) {
      final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
      if ( related.size() > 2 ) {
         return true;
      }
      if ( related.size() < 2 ) {
         return false;
      }
      return related.keySet().stream().anyMatch( r -> !UNNECESSARY_RELATIONS.contains( r ) );
   }




//   static private Map<ConceptInstance,Integer> countDiagnoses( Map<ConceptInstance,Collection<ConceptInstance>> diagnoses ) {
//      final Map<ConceptInstance,Integer> diagnosedCount = new HashMap<>( diagnoses.size() );
//      for ( Collection<ConceptInstance> diagnosed : diagnoses.values() ) {
//         diagnosed.forEach( d -> diagnosedCount.merge( d, 1, Integer::sum ) );
//      }
//      diagnoses.keySet().forEach( d -> diagnosedCount.putIfAbsent( d, 0 ) );
//      return diagnosedCount;
//   }





//   /**
//    * Recursively builds up a map of cancers to tumors.  Some tumors may belong to more than one cancer.
//    * @param instances cancer and tumor concept instances
//    * @param neoplasmTypes -
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    */
//   static private void mapRootDiagnoses( final Collection<ConceptInstance> instances,
//                                         final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//                                         final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                         final Collection<ConceptInstance> usedInstances,
//                                         final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses ) {
//      final Collection<ConceptInstance> currentCancerTumors = new HashSet<>();
////      mapRootDiagnosis( instances, neoplasmTypes, instances, diagnosisMap, currentCancerTumors,
////            usedInstances, diagnoses, diagnosedCount );
//      mapRootDiagnosis( instances, neoplasmTypes, instances, diagnosisMap, currentCancerTumors,
//            usedInstances, diagnoses );
//   }


//   /**
//    * Recursively builds up a map of cancers to tumors.  Some tumors may belong to more than one cancer.
//    * @param instances cancer and tumor concept instances
//    * @param cancerCandidates instances that are candidates for cancers, but possibly tumors
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    * @param currentCancerTumors known tumors to store in any cancers found in the candidates
//    * @param usedInstances instances assigned as cancers or tumors to cancers
//    */
//   static private void mapRootDiagnosis( final Collection<ConceptInstance> instances,
//                                         final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//                                         final Collection<ConceptInstance> cancerCandidates,
//                                         final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                         final Collection<ConceptInstance> currentCancerTumors,
//                                         final Collection<ConceptInstance> usedInstances,
//                                         final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses ) {
//      for ( ConceptInstance cancerCandidate : cancerCandidates ) {
//         if ( !instances.contains( cancerCandidate ) ) {
//            // This candidate was hopefully subsumed by some other candidate.  Need to verify this ...
//            continue;
//         }
//         if ( currentCancerTumors.contains( cancerCandidate ) ) {
//            // The current cancer candidate is already a tumor candidate.  Prevent a loop.
//            continue;
//         }
//         // See if the cancer candidate is a known cancer already.
//         final Collection<ConceptInstance> diagnosedTumors = diagnosisMap.get( cancerCandidate );
//         if ( diagnosedTumors != null ) {
//            // The cancer candidate is already known to be a cancer, add the current tumors.
//            diagnosedTumors.addAll( currentCancerTumors );
//            usedInstances.addAll( currentCancerTumors );
//            // Move on to the next cancer candidate.
//            continue;
//         }
//         final Collection<ConceptInstance> candidateDiagnoses = diagnoses.get( cancerCandidate );
//         if ( candidateDiagnoses != null && !candidateDiagnoses.isEmpty() ) {
//            // Cancer Candidate should be treated as a tumor.  Add it to the list of tumors for the eventual cancer.
//            currentCancerTumors.add( cancerCandidate );
//            mapRootDiagnosis( instances, neoplasmTypes, candidateDiagnoses, diagnosisMap, currentCancerTumors,
//                  usedInstances, diagnoses );
//            // Move on to the next cancer candidate.
////            continue;
//         }
//         // No associated diagnosis, handle these tumors later.
//      }
//   }



//   /**
//    * Recursively builds up a map of cancers to tumors.  Some tumors may belong to more than one cancer.
//    * @param instances cancer and tumor concept instances
//    * @param cancerCandidates instances that are candidates for cancers, but possibly tumors
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    * @param currentCancerTumors known tumors to store in any cancers found in the candidates
//    * @param usedInstances instances assigned as cancers or tumors to cancers
//    */
//   static private void mapRootDiagnosisOld( final Collection<ConceptInstance> instances,
//                                         final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//                                         final Collection<ConceptInstance> cancerCandidates,
//                                         final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                         final Collection<ConceptInstance> currentCancerTumors,
//                                         final Collection<ConceptInstance> usedInstances,
//                                         final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses,
//                                         final Map<ConceptInstance,Integer> diagnosedCount ) {
//      for ( ConceptInstance cancerCandidate : cancerCandidates ) {
//         if ( !instances.contains( cancerCandidate ) ) {
//            // This candidate was hopefully subsumed by some other candidate.  Need to verify this ...
//            continue;
//         }
//         if ( currentCancerTumors.contains( cancerCandidate ) ) {
//            // The current cancer candidate is already a tumor candidate.  Prevent a loop.
//            continue;
//         }
//         // See if the cancer candidate is a known cancer already.
//         final Collection<ConceptInstance> diagnosedTumors = diagnosisMap.get( cancerCandidate );
//         if ( diagnosedTumors != null ) {
//            // The cancer candidate is already known to be a cancer, add the current tumors.
//            diagnosedTumors.addAll( currentCancerTumors );
//            usedInstances.addAll( currentCancerTumors );
//            // Move on to the next cancer candidate.
//            continue;
//         }
//         // Check to see if the cancer candidate is a tumor with a better cancer diagnosis ...
//         final Collection<ConceptInstance> betterDiagnoses
//               = getBetterDiagnoses( cancerCandidate, diagnoses, diagnosedCount );
//         if ( !betterDiagnoses.isEmpty() ) {
//            // Cancer Candidate should be treated as a tumor.  Add it to the list of tumors for the eventual cancer.
//            currentCancerTumors.add( cancerCandidate );
//            mapRootDiagnosisOld( instances, neoplasmTypes, betterDiagnoses, diagnosisMap, currentCancerTumors,
//                  usedInstances, diagnoses, diagnosedCount );
//            // Move on to the next cancer candidate.
//            continue;
//         }
//         // The neoplasm has no associated diagnosis.  Treat it as a root cancer if possible.
//         final NeoplasmType type = neoplasmTypes.get( cancerCandidate );
//         if ( type != SECONDARY ) {
//            // This cancerCandidate can be treated as a cancer.  Add tumors ...
//            final Collection<ConceptInstance> tumors
//                  = diagnosisMap.computeIfAbsent( cancerCandidate, c -> new HashSet<>() );
//            tumors.addAll( currentCancerTumors );
//            if ( type == PRIMARY ) {
//               // a primary tumor can be treated as both a cancer and a tumor
//               tumors.add( cancerCandidate );
//            }
//            usedInstances.add( cancerCandidate );
//            usedInstances.addAll( currentCancerTumors );
//         }
//         // else This candidate is a metastasis without a diagnosis / associated primary.
//         // Skip and handle it later.
//      }
//   }


//   static private Collection<ConceptInstance> getBetterDiagnoses(
//         final ConceptInstance instance,
//         final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses,
//         final Map<ConceptInstance,Integer> diagnosedCount ) {
//      final Collection<ConceptInstance> diagnosedCancers = diagnoses.get( instance );
//      if ( diagnosedCancers.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final int candidateCount = diagnosedCount.get( instance );
//      final Collection<ConceptInstance> betterDiagnoses = new ArrayList<>();
//      for ( ConceptInstance diagnosis : diagnosedCancers ) {
//         if ( diagnosedCount.get( diagnosis ) >= candidateCount ) {
//            betterDiagnoses.add( diagnosis );
//         }
//      }
//      return betterDiagnoses;
//   }













   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Diagnose undiagnosed primaries by location
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////

//   /**
//    * If a tumor is a primary that shares the same location as some cancer, place the tumor in that cancer.
//    * @param instances cancer and tumor concept instances
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    * @param usedInstances instances assigned as cancers or tumors to cancers
//    */
//   static private boolean assignPrimariesByLocation( final Collection<ConceptInstance> instances,
//                                                  final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//                                                  final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                                  final Collection<ConceptInstance> usedInstances ) {
//      if ( usedInstances.size() == instances.size() ) {
//         return true;
//      }
//      for ( ConceptInstance candidate : instances ) {
//         // Do not assign metastases by location
//         if ( usedInstances.contains( candidate ) || SECONDARY == neoplasmTypes.get( candidate ) ) {
//            continue;
//         }
//         final Collection<ConceptInstance> candidateLocations
//               = candidate.getRelatedUris().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
//         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
//            // no location to use for orphan.
//            continue;
//         }
//         for ( Map.Entry<ConceptInstance, Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
//            final Collection<ConceptInstance> cancerLocations
//                  = cancerTumors.getKey().getRelatedUris().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
//            if ( ConceptInstanceUtil.isUriBranchMatch( candidateLocations, cancerLocations ) ) {
//               cancerTumors.getValue().add( candidate );
//               usedInstances.add( candidate );
////               break;
//            }
//         }
//      }
//      return usedInstances.size() == instances.size();
//   }


   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Diagnose undiagnosed tumors by stage
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////

//   /**
//    * If a tumor has a stage that agrees with some cancer's stage or a stage of one of its tumors, add it as a tumor.
//    * @param instances cancer and tumor concept instances
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    * @param usedInstances instances assigned as cancers or tumors to cancers
//    */
//   static private void assignTumorsByStage( final Collection<ConceptInstance> instances,
//                                            final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                            final Collection<ConceptInstance> usedInstances ) {
//      for ( ConceptInstance instance : instances ) {
//         if ( usedInstances.contains( instance ) ) {
//            continue;
//         }
//         for ( Map.Entry<ConceptInstance, Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
//            if ( isStageMatch( instance, cancerTumors.getKey(), cancerTumors.getValue() ) ) {
//               cancerTumors.getValue().add( instance );
//               usedInstances.add( instance );
////               break;
//            }
//         }
//      }
//   }



   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Diagnose undiagnosed tumors by type or histology
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////

//   /**
//    * If a tumor has a histology that agrees with some cancer's histology or the histology of one of its tumors, add it as a tumor.
//    * @param instances cancer and tumor concept instances
//    * @param diagnosisMap map to fill with cancers (key) to tumors for each cancer (value)
//    * @param usedInstances instances assigned as cancers or tumors to cancers
//    */
//   static private void diagnoseTumorsByHistology( final Collection<ConceptInstance> instances,
//                                                  final Map<ConceptInstance,NeoplasmType> neoplasmTypes,
//                                                  final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
//                                                  final Collection<ConceptInstance> usedInstances ) {
//      final Map<ConceptInstance,String> cancerTumorHistologies = new HashMap<>();
//      for ( ConceptInstance candidate : instances ) {
//         // Do not assign metastases by location
//         if ( usedInstances.contains( candidate ) ) {
//            continue;
//         }
//         final String histology = CiSummaryUtil.getHistology( candidate.getUri() );
//         if ( histology == null || histology.isEmpty() ) {
//            // no histology to use for orphan.
//            continue;
//         }
//         for ( Map.Entry<ConceptInstance, Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
//            final String cancerHistology
//                  = cancerTumorHistologies.computeIfAbsent( cancerTumors.getKey(),
//                  c -> CiSummaryUtil.getHistology( cancerTumors.getKey().getUri() ) );
//            if ( histology.equals( cancerHistology ) ) {
//               cancerTumors.getValue().add( candidate );
//               usedInstances.add( candidate );
//               continue;
//            }
//            for ( ConceptInstance tumor : cancerTumors.getValue() ) {
//               final String tumorHistology
//                     = cancerTumorHistologies.computeIfAbsent( tumor,
//                     c -> CiSummaryUtil.getHistology( tumor.getUri() ) );
//               if ( histology.equals( tumorHistology ) ) {
//                  cancerTumors.getValue().add( candidate );
//                  usedInstances.add( candidate );
////                  break;
//               }
//            }
//         }
//      }
//   }

//   static private boolean isHistologyMatch( final ConceptInstance candidate,
//                                            final ConceptInstance cancer,
//                                            final Collection<ConceptInstance> tumors ) {
//      return isHistologyMatch( CiSummaryUtil.getHistology( candidate.getUri() ), cancer, tumors );
//   }








//   static private Map<ConceptInstance,Collection<ConceptInstance>> getDiagnosisCiMap( final Collection<ConceptInstance> instances ) {
//      final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap = new HashMap<>();
//      final Collection<ConceptInstance> usedInstances = new HashSet<>();
//      for ( ConceptInstance instance : instances ) {
//         // Check cancer by tumor diagnosis
//         final Collection<ConceptInstance> diagnoses = instance.getRelatedUris().get( RelationConstants.HAS_DIAGNOSIS );
//         if ( diagnoses != null ) {
//            // CI is 'most likely' tumor, diagnosis is 'most likely' a cancer.
//            for ( ConceptInstance diagnosis : diagnoses ) {
//               if ( instances.contains( diagnosis ) ) {
//                  diagnosisMap.computeIfAbsent( diagnosis, d -> new HashSet<>() ).add( instance );
//                  usedInstances.add( instance );
//                  usedInstances.add( diagnosis );
//               }
//            }
//         } else {
//            // Check cancer by tumor metastasis
//            final Collection<ConceptInstance> metastases = instance.getRelatedUris().get( RelationConstants.METASTASIS_OF );
//            if ( metastases != null ) {
//               // CI is 'most likely' a tumor, a metastasis of what is 'most likely' a cancer.
//               for ( ConceptInstance metastasis : metastases ) {
//                  if ( instances.contains( metastasis ) ) {
//                     diagnosisMap.computeIfAbsent( metastasis, d -> new HashSet<>() ).add( instance );
//                     usedInstances.add( instance );
//                     usedInstances.add( metastasis );
//                  }
//               }
//            }
//         }
//      }
////      coLocateCancers( diagnosisMap );
//
//      for ( ConceptInstance instance : instances ) {
//         // go through CIs again, assigning any orphans
//         if ( usedInstances.contains( instance ) ) {
//            continue;
//         }
//         if ( instance.getRelatedUris().get( RelationConstants.HAS_TUMOR_EXTENT ) != null ) {
//            // CI is 'most likely' a tumor, but without an assigned cancer.
//
//
//
//            //  Add to a "null" cancer.
//            diagnosisMap.computeIfAbsent( ConceptInstance.NULL_INSTANCE, d -> new HashSet<>() ).add( instance );
//         } else {
//            // CI is 'most likely' a cancer.  Could check HAS_STAGE, HAS_TNM
//            diagnosisMap.computeIfAbsent( instance, d -> new HashSet<>() );
//         }
//         usedInstances.add( instance );
//      }
//      return diagnosisMap;
//   }


//   static private Collection<ConceptInstance> getCoLocationCancers(
//         final ConceptInstance instance,
//         final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap ) {
//      final Collection<ConceptInstance> locations = instance.getRelatedUris().get( RelationConstants.HAS_DIAGNOSIS );
//      if ( locations == null || locations.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final Collection<ConceptInstance> cancersByLocation = new ArrayList<>();
//      final Collection<ConceptInstance> cancersByTumorLocation = new ArrayList<>();
//      for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> diagnosisEntry : diagnosisMap.entrySet() ) {
//         final Collection<ConceptInstance> cancerLocations
//               = diagnosisEntry.getKey().getRelatedUris().get( RelationConstants.HAS_DIAGNOSIS );
//         if ( cancerLocations != null ) {
//         }
//      }
//      if ( !cancersByLocation.isEmpty() ) {
//         return cancersByLocation;
//      }
//      return cancersByTumorLocation;
//   }


//   /**
//    *
//    * @param uriConceptInstances map of tumor / cancer uris and their concept instances
//    * @return map of diagnosis uris and all input concept instances arranged by diagnosis
//    */
//   static private Map<String,Map<String,Collection<ConceptInstance>>> groupByDiagnosis( final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
//      final Map<String,Map<String,Collection<ConceptInstance>>> diagnosisMap = new HashMap<>();
//      for ( Map.Entry<String,Collection<ConceptInstance>> instances : uriConceptInstances.entrySet() ) {
//         final String instanceUri = instances.getKey();
//         for ( ConceptInstance instance : instances.getValue() ) {
//            final Collection<ConceptInstance> diagnoses = instance.getRelatedUris().get( RelationConstants.HAS_DIAGNOSIS );
//            if ( diagnoses != null ) {
//               for ( ConceptInstance diagnosis : diagnoses ) {
//                  diagnosisMap.computeIfAbsent( diagnosis.getUri(), d -> new HashMap<>() )
//                              .computeIfAbsent( instanceUri, u -> new HashSet<>() )
//                              .add( instance );
//               }
//            }
//         }
//      }
//      return diagnosisMap;
//   }


   static private boolean wantedForSummary( final ConceptInstance instance ) {
      return !instance.isNegated() && !instance.isUncertain() && !instance.isGeneric() && !instance.isConditional()
             && CONST.ATTR_SUBJECT_PATIENT.equals( instance.getSubject() )
//             && (instance.getRelatedUris().size() + instance.getReverseRelated().size()) != 0
             && !instance.getRelated().isEmpty()
             // TODO Temporary filter for drools
             && !instance.getUri().contains( "Breslow" );
   }





//   static private final String DEBUG_DIR = "C:\\Spiffy\\dphe_output\\output_04_19_2019\\patientX\\brca\\test\\DEBUG\\";
//
   static private Collection<String> CANCERY_URIS = new ArrayList<>();
   static {
//      CANCERY_URIS.addAll( UriConstants.getCancerUris() );
      CANCERY_URIS.addAll( UriConstants.getTumorUris() );
//      CANCERY_URIS.removeAll( UriConstants.getBenignTumorUris() );
//      new File( DEBUG_DIR ).mkdirs();
   }
//
//   static private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
//   static private void writeRawConceptInstanceDebug( final ConceptInstance ci ) {
//      if ( !DEBUG ) { //|| !CANCERY_URIS.contains( ci.getUri() ) ) {
//         return;
//      }
//      final String patient = ci.getPatientId();
//      try ( Writer writer = new FileWriter( DEBUG_DIR + patient+"_raw_concepts.txt", true ) ) {
//         writer.write( TIME_FORMATTER.format( LocalTime.now() )
//                       + "    ===================================================\n" + ci.toText() + "\n" );
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//      }
//   }
//
//   static private void writeConceptSummaryDebug( final CiContainer summary ) {
//      if ( !DEBUG ) {
//         return;
//      }
////      final String patient = summary.getId().substring( 0, summary.getId().indexOf( '_' )+1 );
//      try ( Writer writer = new FileWriter( DEBUG_DIR + summary.getId() + "_summary_concepts.txt", true ) ) {
//         writer.write( TIME_FORMATTER.format( LocalTime.now() )
//                       + "    ===================================================\n" + summary.toString() + "\n"  );
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//      }
//   }


}
