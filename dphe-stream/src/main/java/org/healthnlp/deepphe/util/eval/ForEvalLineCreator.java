package org.healthnlp.deepphe.util.eval;

import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;

import java.util.Arrays;
import java.util.List;

final public class ForEvalLineCreator {

   private ForEvalLineCreator() {}
   static public String createBsv( final String patientId, final NeoplasmSummary summary,
                                   final List<String> attributeNames ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( patientId ).append( '|' );
      sb.append( summary.getId() ).append( '|' );
      final List<String> values = createAttributeList( summary, attributeNames );
      values.forEach( v -> sb.append( v ).append( '|' ) );
      sb.append( '\n' );
      return sb.toString();
   }

   static private List<String> createAttributeList( final NeoplasmSummary summary,
                                                    final List<String> attributeNames ) {
      final String[] values = new String[ attributeNames.size() ];
      Arrays.fill( values, "" );

      final List<NeoplasmAttribute> attributes = summary.getAttributes();
      for ( NeoplasmAttribute attribute : attributes ) {
         final int index = getAttributeIndex( attribute.getName(), attributeNames );
         if ( index < 0 ) {
            continue;
         }
         values[ index ] = attribute.getValue();
      }
      return Arrays.asList( values );
   }

   static private int getAttributeIndex( final String name, final List<String> attributeNames ) {
      int index = attributeNames.indexOf( name );
      if ( index >= 0 ) {
         return index;
      }
      index = attributeNames.indexOf( '-' + name );
      if ( index >= 0 ) {
         return index;
      }
      return attributeNames.indexOf( '*' + name );
   }

}
