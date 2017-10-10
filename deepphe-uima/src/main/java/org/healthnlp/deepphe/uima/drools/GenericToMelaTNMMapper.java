package org.healthnlp.deepphe.uima.drools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.healthnlp.deepphe.util.FHIRConstants;

public class GenericToMelaTNMMapper {

   public static Set<String> getMelaClassification( String prefix, String genericValue, List<String> suffixList ) {
      Set<String> tClassifSet = new HashSet<String>();
      String genV = genericValue.substring( 0, genericValue.indexOf( "_" ) );


      String pref = "c";
      if ( prefix.equals( FHIRConstants.P_MODIFIER ) )
         pref = "p";

      tClassifSet.add( "Cutaneous_Melanoma_" + pref + genV + "_TNM_Finding" );

      return tClassifSet;
   }

}
