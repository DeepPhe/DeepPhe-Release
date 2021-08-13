package org.healthnlp.deepphe.util;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.UriConstants.TRIPLE_NEGATIVE;
import static org.healthnlp.deepphe.neo4j.constant.UriConstants.TRUE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/17/2016
 */
final public class ConceptAggregateUtil {

   static private final Logger LOGGER = Logger.getLogger( "ConceptAggregateUtil" );

   public enum RelationDirection {
      FORWARD, REVERSE, ALL
   }

   private ConceptAggregateUtil() {
   }


   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Neoplasm to Type Matching
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////




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


   static public Map<String,Collection<ConceptAggregate>> getCancerFacts(
         final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         for ( String fact : CANCER_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptAggregate>> getCancerOnlyFacts(
         final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptAggregate>> getNonLocationFacts(
         final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final Map<String,Collection<ConceptAggregate>> related = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> relation : related.entrySet() ) {
            if ( !LOCATION_FACT.contains( relation.getKey() ) ) {
               allRelated.computeIfAbsent( relation.getKey(), f -> new HashSet<>() )
                         .addAll( relation.getValue() );
            }
         }
      }
      return allRelated;
   }

   static public Map<String,Collection<ConceptAggregate>> getNonLocationFacts( final ConceptAggregate neoplasm ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>( neoplasm.getRelatedConceptMap() );
      allRelated.keySet().removeAll( LOCATION_FACT );
      return allRelated;
   }




   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Concept Instance Matching
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


   static public boolean isUriBranchMatch( final Collection<ConceptAggregate> instances1,
                                           final Collection<ConceptAggregate> instances2 ) {
      if ( instances1 == null || instances2 == null ) {
         return false;
      }
      final Collection<String> uris1 = instances1.stream().map( ConceptAggregate::getUri ).collect( Collectors.toSet() );
      final Collection<String> uris2 = instances2.stream().map( ConceptAggregate::getUri ).collect( Collectors.toSet() );
      return UriUtil.isUriBranchMatch( uris1, uris2 );
   }


   static private int countPropertyMatch( final Collection<ConceptAggregate> properties1,
                                          final Collection<ConceptAggregate> properties2 ) {
      if ( properties1 == null || properties2 == null ) {
         return 0;
      }
      int matches = 0;
      for ( ConceptAggregate property1 : properties1 ) {
         final String uri1 = property1.getUri();
         final Collection<String> branch1 = Neo4jOntologyConceptUtil.getBranchUris( uri1 );
         for ( ConceptAggregate property2 : properties2 ) {
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



   static public boolean anyUriMatch( final Collection<ConceptAggregate> instances1,
                                      final Collection<ConceptAggregate> instances2 ) {
      if ( instances1 == null || instances2 == null ) {
         return false;
      }
      final Collection<String> uris1 = instances1.stream().map( ConceptAggregate::getUri ).collect( Collectors.toSet() );
      return instances2.stream().map( ConceptAggregate::getUri ).anyMatch( uris1::contains );
   }



   static public Collection<ConceptAggregate> getMostSpecificInstances( final Collection<ConceptAggregate> instances ) {
      if ( instances.size() == 1 ) {
         return instances;
      }
      final Map<String,List<ConceptAggregate>> uriInstances = mapUriInstances( instances );
      if ( uriInstances.size() == 1 ) {
         return instances;
      }
      final String bestUri = UriUtil.getMostSpecificUri( uriInstances.keySet() );
      return uriInstances.get( bestUri );
   }

   static public Map<String,List<ConceptAggregate>> mapUriInstances( final Collection<ConceptAggregate> instances ) {
      return instances.stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
   }













      static public Map<String, Collection<ConceptAggregate>> getFullRelations( final ConceptAggregate ConceptAggregate ) {
      return getRelations( ConceptAggregate, RelationDirection.FORWARD );
   }

   static public Map<String, Collection<ConceptAggregate>> getRelations( final ConceptAggregate ConceptAggregate,
                                                                        final RelationDirection direction ) {
      final Map<String, Collection<ConceptAggregate>> relations = new HashMap<>();
      final Map<String, Collection<ConceptAggregate>> usedRelations = new HashMap<>();
      int wantedDepth = 1;
      Map<String, Collection<ConceptAggregate>> layerRelations
            = getLayerRelations( usedRelations, 1, wantedDepth, ConceptAggregate, direction );
      while ( !layerRelations.isEmpty() ) {
         for ( Map.Entry<String, Collection<ConceptAggregate>> entry : layerRelations.entrySet() ) {
            if ( !entry.getValue().isEmpty() ) {
               relations.computeIfAbsent( entry.getKey(), c -> new HashSet<>() ).addAll( entry.getValue() );
            }
         }
         wantedDepth++;
         layerRelations = getLayerRelations( usedRelations, 1, wantedDepth, ConceptAggregate, direction );
      }
      return relations;
   }

   static private Map<String, Collection<ConceptAggregate>> getLayerRelations(
         final Map<String, Collection<ConceptAggregate>> usedRelations,
         final int depth,
         final int wantedDepth,
         final ConceptAggregate ConceptAggregate,
         final RelationDirection direction ) {
      if ( ConceptAggregate == null ) {
         LOGGER.error( "Null Concept Instance" );
         return Collections.emptyMap();
      }
      final Map<String, Collection<ConceptAggregate>> relations = new HashMap<>();
      if ( depth == wantedDepth ) {
         if ( direction != RelationDirection.REVERSE ) {
            for ( Map.Entry<String, Collection<ConceptAggregate>> entry : ConceptAggregate.getRelatedConceptMap().entrySet() ) {
               final String type = entry.getKey();
               final Collection<ConceptAggregate> usedInstances = usedRelations
                     .computeIfAbsent( type, t -> new ArrayList<>() );
               for ( ConceptAggregate instance : entry.getValue() ) {
                  if ( !usedInstances.contains( ConceptAggregate ) || !usedInstances.contains( instance ) ) {
                     relations.computeIfAbsent( type, c -> new HashSet<>() ).add( instance );
                     usedInstances.add( ConceptAggregate );
                     usedInstances.add( instance );
                  }
               }
            }
         }
         return relations;
      }
      if ( direction != RelationDirection.REVERSE ) {
         for ( Collection<ConceptAggregate> instances : ConceptAggregate.getRelatedConceptMap().values() ) {
            for ( ConceptAggregate instance : instances ) {
               final Map<String, Collection<ConceptAggregate>> forwards
                     = getLayerRelations( usedRelations, depth + 1, wantedDepth, instance, direction );
               for ( Map.Entry<String, Collection<ConceptAggregate>> forward : forwards.entrySet() ) {
                  relations.computeIfAbsent( forward.getKey(), c -> new HashSet<>() ).addAll( forward.getValue() );
               }
            }
         }
      }
      return relations;
   }

   static public Map<String,String> getProperties( final ConceptAggregate instance ) {
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

   static private String getTemporality( final ConceptAggregate instance ) {
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
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                 .getGraph();
      if ( UriConstants.getCancerStages( graphDb ).contains( uri ) ) {
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
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                 .getGraph();
      if ( UriConstants.getCancerStages( graphDb ).contains( uri ) ) {
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
