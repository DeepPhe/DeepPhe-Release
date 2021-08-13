package org.healthnlp.deepphe.util.eval.old.eval;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final class NeoplasmSummary implements IdOwner {
   private final String _id;
   private final Map<String, String> _requiredMap = new HashMap<>();
   private final Map<String, String> _scoringMap = new HashMap<>();

   NeoplasmSummary( final String id,
                    final Map<String, String> requiredMap,
                    final Map<String, String> scoringMap ) {
      _id = id;
      _requiredMap.putAll( requiredMap );
      _scoringMap.putAll( scoringMap );
      mergeAlikeAttributes();
   }

   public String getId() {
      return _id;
   }

   Map<String, String> getRequiredAttributes() {
      return _requiredMap;
   }

   Map<String, String> getScoringAttributes() {
      return _scoringMap;
   }

   String getAttribute( final String name ) {
      final String required = _requiredMap.get( name );
      if ( required != null ) {
         return required.trim();
      }
      final String scoring = _scoringMap.get( name );
      return scoring == null ? "" : scoring.trim();
   }

   private void mergeAlikeAttributes() {
      mergeAlikeAttributes( HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
      mergeAlikeAttributes( HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      mergeAlikeAttributes( HAS_PATHOLOGIC_M, HAS_CLINICAL_M );
      mergeAttributeNames( _scoringMap.keySet() );
   }

   static private void mergeAttributeNames( final Collection<String> names ) {
      names.remove( HAS_PATHOLOGIC_T );
      names.remove( HAS_PATHOLOGIC_N );
      names.remove( HAS_PATHOLOGIC_M );
   }

   private void mergeAlikeAttributes( final String toMerge, final String toUse ) {
      final String pt = _scoringMap.get( toMerge );
      if ( pt != null && !pt.isEmpty() ) {
         String ct = _scoringMap.computeIfAbsent( toUse, String::new );
         _scoringMap.put( toUse, ct + ";" + pt );
      }
   }

   public boolean equals( final Object other ) {
      return other instanceof NeoplasmSummary && hashCode() == other.hashCode();
   }

   public int hashCode() {
      return _id.hashCode();
   }
}
