package org.healthnlp.deepphe.nlp.attribute.stage;

import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class SpecialStageNormalizer extends DefaultXnAttributeNormalizer {


   public List<XnAttributeValue> getValues() {
      final List<XnAttributeValue> superList = super.getValues();
      return superList.stream().sorted( STAGE_COMPARATOR ).collect( Collectors.toList() );
   }

   static private final Map<String,String> EXACT_URIS = new HashMap<>();
   static {
      // Occult Stage is special for Lung Cancer
      EXACT_URIS.put( "OccultStage", "Occult" );
      // Stage 1E is special for Non-Hodgkin Lymphoma
      EXACT_URIS.put( "StageIE", "IE" );
      // Stage 2E is special for Non-Hodgkin Lymphoma
      EXACT_URIS.put( "StageIIE", "IIE" );
      // Stage 3D is special for Melanoma
      EXACT_URIS.put( "StageIIID", "IIID" );
      // stage 4s is special for neuroblastoma
      EXACT_URIS.put( "StageIVS", "IVS" );
      // Stage 5 is for Kidney tumors (Wilms)
      EXACT_URIS.put( "StageV", "V" );
      // Stages A, B, C, D are for Liver Cancer
      EXACT_URIS.put( "StageA", "A" );
      EXACT_URIS.put( "StageB", "B" );
      EXACT_URIS.put( "StageB1", "B" );
      EXACT_URIS.put( "StageB2", "B" );
      EXACT_URIS.put( "StageC", "C" );
      EXACT_URIS.put( "StageD", "D" );
      EXACT_URIS.put( "StageD1", "D" );
      EXACT_URIS.put( "StageD2", "D" );
      // Small cell lung cancer
      EXACT_URIS.put( "LimitedStage", "Limited" );
      EXACT_URIS.put( "ExtensiveStage", "Extensive" );

      // I think that this is actually a TNM M2
//      EXACT_URIS.put( "StageM2", "M2" );
      // Stage R values are used in clinical trial design and are not in summary stage 1-4 scale
//      EXACT_URIS.put( "StageR", "R" );
//      EXACT_URIS.put( "StageR0", "R0" );
//      EXACT_URIS.put( "StageR1", "R1" );
//      EXACT_URIS.put( "StageR2", "R2" );
//      EXACT_URIS.put( "StageRX", "RX" );
   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      return EXACT_URIS.getOrDefault( uri, "" );
   }

   static private final List<String> SORT_LIST = Arrays.asList(
         "", "Occult", "IE", "IIE", "IIID", "IVS", "V", "A", "B", "C", "D", "Limited", "Extensive" );


   static private final Comparator<XnAttributeValue> STAGE_COMPARATOR
         = ( v1, v2 ) -> SORT_LIST.indexOf( v2.getValue() ) - SORT_LIST.indexOf( v1.getValue() );

}
