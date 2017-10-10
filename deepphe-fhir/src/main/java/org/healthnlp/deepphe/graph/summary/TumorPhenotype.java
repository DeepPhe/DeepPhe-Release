package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;

public class TumorPhenotype extends Summary {

   private FactList manifestations;
   private FactList histologicTypes;
   private FactList tumorExtent;

   public FactList getManifestations() {
      return manifestations;
   }

   public void setManifestations( FactList manifestations ) {
      this.manifestations = manifestations;
   }

   public FactList getHistologicTypes() {
      return histologicTypes;
   }

   public void setHistologicTypes( FactList histologicTypes ) {
      this.histologicTypes = histologicTypes;
   }

   public FactList getTumorExtent() {
      return tumorExtent;
   }

   public void setTumorExtent( FactList tumorExtent ) {
      this.tumorExtent = tumorExtent;
   }

   public String getResourceIdentifier() {
      return getClass().getSimpleName() + "_" + Math.abs( hashCode() );
   }

   public URI getConceptURI() {
      return FHIRConstants.TUMOR_PHENOTYPE_SUMMARY_URI;
   }

   public String getDisplayText() {
      return getClass().getSimpleName();
   }

}