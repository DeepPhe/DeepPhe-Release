package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.uri.UriConstants.*;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/17/2016
 */
final public class ConceptInstanceUtil {

   static private final Logger LOGGER = Logger.getLogger( "ConceptInstanceUtil" );

   public enum RelationDirection {
      FORWARD, REVERSE, ALL
   }

   public enum NeoplasmType {
      CANCER,
      PRIMARY,
      SECONDARY,
      NON_CANCER,
      UNKNOWN
   }

   private ConceptInstanceUtil() {
   }

   static public Collection<String> getUris( final Collection<ConceptInstance> instances ) {
      return instances.stream()
                      .map( ConceptInstance::getUri )
                      .collect( Collectors.toSet() );
   }

   static public Collection<String> getRelatedUris( final String relationName,
                                                    final ConceptInstance instance ) {
      final Collection<ConceptInstance> related = instance.getRelated().get( relationName );
      if ( related == null ) {
         return Collections.emptyList();
      }
      return getUris( related );
   }


   static public Collection<ConceptInstance> getRelated( final String relationName,
                                                         final ConceptInstance instance ) {
      final Collection<ConceptInstance> related = instance.getRelated().get( relationName );
      if ( related == null ) {
         return Collections.emptyList();
      }
      return related;
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Neoplasm to Type Matching
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param neoplasm neoplasm, type unknown
    * @return Secondary : metastasis uri or Invasive_Lesion; Primary : not INVASIVE_LESION, Cancer : no tumor extent
    */
   static public NeoplasmType getNeoplasmType( final ConceptInstance neoplasm ) {
      if ( neoplasm.getUri().contains( "In_Situ" ) ) {
         // An in situ has to be primary.
         return NeoplasmType.PRIMARY;
      }
      final Collection<String> stages = UriConstants.getCancerStages();
      if ( getRelated( HAS_STAGE, neoplasm ).stream().map( ConceptInstance::getUri ).anyMatch( stages::contains ) ) {
          return NeoplasmType.PRIMARY;
      }

      final Collection<String> relations = neoplasm.getRelated().keySet();
      if ( relations.contains( HAS_CLINICAL_T )
           || relations.contains( HAS_CLINICAL_N )
           || relations.contains( HAS_CLINICAL_M )
           || relations.contains( HAS_PATHOLOGIC_T )
           || relations.contains( HAS_PATHOLOGIC_N )
           || relations.contains( HAS_PATHOLOGIC_M )
      ) {
         return NeoplasmType.PRIMARY;
      }
      final Collection<String> diagnoses = new ArrayList<>();
      diagnoses.add( neoplasm.getUri() );
      ConceptInstanceUtil.getRelated( HAS_DIAGNOSIS, neoplasm ).stream()
                         .map( ConceptInstance::getUri )
                         .forEach( diagnoses::add );
      final boolean benignUri = UriConstants.getBenignTumorUris().stream()
                                            .anyMatch( diagnoses::contains );
      if ( benignUri ) {
         return NeoplasmType.NON_CANCER;
      }
      // Check metastases first so that we don't get something like historic In_Situ in a lymph node
      final boolean metastasisUri = UriConstants.getMetastasisUris().stream()
                                                .anyMatch( diagnoses::contains );
      if ( metastasisUri ) {
         return NeoplasmType.SECONDARY;
      }
      final boolean primaryUri = UriConstants.getPrimaryUris().stream()
                                             .anyMatch( diagnoses::contains );
      if ( primaryUri ) {
         return NeoplasmType.PRIMARY;
      }
      final Collection<ConceptInstance> tumorExtents
            = ConceptInstanceUtil.getRelated( HAS_TUMOR_EXTENT, neoplasm );
      final Collection<String> extentUris = tumorExtents.stream()
                                                        .map( ConceptInstance::getUri )
                                                        .collect( Collectors.toList() );
      if ( extentUris.contains( "In_Situ_Lesion" ) ) {
         return NeoplasmType.PRIMARY;
      } else if ( extentUris.contains( "Invasive_Lesion" )
                  || extentUris.contains( "Metastatic_Lesion" ) ) {
         return NeoplasmType.SECONDARY;
      }
      final Collection<ConceptInstance> metastasesOf
            = ConceptInstanceUtil.getRelated( METASTASIS_OF, neoplasm );
      if ( !metastasesOf.isEmpty() ) {
         return NeoplasmType.SECONDARY;
      }
      final Collection<ConceptInstance> primarySites
            = ConceptInstanceUtil.getRelated( DISEASE_HAS_PRIMARY_ANATOMIC_SITE, neoplasm );
      if ( !primarySites.isEmpty() ) {
         // This won't work for lymphoma
         final Collection<String> lymphSiteUris = Neo4jOntologyConceptUtil.getBranchUris( LYMPH_NODE );
         if ( primarySites.stream().map( ConceptInstance::getUri ).anyMatch( lymphSiteUris::contains ) ) {
            return NeoplasmType.SECONDARY;
         }
      }
      return NeoplasmType.UNKNOWN;
   }


   static private final  Collection<String> LOCATION_FACT
         = Arrays.asList( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
         HAS_BODY_MODIFIER,
         HAS_LATERALITY,
         HAS_QUADRANT,
         HAS_CLOCKFACE
   );
   static public boolean isLocationFact( final String category ) {
      return LOCATION_FACT.contains( category );
   }


   static private final  Collection<String> CANCER_FACT
         = Arrays.asList( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
         HAS_BODY_MODIFIER,
         HAS_LATERALITY,
         HAS_CANCER_TYPE,
         HAS_HISTOLOGY
   );
   static private boolean isCancerFact( final String category ) {
      return CANCER_FACT.contains( category );
   }

   static private final Collection<String> CANCER_ONLY_FACT
         = Arrays.asList( HAS_CLINICAL_T,
         HAS_CLINICAL_N,
         HAS_CLINICAL_M,
         HAS_PATHOLOGIC_T,
         HAS_PATHOLOGIC_N,
         HAS_PATHOLOGIC_M,
         HAS_STAGE );
   static public boolean isCancerOnlyFact( final String category ) {
      return CANCER_ONLY_FACT.contains( category );
   }

   static private final Collection<String> TUMOR_ONLY_FACT = Arrays.asList(
         HAS_DIAGNOSIS,
         METASTASIS_OF,
         HAS_SIZE,
         HAS_TUMOR_TYPE,
         HAS_TUMOR_EXTENT,
         HAS_CALCIFICATION,
         HAS_ULCERATION,
         HAS_BRESLOW_DEPTH,
         HAS_SIZE,
         HAS_ER_STATUS,
         HAS_PR_STATUS,
         HAS_HER2_STATUS,
         HAS_QUADRANT,
         HAS_CLOCKFACE
         );
   static public boolean isTumorOnlyFact( final String category ) {
      return TUMOR_ONLY_FACT.contains( category );
   }


   static public Map<String,Collection<ConceptInstance>> getCancerFacts(
         final Collection<ConceptInstance> neoplasms ) {
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
      for ( ConceptInstance neoplasm : neoplasms ) {
         for ( String fact : CANCER_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( getRelated( fact, neoplasm ) );
         }
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( getRelated( fact, neoplasm ) );
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptInstance>> getCancerOnlyFacts(
         final Collection<ConceptInstance> neoplasms ) {
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
      for ( ConceptInstance neoplasm : neoplasms ) {
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( getRelated( fact, neoplasm ) );
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptInstance>> getNonLocationFacts(
         final Collection<ConceptInstance> neoplasms ) {
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
      for ( ConceptInstance neoplasm : neoplasms ) {
         final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : related.entrySet() ) {
            if ( !LOCATION_FACT.contains( relation.getKey() ) ) {
               allRelated.computeIfAbsent( relation.getKey(), f -> new HashSet<>() )
                         .addAll( relation.getValue() );
            }
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptInstance>> getNonLocationFacts( final ConceptInstance neoplasm ) {
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>( neoplasm.getRelated() );
      allRelated.keySet().removeAll( LOCATION_FACT );
      return allRelated;
   }




   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Concept Instance Matching
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


   static public boolean isUriBranchMatch( final Collection<ConceptInstance> instances1,
                                           final Collection<ConceptInstance> instances2 ) {
      if ( instances1 == null || instances2 == null ) {
         return false;
      }
      final Collection<String> uris1 = getUris( instances1 );
      final Collection<String> uris2 = getUris( instances2 );
      return UriUtil.isUriBranchMatch( uris1, uris2 );
   }


   static private int countPropertyMatch( final Collection<ConceptInstance> properties1,
                                          final Collection<ConceptInstance> properties2 ) {
      if ( properties1 == null || properties2 == null ) {
         return 0;
      }
      int matches = 0;
      for ( ConceptInstance property1 : properties1 ) {
         final String uri1 = property1.getUri();
         final Collection<String> branch1 = Neo4jOntologyConceptUtil.getBranchUris( uri1 );
         for ( ConceptInstance property2 : properties2 ) {
            final String uri2 = property2.getUri();
            if ( branch1.contains( uri2 ) ) {
               matches++;
               continue;
            }
            final Collection<String> branch2 = Neo4jOntologyConceptUtil.getBranchUris( property1.getUri() );
            if ( branch2.contains( uri1 ) ) {
               matches++;
            }
         }
      }
      return matches;
   }



   static public boolean anyUriMatch( final Collection<ConceptInstance> instances1,
                                      final Collection<ConceptInstance> instances2 ) {
      if ( instances1 == null || instances2 == null ) {
         return false;
      }
      final Collection<String> uris1 = getUris( instances1 );
      return instances2.stream().map( ConceptInstance::getUri ).anyMatch( uris1::contains );
   }



   static public Collection<ConceptInstance> getMostSpecificInstances( final Collection<ConceptInstance> instances ) {
      if ( instances.size() == 1 ) {
         return instances;
      }
      final Map<String,List<ConceptInstance>> uriInstances = mapUriInstances( instances );
      if ( uriInstances.size() == 1 ) {
         return instances;
      }
      final String bestUri = UriUtil.getMostSpecificUri( uriInstances.keySet() );
      return uriInstances.get( bestUri );
   }

   static public Map<String,List<ConceptInstance>> mapUriInstances( final Collection<ConceptInstance> instances ) {
      return instances.stream().collect( Collectors.groupingBy( ConceptInstance::getUri ) );
   }













      static public Map<String, Collection<ConceptInstance>> getFullRelations( final ConceptInstance conceptInstance ) {
      return getRelations( conceptInstance, RelationDirection.FORWARD );
   }

   static public Map<String, Collection<ConceptInstance>> getRelations( final ConceptInstance conceptInstance,
                                                                        final RelationDirection direction ) {
      final Map<String, Collection<ConceptInstance>> relations = new HashMap<>();
      final Map<String, Collection<ConceptInstance>> usedRelations = new HashMap<>();
      int wantedDepth = 1;
      Map<String, Collection<ConceptInstance>> layerRelations
            = getLayerRelations( usedRelations, 1, wantedDepth, conceptInstance, direction );
      while ( !layerRelations.isEmpty() ) {
         for ( Map.Entry<String, Collection<ConceptInstance>> entry : layerRelations.entrySet() ) {
            if ( !entry.getValue().isEmpty() ) {
               relations.computeIfAbsent( entry.getKey(), c -> new HashSet<>() ).addAll( entry.getValue() );
            }
         }
         wantedDepth++;
         layerRelations = getLayerRelations( usedRelations, 1, wantedDepth, conceptInstance, direction );
      }
      return relations;
   }

   static private Map<String, Collection<ConceptInstance>> getLayerRelations(
         final Map<String, Collection<ConceptInstance>> usedRelations,
         final int depth,
         final int wantedDepth,
         final ConceptInstance conceptInstance,
         final RelationDirection direction ) {
      if ( conceptInstance == null ) {
         LOGGER.error( "Null Concept Instance" );
         return Collections.emptyMap();
      }
      final Map<String, Collection<ConceptInstance>> relations = new HashMap<>();
      if ( depth == wantedDepth ) {
         if ( direction != RelationDirection.REVERSE ) {
            for ( Map.Entry<String, Collection<ConceptInstance>> entry : conceptInstance.getRelated().entrySet() ) {
               final String type = entry.getKey();
               final Collection<ConceptInstance> usedInstances = usedRelations
                     .computeIfAbsent( type, t -> new ArrayList<>() );
               for ( ConceptInstance instance : entry.getValue() ) {
                  if ( !usedInstances.contains( conceptInstance ) || !usedInstances.contains( instance ) ) {
                     relations.computeIfAbsent( type, c -> new HashSet<>() ).add( instance );
                     usedInstances.add( conceptInstance );
                     usedInstances.add( instance );
                  }
               }
            }
         }
         return relations;
      }
      if ( direction != RelationDirection.REVERSE ) {
         for ( Collection<ConceptInstance> instances : conceptInstance.getRelated().values() ) {
            for ( ConceptInstance instance : instances ) {
               final Map<String, Collection<ConceptInstance>> forwards
                     = getLayerRelations( usedRelations, depth + 1, wantedDepth, instance, direction );
               for ( Map.Entry<String, Collection<ConceptInstance>> forward : forwards.entrySet() ) {
                  relations.computeIfAbsent( forward.getKey(), c -> new HashSet<>() ).addAll( forward.getValue() );
               }
            }
         }
      }
      return relations;
   }

   static public Map<String,String> getProperties( final ConceptInstance instance ) {
      final Map<String,String> properties = new HashMap<>();
      properties.put( PREF_TEXT_KEY, instance.getPreferredText() );
      if ( instance.isNegated() ) {
         properties.put( INSTANCE_NEGATED, TRUE );
      }
      if ( instance.isUncertain() ) {
         properties.put( INSTANCE_UNCERTAIN, TRUE );
      }
      if ( instance.isConditional() ) {
         properties.put( INSTANCE_CONDITIONAL, TRUE );
      }
      if ( instance.isGeneric() ) {
         properties.put( INSTANCE_GENERIC, TRUE );
      }
      final String temporality = getTemporality( instance );
      if ( !temporality.isEmpty() ) {
         properties.put( INSTANCE_TEMPORALITY, temporality );
      }
      final String value = getValue( instance.getUri() );
      if ( !value.isEmpty() ) {
         properties.put( VALUE_TEXT, value );
         final String valueKey = getValueKey( instance.getUri(), value );
         properties.put( VALUE_KEY_TEXT, valueKey );
      }
      return properties;
   }

   static private String getTemporality( final ConceptInstance instance ) {
      final String dtr = instance.getDocTimeRel();
      if ( dtr == null ) {
         return "";
      }
      if ( dtr.equalsIgnoreCase( "Before" ) ) {
         return "Historic";
      }
      return "Current";
   }

   static private String getValue( final String uri ) {
      if ( UriConstants.getCancerStages().contains( uri ) ) {
         return getStageText( uri );
      }
      if ( uri.endsWith( "_Positive" ) ) {
         return "Positive";
      } else if ( uri.endsWith( "_Negative" ) ) {
         return "Negative";
      } else if ( uri.endsWith( "_Unknown" ) ) {
         return "Unknown";
      } else if ( uri.equals( TRIPLE_NEGATIVE ) ) {
         return "Negative";
      } else if ( uri.endsWith( "_Stage_Finding" ) ) {
         return uri.substring( 0, uri.indexOf( "_Stage_Finding" ) );
      }
      return "";
   }

   static private String getValueKey( final String uri, final String valueText ) {
      if ( UriConstants.getCancerStages().contains( uri ) ) {
         return "Summary Stage";
      }
      if ( uri.endsWith( "_Stage_Finding" ) ) {
         return "Stage";
      }
      final int valueIndex = uri.indexOf( valueText );
      return uri.substring( 0, valueIndex - 1 ).replace( "_", " " );
   }

   static private String getStageText( final String uri ) {
      final String prefText = Neo4jOntologyConceptUtil.getPreferredText( uri );
      if ( prefText.contains( "IV" ) ) {
         return "Stage IV";
      } else if ( prefText.contains( "III" ) ) {
         return "Stage III";
      } else if ( prefText.contains( "II" ) ) {
         return "Stage II";
      } else if ( prefText.contains( "I" ) ) {
         return "Stage I";
      } else if ( prefText.contains( "0" ) ) {
         return "Stage 0";
      } else if ( prefText.contains( "4" ) ) {
         return "Stage IV";
      } else if ( prefText.contains( "3" ) ) {
         return "Stage III";
      } else if ( prefText.contains( "2" ) ) {
         return "Stage II";
      } else if ( prefText.contains( "1" ) ) {
         return "Stage I";
      }
      return prefText;
   }


}
