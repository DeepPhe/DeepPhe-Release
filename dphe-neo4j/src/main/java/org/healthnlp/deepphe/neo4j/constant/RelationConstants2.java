package org.healthnlp.deepphe.neo4j.constant;

import java.util.Arrays;
import java.util.Collection;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
final public class RelationConstants2 {

   private RelationConstants2() {}

   static public final Collection<String> REQUIRED_RELATIONS = Arrays.asList(
         HAS_SITE, HAS_ASSOCIATED_SITE, HAS_METASTATIC_SITE,
         HAS_PROCEDURE, HAS_TREATMENT, HAS_FINDING, HAS_TEST_RESULT,
         HAS_TISSUE, HAS_GENE, HAS_MASS,
         HAS_LATERALITY, HAS_QUADRANT, HAS_CLOCKFACE,
         HAS_STAGE_XN, HAS_GRADE_XN, HAS_GLEASON_GRADE,
         HAS_CLINICAL_T_XN, HAS_CLINICAL_N_XN, HAS_CLINICAL_M_XN,
         HAS_PATHOLOGIC_T_XN, HAS_PATHOLOGIC_N_XN, HAS_PATHOLOGIC_M_XN,
         HAS_BEHAVIOR, HAS_COURSE, HAS_LYMPH_NODE, HAS_COMORBIDITY );


   static public boolean isRequiredRelation( final String relation ) {
      return REQUIRED_RELATIONS.contains( relation ) || RelationConstants.REQUIRED_RELATIONS.contains( relation );
   }
}
