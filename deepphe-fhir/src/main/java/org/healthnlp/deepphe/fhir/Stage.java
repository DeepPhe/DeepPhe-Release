package org.healthnlp.deepphe.fhir;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.healthnlp.deepphe.util.FHIRRegistry;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Condition.ConditionStageComponent;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.StringType;

public class Stage extends ConditionStageComponent implements Serializable {
   public static final String TNM_PRIMARY_TUMOR = FHIRUtils.STAGE_URL + "/PrimaryTumor";
   public static final String TNM_DISTANT_METASTASIS = FHIRUtils.STAGE_URL + "/DistantMetastasis";
   public static final String TNM_REGIONAL_LYMPH_NODES = FHIRUtils.STAGE_URL + "/RegionalLymphNodes";

   private static final String TNM_SUFFIX = "Stage_Finding";

   public void setStringExtension( String url, String value ) {
      Extension e = new Extension();
      e.setUrl( url );
      e.setValue( new StringType( value ) );
      addExtension( e );

   }

   /**
    * get primary tumor stage
    *
    * @return
    */
   public String getPrimaryTumorStage() {
      Extension e = getExtension( TNM_PRIMARY_TUMOR ); //FHIRUtils.CANCER_URL+"#"+FHIRUtils.T_STAGE
      return e != null ? ((StringType) e.getValue()).getValue() : null;
   }

   private Extension getExtension( String url ) {
      List<Extension> list = getExtensionsByUrl( url );
      return list.isEmpty() ? null : list.get( 0 );
   }

   /**
    * get primary tumor stage
    *
    * @return
    */
   public CodeableConcept getPrimaryTumorStageCode() {
      return getStageValue( TNM_PRIMARY_TUMOR );
   }


   /**
    * get primary tumor stage
    *
    * @return
    */
   public CodeableConcept getDistantMetastasisStageCode() {
      return getStageValue( TNM_DISTANT_METASTASIS );
   }

   /**
    * get primary tumor stage
    *
    * @return
    */
   public CodeableConcept getRegionalLymphNodeStageCode() {
      return getStageValue( TNM_REGIONAL_LYMPH_NODES );
   }


   private CodeableConcept getStageValue( String stage ) {
      Extension e = getExtension( stage );
      if ( e == null )
         return null;
      String val = ((StringType) e.getValue()).getValue();
      if ( val != null ) {
         CodeableConcept cc = FHIRUtils.getCodeableConcept( URI.create( val ) );
         for ( Reference r : getAssessment() ) {
            if ( val.endsWith( r.getDisplay() ) ) {
               FHIRUtils.addResourceReference( cc, r.getDisplay(), r.getReference() );
            }
         }
         return cc;
      }
      return null;
   }

   /**
    * add assessment to
    *
    * @param f
    */
   public void addAssessment( Finding f ) {
      addAssessment( FHIRUtils.getResourceReference( f ) );
      getAssessmentTarget().add( f );

      //infer TNM
      CodeableConcept c = f.getCode();
      if ( c.getText() != null && c.getText().endsWith( TNM_SUFFIX ) ) {
         if ( c.getText().startsWith( "T" ) ) {
            setStringExtension( Stage.TNM_PRIMARY_TUMOR, "" + FHIRUtils.getConceptURI( c ) );
         } else if ( c.getText().startsWith( "N" ) ) {
            setStringExtension( Stage.TNM_REGIONAL_LYMPH_NODES, "" + FHIRUtils.getConceptURI( c ) );
         } else if ( c.getText().startsWith( "M" ) ) {
            setStringExtension( Stage.TNM_DISTANT_METASTASIS, "" + FHIRUtils.getConceptURI( c ) );
         }
      }

   }

   /**
    * get primary tumor stage
    *
    * @return
    */
   public String getDistantMetastasisStage() {
      Extension e = getExtension( TNM_DISTANT_METASTASIS );
      return e != null ? ((StringType) e.getValue()).getValue() : null;
   }

   /**
    * get primary tumor stage
    *
    * @return
    */
   public String getRegionalLymphNodeStage() {
      Extension e = getExtension( TNM_REGIONAL_LYMPH_NODES );
      return e != null ? ((StringType) e.getValue()).getValue() : null;
   }

   public Stage copy() {
      Stage dst = new Stage();
      dst.summary = ((this.summary == null) ? null : this.summary.copy());
      dst.assessment = new ArrayList();
      for ( Reference i : this.assessment )
         dst.assessment.add( i.copy() );
      for ( Extension e : getExtension() ) {
         dst.setStringExtension( e.getUrl(), ((StringType) e.getValue()).asStringValue() );
      }

      return dst;
   }

   public List<Resource> getAssessmentTarget() {
      // if list of targets is empty, check the references
      if ( super.getAssessmentTarget().isEmpty() && !getAssessment().isEmpty() ) {
         for ( Reference r : getAssessment() ) {
            Element e = FHIRRegistry.getInstance().getElement( r.getReference() );
            if ( e != null ) {
               super.getAssessmentTarget().add( (Resource) e );
            }
         }
      }
      return super.getAssessmentTarget();
   }


   public void copy( ConditionStageComponent st ) {
      setSummary( st.getSummary() );
      for ( Reference r : st.getAssessment() ) {
         addAssessment( r );
      }
      for ( Extension e : st.getExtension() ) {
         setStringExtension( e.getUrl(), ((StringType) e.getValue()).asStringValue() );
      }
   }

   public String getDisplayText() {
      CodeableConcept c = getSummary();
      return c != null ? c.getText() : "TNM unknown";
   }

   public String toString() {
      return getDisplayText();
   }
}
