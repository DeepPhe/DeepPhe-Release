package org.healthnlp.deepphe.neo4j.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {1/10/2024}
 */
public class RelatedUris {
   private final Map<String, Collection<String>> _relationTargets;
   private final Map<String, Integer> _targetOwnerDistances;

   RelatedUris( final Map<String, Collection<String>> relationTargets,
                final Map<String, Integer> targetOwnerDistances ) {
      _relationTargets = relationTargets;
      _targetOwnerDistances = targetOwnerDistances;
   }

   public boolean isEmpty() {
      return _relationTargets.isEmpty();
   }

   public Map<String, Collection<String>> getRelationTargets() {
      return _relationTargets;
   }

   public Map<String, Integer> getTargetOwnerDistances() {
      return _targetOwnerDistances;
   }

   public int getTargetOwnerDistance( final String target ) {
      return _targetOwnerDistances.getOrDefault( target, Integer.MAX_VALUE );
   }

   static public final RelatedUris EMPTY_URIS = new RelatedUris( Collections.emptyMap(), Collections.emptyMap() );
}
