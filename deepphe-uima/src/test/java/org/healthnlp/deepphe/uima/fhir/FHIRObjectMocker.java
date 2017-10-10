package org.healthnlp.deepphe.uima.fhir;

import org.healthnlp.deepphe.fhir.*;
import org.healthnlp.deepphe.fhir.fact.BodySiteFact;
import org.healthnlp.deepphe.fhir.fact.DefaultFactList;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.fhir.summary.*;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class FHIRObjectMocker {
   private Mockery context = new JUnit4Mockery() {{
      setImposteriser( ClassImposteriser.INSTANCE );
   }};
   final Report report1 = context.mock( Report.class, "report1" );
   final Report report2 = context.mock( Report.class, "report2" );

   final Patient patient = context.mock( Patient.class );
   final CancerSummary cancerSummary = context.mock( CancerSummary.class );
   final TumorSummary tumorSummary = context.mock( TumorSummary.class );
   //	final PatientSummary patientSummary = context.mock(PatientSummary.class);
//	final PatientPhenotype patientPhenotype = context.mock(PatientPhenotype.class);
   final CancerPhenotype cancerPhenotype = context.mock( CancerPhenotype.class );
   final TumorPhenotype tumorPhenotype = context.mock( TumorPhenotype.class );
   final MedicalRecord medicalRecord = context.mock( MedicalRecord.class );

   public FHIRObjectMocker() {
      super();
      init();
   }


   private void init() {

      final Fact leftModifierFact = new Fact();
      leftModifierFact.setIdentifier( "body_side_left_1" );
      leftModifierFact.setName( "left" );

      final BodySiteFact breastBodySiteFact = new BodySiteFact();
      breastBodySiteFact.setIdentifier( "body_site_Breast_1" );
      breastBodySiteFact.setName( "Breast" );
      breastBodySiteFact.setType( FHIRConstants.BODY_SITE );
      breastBodySiteFact.setModifiers( new DefaultFactList( leftModifierFact ) );

      final Fact cStageFindingFact = new Fact();
      cStageFindingFact.setIdentifier( "stage_Stage_IIA_Breast_Cancer_1" );
      cStageFindingFact.setName( "Stage_IIA_Breast_Cancer" );
      cStageFindingFact.setType( FHIRConstants.FINDING );

      final Fact diagnosisFact = new Fact();
      diagnosisFact.setIdentifier( "diagnosis_DCIS_1" );
      diagnosisFact.setName( "Invasive_Ductal_Carcinoma_Not_Otherwise_Specified; Ductal_Breast_Carcinoma_In_Situ" );
      diagnosisFact.setType( FHIRConstants.DIAGNOSIS );

      final Fact tumorTypeFact = new Fact();
      tumorTypeFact.setIdentifier( "tumor_type_PrimaryTumor_1" );
      tumorTypeFact.setName( "PrimaryTumor" );

      final Fact histologicTypeFact = new Fact();
      histologicTypeFact.setIdentifier( "histologic_type_Ductal_1" );
      histologicTypeFact.setName( "Ductal" );

      final Fact tumorExtentFact = new Fact();
      tumorExtentFact.setIdentifier( "tumor_extent_in_situ_1" );
      tumorExtentFact.setName( "In_Situ_Lesion" );

      context.checking( new Expectations() {
         {
            //Patient
            allowing( patient ).getPatientName();
            will( returnValue( "Patient Frankenstein" ) );


            //Reports
            allowing( report1 ).getResourceIdentifier();
            will( returnValue( "Pathology_Report_01" ) );

            allowing( report1 ).getTitle();
            will( returnValue( "Pathology Report" ) );

            allowing( report1 ).getReportText();
            will( returnValue( "This is a mock pathology report" ) );


            allowing( report2 ).getResourceIdentifier();
            will( returnValue( "Radiology_Report_01" ) );

            allowing( report2 ).getTitle();
            will( returnValue( "Radiology Report" ) );

            allowing( report2 ).getReportText();
            will( returnValue( "This is a mock radiology report" ) );


            //Medical Record
            allowing( medicalRecord ).getSummaryText();
            will( returnValue( "Medical Record Summary\nThis is where the summary appears.\n\n This one is mocked." ) );

            allowing( medicalRecord ).getPatient();
            will( returnValue( patient ) );
//
//				allowing(medicalRecord).getPatientSummary();
//				will(returnValue(patientSummary));

            allowing( medicalRecord ).getCancerSummary();
            will( returnValue( cancerSummary ) );

            allowing( medicalRecord ).getReports();
            will( returnValue( Arrays.asList( new Report[]{ report1, report2 } ) ) );


            //Cancer Summary
            allowing( cancerSummary ).getResourceIdentifier();
            will( returnValue( "cancer_summary_1" ) );

            allowing( cancerSummary ).getSummaryText();
            will( returnValue( "This is the cancer summary" ) );

            allowing( cancerSummary ).getFactCategories();
            will( returnValue( new HashSet( Arrays.asList( new String[]{ FHIRConstants.HAS_BODY_SITE, FHIRConstants.HAS_DIAGNOSIS } ) ) ) );
            allowing( cancerSummary ).getFacts( FHIRConstants.HAS_BODY_SITE );
            will( returnValue( new DefaultFactList( breastBodySiteFact ) ) );
            allowing( cancerSummary ).getFacts( FHIRConstants.HAS_DIAGNOSIS );
            will( returnValue( new DefaultFactList( diagnosisFact ) ) );

            allowing( cancerSummary ).getPhenotype();
            will( returnValue( cancerPhenotype ) );

            allowing( cancerSummary ).getTumors();
            will( returnValue( Collections.singletonList( tumorSummary ) ) );


            //Cancer Phenotype
            allowing( cancerPhenotype ).getResourceIdentifier();
            will( returnValue( "cancerPhenotype_1" ) );

            allowing( cancerPhenotype ).getSummaryText();
            will( returnValue( "This is the cancer phenotype summary" ) );

            allowing( cancerPhenotype ).getFactCategories();
            will( returnValue( new HashSet( Arrays.asList( new String[]{ FHIRConstants.HAS_CANCER_STAGE } ) ) ) );
            allowing( cancerPhenotype ).getFacts( FHIRConstants.HAS_CANCER_STAGE );
            will( returnValue( new DefaultFactList( cStageFindingFact ) ) );


            //Tumor Summary
            allowing( tumorSummary ).getResourceIdentifier();
            will( returnValue( "tumorSummary_1" ) );

            allowing( tumorSummary ).getSummaryText();
            will( returnValue( "This is the tumor summary" ) );

            allowing( tumorSummary ).getFactCategories();
            will( returnValue( new HashSet( Arrays.asList( new String[]{ FHIRConstants.HAS_BODY_SITE, FHIRConstants.HAS_TUMOR_TYPE, FHIRConstants.HAS_DIAGNOSIS } ) ) ) );
            allowing( tumorSummary ).getFacts( FHIRConstants.HAS_BODY_SITE );
            will( returnValue( new DefaultFactList( breastBodySiteFact ) ) );
            allowing( tumorSummary ).getFacts( FHIRConstants.HAS_TUMOR_TYPE );
            will( returnValue( new DefaultFactList( tumorTypeFact ) ) );
            allowing( tumorSummary ).getFacts( FHIRConstants.HAS_DIAGNOSIS );
            will( returnValue( new DefaultFactList( diagnosisFact ) ) );

            allowing( tumorSummary ).getPhenotype();
            will( returnValue( tumorPhenotype ) );


            //Tumor Phenotype
            allowing( tumorPhenotype ).getResourceIdentifier();
            will( returnValue( "tumorPhenotype_1" ) );

            allowing( tumorPhenotype ).getSummaryText();
            will( returnValue( "This is the tumor Phenotype" ) );

            allowing( tumorPhenotype ).getFactCategories();
            will( returnValue( new HashSet( Arrays.asList( new String[]{ FHIRConstants.HAS_HISTOLOGIC_TYPE, FHIRConstants.HAS_TUMOR_EXTENT } ) ) ) );
            allowing( tumorPhenotype ).getFacts( FHIRConstants.HAS_HISTOLOGIC_TYPE );
            will( returnValue( new DefaultFactList( histologicTypeFact ) ) );
            allowing( tumorPhenotype ).getFacts( FHIRConstants.HAS_TUMOR_EXTENT );
            will( returnValue( new DefaultFactList( tumorExtentFact ) ) );


//				//Patient Summary
//				allowing(patientSummary).getSummaryText();
//				will(returnValue("This is the patient summary"));
//
//				allowing(patientSummary).getPhenotype();
//				will(returnValue(patientPhenotype));
//
//
//				//Patient Phenotype
//				allowing(patientPhenotype).getSummaryText();
//				will(returnValue("This is the Patient Phenotype"));

         }
      } );
   }


   public MedicalRecord getMedicalRecord() {
      return medicalRecord;
   }

}
