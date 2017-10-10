package org.healthnlp.deepphe.fhir;

import org.healthnlp.deepphe.util.FHIRUtils;

/**
 * This class represents a diagnosis mention in a report
 *
 * @author tseytlin
 */
public class Disease extends Condition {

   public Disease() {
      setCategory( FHIRUtils.CONDITION_CATEGORY_DIAGNOSIS );
      setLanguage( FHIRUtils.DEFAULT_LANGUAGE ); // we only care about English

      //setClinicalStatus("active"); // here we only deal with 'confirmed' dx
      setVerificationStatus( ConditionVerificationStatus.CONFIRMED ); //?????
   }
}
