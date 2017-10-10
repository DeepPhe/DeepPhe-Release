package org.healthnlp.deepphe.fhir;

import org.healthnlp.deepphe.util.FHIRUtils;

public class Finding extends Condition {

   public Finding() {
      setCategory( FHIRUtils.CONDITION_CATEGORY_FINDING );
      setLanguage( FHIRUtils.DEFAULT_LANGUAGE ); // we only care about English
      setVerificationStatus( ConditionVerificationStatus.CONFIRMED );
   }

}
