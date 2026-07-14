package org.healthnlp.deepphe.nlp.attribute.tnm;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;

import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class TnmNormalizer extends DefaultXnAttributeNormalizer {

   public List<XnAttributeValue> getValues() {
      final List<XnAttributeValue> superList = super.getValues();
      return superList.stream().sorted( TNM_COMPARATOR ).collect( Collectors.toList() );
   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      if ( uri.isEmpty() || uri.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "";
      }
      String normal =  uri.replace( "StageFinding", "" )
                    .replace( "_lpn_i_add__rpn_", " (i+)" )
                .replace( "_lpn_mol_add__rpn_", " (mol+)" )
                .replace( "_lpn_i_sub__rpn_", " (i-)" )
            .replace( "_lpn_mol_sub__rpn_", " (mol-)" )
                .replace( "IV", "4" )
            .replace( "III", "3" )
            .replace( "II", "2" )
            .replace( "I", "1" );
      if ( normal.length() > 1 ) {
         final char pre = normal.charAt( 0 );
         if ( pre == 'C' || pre == 'P' ) {
            normal = normal.substring( 1 );
         }
      }
      if ( normal.length() > 1 ) {
         final char tnm = normal.charAt( 0 );
         if ( tnm == 'T' || tnm == 'N' || tnm == 'M' ) {
            normal = normal.substring( 1 );
         }
      }
      final char num = normal.charAt( 0 );
      if ( num == '0' || num == '1' || num == '2' || num == '3' || num == '4' ) {
         return normal.substring( 0,1 );
      }
      return normal;
   }


   static private final List<String> SORT_LIST = Arrays.asList(
         "", "0_lpn_i_add__rpn_", " (i+)", "0_lpn_mol_add__rpn_", " (m+)",
         "0i_lpn_i_sub__rpn_", " (i-)", "0_lpn_mol_sub__rpn_", " (mol+)",
         "a", "X", "0i", "is", "0",
         "1", "1mi", "1mic", "1a", "1a1", "1aI", "1a2", "1aII", "1b",
         "1b1", "1bI", "1b2", "1bII", "1b3", "1bIII", "1b4", "1bIV", "1bIVa", "1bIVb", "1bIVc", "1c", "1d",
         "2", "2a", "2a1", "2a2", "2b", "2c", "2d",
         "3", "3a", "3b", "3b1", "3b2", "3c", "3d",
         "4", "4a", "4a1", "4a2", "4b", "4c", "4d", "4e",
         "5"
         );

   static private final Comparator<XnAttributeValue> TNM_COMPARATOR
         = ( v1, v2 ) -> SORT_LIST.indexOf( v2.getValue() ) - SORT_LIST.indexOf( v1.getValue() );


}
