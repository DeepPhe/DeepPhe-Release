package org.apache.ctakes.cancer.summary.writer;


import org.apache.ctakes.cancer.summary.NeoplasmCiContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.cancer.summary.NeoplasmCiContainer.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class TumorBsvWriter extends AbstractNeoplasmBsvWriter {


   static private final String TUMOR_EVAL_FILE = "Tumor_CI_Eval.bsv";

   static private final Collection<String> CANCER_TYPES = Arrays.asList(
         TYPE_PRIMARY,
         TYPE_METASTASIS,
         TYPE_GENERIC,
         TYPE_BENIGN );
   static private final List<String> PROPERTY_NAMES = Arrays.asList(
         HAS_CANCER_TYPE,
         HAS_HISTOLOGY,
         HAS_HISTORICITY );

   static private final List<String> RELATION_NAMES = Arrays.asList(
         DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
         HAS_LATERALITY,
         HAS_DIAGNOSIS,
         METASTASIS_OF,
         HAS_TUMOR_TYPE,
         HAS_TUMOR_EXTENT,
         HAS_METHOD,
         HAS_TREATMENT,
         DISEASE_HAS_NORMAL_TISSUE_ORIGIN,
         DISEASE_HAS_NORMAL_CELL_ORIGIN,
         HAS_SIZE,
         HAS_CALCIFICATION,
         HAS_ULCERATION,
         HAS_BRESLOW_DEPTH,
         HAS_QUADRANT,
         HAS_CLOCKFACE,
         HAS_ER_STATUS,
         HAS_PR_STATUS,
         HAS_HER2_STATUS );

   private String _cancerId;

   public TumorBsvWriter( final String outputDir ) {
      super( outputDir );
   }

   public void setCancerId( final String cancerId ) {
      _cancerId = cancerId;
   }

   protected String getIdHeaders() {
      return "Cancer_ID" + B + super.getIdHeaders();
   }

   protected String getIdValues( final String patientId, final NeoplasmCiContainer summary ) {
      return _cancerId + B + super.getIdValues( patientId, summary );
   }

   protected String getFileName() {
      return TUMOR_EVAL_FILE;
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
