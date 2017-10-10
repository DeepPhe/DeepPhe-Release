package org.healthnlp.deepphe.fhir;

import java.io.File;
import java.io.Serializable;
import java.net.URI;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Resource;


/**
 * DeepPhe model interface that
 *
 * @author tseytlin
 */
public interface Element extends Serializable {
   public String getDisplayText();

   public String getResourceIdentifier();

   public String getSummaryText();

   public Resource getResource();

   public CodeableConcept getCode();

   //public IClass getConceptClass();
   public URI getConceptURI();

   public void setComposition( Report r );

   public Report getComposition();

   public void save( File e ) throws Exception;

   public void copy( Resource r );

   public String getAnnotationType();
}
