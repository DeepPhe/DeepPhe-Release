package org.healthnlp.deepphe.fhir.summary;

import java.net.URI;

import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

public class TumorPhenotype extends Summary {
   public FactList getManifestations() {
      return getFactsOrInsert( FHIRConstants.HAS_MANIFESTATION );
   }

   public FactList getHistologicTypes() {
      return getFactsOrInsert( FHIRConstants.HAS_HISTOLOGIC_TYPE );
   }

   public FactList getTumorExtent() {
      return getFactsOrInsert( FHIRConstants.HAS_TUMOR_EXTENT );
   }


   public URI getConceptURI() {
      return conceptURI != null ? conceptURI : FHIRConstants.TUMOR_PHENOTYPE_SUMMARY_URI;
   }

   public boolean isAppendable( Summary s ) {
      return s instanceof TumorPhenotype;
   }
}