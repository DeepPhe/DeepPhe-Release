package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.concept.instance.IdOwner;

/**
 * Wraps related ConceptInstances into a summary.
 * A summary typically represents a Dphe Cancer, Dphe primary tumor, Dphe metastatic tumor.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/5/2018
 */
public interface CiContainer extends IdOwner {


   String getType();

   String getUri();

   String getWorldId();

   /**
    * ANY summary can have a patient_site_etc id in common with another.
    * For instance, patientA and patientB have current tumors on the left breast.
    *
    * @return an index number unique to this summary
    */
   long getUniqueIdNum();



   /**
    * @return some unique id for this summary.
    */
   default String getId() {
      return getType() + "_" + getUri() + "_" + getWorldId() + "_" + getUniqueIdNum();
   }


}
