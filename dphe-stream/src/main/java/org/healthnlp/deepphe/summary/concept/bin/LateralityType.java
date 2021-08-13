package org.healthnlp.deepphe.summary.concept.bin;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {6/13/2021}
 */
public enum LateralityType {
   LEFT( UriConstants.LEFT ),
   RIGHT( UriConstants.RIGHT ),
   BILATERAL( UriConstants.BILATERAL ),
   NO_LATERALITY( "NO_LATERALITY" );

   final String _uri;
   LateralityType( final String uri ) {
      _uri = uri;
   }

   boolean hasLateralityType( final Map<String,Collection<Mention>> relations ) {
      return hasLateralityType( relations.get( RelationConstants.HAS_LATERALITY ) );
   }

   boolean hasLateralityType( final Collection<Mention> lateralities ) {
      if ( lateralities == null || lateralities.isEmpty() ) {
         return this == NO_LATERALITY;
      }
      return lateralities.stream()
                         .map( Mention::getClassUri )
                         .anyMatch( _uri::equals );
   }

   static boolean isNoLateralityType( final Map<String,Collection<Mention>> relations ) {
      return NO_LATERALITY.hasLateralityType( relations );
   }

   static Collection<LateralityType> getLateralityTypes( final Map<String,Collection<Mention>> relations ) {
      if ( relations == null || relations.isEmpty() ) {
         return Collections.singletonList( NO_LATERALITY );
      }
      final Collection<Mention> lateralities = relations.get( RelationConstants.HAS_LATERALITY );
      if ( lateralities == null || lateralities.isEmpty() ) {
         return Collections.singletonList( NO_LATERALITY );
      }
      final Collection<LateralityType> lateralityTypes = new HashSet<>();
      if ( LEFT.hasLateralityType( lateralities ) ) {
         lateralityTypes.add( LEFT );
      }
      if ( RIGHT.hasLateralityType( lateralities ) ) {
         lateralityTypes.add( RIGHT );
      }
      if ( BILATERAL.hasLateralityType( lateralities ) ) {
         lateralityTypes.add( BILATERAL );
      }
      return lateralityTypes;
   }


}
