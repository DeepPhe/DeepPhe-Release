package org.apache.ctakes.cancer.concept.instance;

import org.apache.log4j.Logger;

import java.util.*;

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

   private ConceptInstanceUtil() {
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
         if ( direction != RelationDirection.FORWARD ) {
            for ( Map.Entry<String, Collection<ConceptInstance>> entry : conceptInstance.getReverseRelated()
                                                                                        .entrySet() ) {
               final String type = entry.getKey();
               final Collection<ConceptInstance> usedInstances = usedRelations
                     .computeIfAbsent( type, t -> new ArrayList<>() );
               for ( ConceptInstance instance : entry.getValue() ) {
                  if ( !usedInstances.contains( conceptInstance ) || !usedInstances.contains( instance ) ) {
                     relations.computeIfAbsent( type, c -> new HashSet<>() ).add( instance );
                     LOGGER.info( depth + " < " + type + " " + conceptInstance.getPreferredText() + " " +
                                  instance.getPreferredText() + " " + usedInstances.contains( conceptInstance ) + " " +
                                  usedInstances.contains( instance ) );
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
      if ( direction != RelationDirection.FORWARD ) {
         for ( Collection<ConceptInstance> instances : conceptInstance.getReverseRelated().values() ) {
            for ( ConceptInstance instance : instances ) {
               final Map<String, Collection<ConceptInstance>> reverses
                     = getLayerRelations( usedRelations, depth + 1, wantedDepth, instance, direction );
               for ( Map.Entry<String, Collection<ConceptInstance>> reverse : reverses.entrySet() ) {
                  relations.computeIfAbsent( reverse.getKey(), c -> new HashSet<>() ).addAll( reverse.getValue() );
               }
            }
         }
      }
      return relations;
   }

}
