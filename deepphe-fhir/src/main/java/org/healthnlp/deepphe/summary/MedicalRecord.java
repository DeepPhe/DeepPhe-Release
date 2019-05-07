package org.healthnlp.deepphe.summary;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;
import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * medical record for a given Patient
 *
 * @author tseytlin
 */
public class MedicalRecord implements DpheElement {

    static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private PatientSummary patientSummary;
   private final List<CancerSummary> _cancerSummaries = new ArrayList<>();
    private Collection<TumorSummary> tumorSummaries = new ArrayList<TumorSummary>();
    // TODO Patients can have more than one cancer summary.
    private Collection<EpisodeSummary> _refinedEpisodeSummaries;
    private Collection<CancerSummary> _refinedCancerSummaries;
    private Collection<NoteSummary> _reports;
    private String _patientName;
    private String _patientId;
    private String _simplePatientName;

   // TODO Consider using more than just the name for patient identifier.  Perhaps [name]_[patientNum]
   public MedicalRecord( final String patientId ) {
      _patientId = patientId;
      _patientName = patientId;
      _simplePatientName = patientId;
      patientSummary = new PatientSummary( patientId );
      patientSummary.setPatientIdentifier( patientId );
   }

    public void addNote( final JCas jCas, final boolean forDrools ) {
        addNoteSummary( new NoteSummary( jCas, forDrools ) );
    }

    public String getPatientName() {
        return _patientName == null ? "Generic" : _patientName;
    }

    public String getSimplePatientName() {
        return _simplePatientName;
    }

    public String getDisplayText() {
        return getPatientName() + " Medical Record";
    }

    public String getResourceIdentifier() {
        return getDisplayText();
    }

    public String toString() {
        return getDisplayText();
    }

    public String getSummaryText() {
        final StringBuilder sb = new StringBuilder(getDisplayText());
        sb.append("\n==========================\n");
        if (patientSummary != null) {
            sb.append(patientSummary.getSummaryText()).append("\n");
        }
       _cancerSummaries.forEach( s ->  sb.append( s.getSummaryText() ).append( "\n" ) );
        return sb.toString();
    }

    public String getPatientIdentifier() {
        return _patientId;
    }

    public String getConceptURI() {
        return FHIRConstants.MEDICAL_RECORD_URI;
    }

    public String getAnnotationType() {
        return FHIRConstants.ANNOTATION_TYPE_RECORD;
    }

    public PatientSummary getPatientSummary() {
        return patientSummary;
    }

    /**
     * Link the patient summary to the medical record.
     * and add all facts from all notes to the patient summary.
     *
     * @param patientSummary -
     */
    public void setPatientSummary(final PatientSummary patientSummary) {
        this.patientSummary = patientSummary;
        copyAllNoteFacts(patientSummary);  // copy all facts from all notes (NoteSummary's) to the patient summary
    }

    /**
     * Link the patient summary to the medical record.
     *
     * @param patientSummary -
     */
    public void setPatientSummaryWithoutFacts(final PatientSummary patientSummary) {
        this.patientSummary = patientSummary;
    }

    @Deprecated
    public CancerSummary getCancerSummary() {
       if ( _cancerSummaries.isEmpty() ) {
          return null;
       }
       return _cancerSummaries.get( 0 );
    }

    public Collection<CancerSummary> getCancerSummaries() {
       return _cancerSummaries;
    }

    /**
     * link a cancer summary for Drools to use. Do not add any facts to the cancer summary
     * @param cancerSummary -
     */
    @Deprecated
    public void setCancerSummaryWithoutFacts(CancerSummary cancerSummary) {
       addCancerSummaryWithoutFacts( cancerSummary );
    }

    @Deprecated
    public void setCancerSummary(CancerSummary cancerSummary) {
       addCancerSummary( cancerSummary );
    }

   /**
    * link a cancer summary for Drools to use. Do not add any facts to the cancer summary
    * @param cancerSummary -
    */
   public void addCancerSummaryWithoutFacts( final CancerSummary cancerSummary ) {
      _cancerSummaries.add( cancerSummary );
   }

   public void addCancerSummary( final CancerSummary cancerSummary ) {
      _cancerSummaries.add( cancerSummary );
      copyAllNoteFacts( cancerSummary );  // copy all facts from all notes (NoteSummary's) to the cancer summary
   }


   /**
     * Add just to the medical record, not to any CancerSummary
     *
     * @param summary
     */
    public void addTumorSummary(TumorSummary summary) {
        tumorSummaries.add(summary);
    }


    /**
     * Each NoteSummary potentially has TumorSummary(s), add each of those to the medicalRecord
     */
    public void addTumorSummariesFromNoteSummaries() {

        for (NoteSummary noteSummary : getNoteSummaries()) {
            for (TumorSummary tumorSummary : noteSummary.getTumorSummaries()) {

                this.addTumorSummary(tumorSummary);

                Collection<String> categories = tumorSummary.getFactCategories();
                if (categories != null) {
                    for (String category : categories) {
                        for (Fact f : tumorSummary.getFacts(category)) {
                            tumorSummary.tagAsTumorSummaryFact(f);
                            for (Fact cf : f.getContainedFacts()) {  // Changed getContainedFacts to actually return Facts from getRelatedFacts
                                tumorSummary.tagAsTumorSummaryFact(cf);
                            }
                        }
                    }
                }

            }
        }

    }

//    public void moveCancerFactToTumor(NoteSummary noteSummary, Fact f) {
//        if (f.getSummaryType() == null) {
//            LOGGER.warn("Found a fact with " + f.getSummaryType() + " for Summary Type. Unexpected.");
//        } else if (CancerSummary.class.getSimpleName().equals(f.getSummaryType())){
//            LOGGER.warn("Found a fact with " + f.getSummaryType() + " for Summary Type. No longer expect this");
//        } else if (PatientSummary.class.getSimpleName().equals(f.getSummaryType())){
//            LOGGER.warn("Found a fact with " + f.getSummaryType() + " for Summary Type. No longer expect this");
//        }
//        if (f.getSummaryType() == null || CancerSummary.class.getSimpleName().equals(f.getSummaryType())) {
//
//            if (noteSummary.getTumorSummaries() == null || noteSummary.getTumorSummaries().isEmpty()) {
//                LOGGER.error("No TumorSummary for NoteSummary: " + noteSummary.getId() + "  NoteSummary details: " + noteSummary);
//                return;
//            }
//
//            for (TumorSummary tumorSummary: noteSummary.getTumorSummaries()){
//                cancerSummary.addTumorWithoutAddingFacts(tumorSummary); // Drools uses medicalRecord.getCancerSummary().getTumorSummaryByIdentifier(tsId) which needs cancerSummary.getTumors() to show this tumor
//                noteSummary.addTumorSummary(tumorSummary);
//                this.addTumorSummary(tumorSummary);
//                tumorSummary.tagAsTumorSummaryFact(f); // doesn't change value if already set
//            }
//        }
//    }
//
//    public void moveCancerFactsToTumors() {
//        for (NoteSummary noteSummary : getNoteSummaries()) {
//            Map<String, FactList> content = noteSummary.getContent();
//            for (Map.Entry<String, FactList> entry : content.entrySet()) {
//                FactList factlist = entry.getValue();
//                for (Fact fact : factlist) {
//                    moveCancerFactToTumor(noteSummary, fact);
//                    for (Fact cf : fact.getContainedFacts()) {
//                        moveCancerFactToTumor(noteSummary, cf);
//                    }
//                }
//            }
//        }
//    }

   public Collection<NoteSummary> getNoteSummaries() {
      if ( _reports == null ) {
         _reports = new ArrayList<>();
      }
      return _reports;
   }

   public void addNoteSummary( final NoteSummary noteSummary ) {
       LOGGER.info( "Added summaries for " + noteSummary.getNoteSpecs().getDocumentId() );
      getNoteSummaries().add( noteSummary );
      if ( _patientName == null ) {
         _patientName = noteSummary.getPatientIdentifier();
      }
      if ( _patientId == null ) {
         _patientId = noteSummary.getPatientIdentifier();
      }
      if ( _simplePatientName == null ) {
         _simplePatientName = noteSummary.getPatientIdentifier();
      }
      patientSummary.addNoteSpecs( noteSummary.getNoteSpecs() );
   }

   /**
    * @return all facts that are contained within report level summaries
    */
   public List<Fact> getReportLevelFacts() {
      return getNoteSummaries().stream()
                               .map( NoteSummary::getContainedFacts )
                               .flatMap( Collection::stream )
                               .sorted( FactFactory.FACT_SORTER )
                               .collect( Collectors.toList() );
   }

   private void copyAllNoteFacts( final Summary summary ) {
      for ( NoteSummary noteSummary : getNoteSummaries() ) {
         for ( Map.Entry<String, FactList> content : noteSummary.getContent().entrySet() ) {
            final String category = content.getKey();
            content.getValue().forEach( f -> summary.addFact( category, f ) );
         }
      }
   }

}
