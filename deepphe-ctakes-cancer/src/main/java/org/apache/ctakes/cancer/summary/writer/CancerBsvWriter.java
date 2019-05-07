package org.apache.ctakes.cancer.summary.writer;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.cancer.summary.NeoplasmCiContainer.TYPE_CANCER;
import static org.apache.ctakes.cancer.summary.NeoplasmCiContainer.TYPE_NON_CANCER;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class CancerBsvWriter extends AbstractNeoplasmBsvWriter {


   static private final String CANCER_EVAL_FILE = "Cancer_CI_Eval.bsv";


   static private final Collection<String> CANCER_TYPES = Arrays.asList(
         TYPE_CANCER,
         TYPE_NON_CANCER );
   static private final List<String> PROPERTY_NAMES = Arrays.asList(
         HAS_CANCER_TYPE,
         HAS_HISTOLOGY,
         HAS_HISTORICITY );

   static private final List<String> RELATION_NAMES = Arrays.asList(
         DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
         HAS_LATERALITY,
         HAS_STAGE,
         HAS_CLINICAL_T,
         HAS_CLINICAL_N,
         HAS_CLINICAL_M,
         HAS_PATHOLOGIC_T,
         HAS_PATHOLOGIC_N,
         HAS_PATHOLOGIC_M,
         HAS_METHOD,
         HAS_TREATMENT,
         REGIMEN_HAS_ACCEPTED_USE_FOR_DISEASE,
         DISEASE_HAS_NORMAL_TISSUE_ORIGIN,
         DISEASE_HAS_NORMAL_CELL_ORIGIN,
         DISEASE_HAS_FINDING,
         DISEASE_MAY_HAVE_FINDING,

         HAS_PSA_LEVEL,
         HAS_GLEASON_SCORE );

   public CancerBsvWriter( final String outputDir ) {
      super( outputDir );
   }

   protected String getFileName() {
      return CANCER_EVAL_FILE;
   }

   protected Collection<String> getSummaryTypes() {
      return CANCER_TYPES;
   }

   protected List<String> getPropertyNames() {
      return PROPERTY_NAMES;
   }

   protected List<String> getRelationNames() {
      return RELATION_NAMES;
   }

}
