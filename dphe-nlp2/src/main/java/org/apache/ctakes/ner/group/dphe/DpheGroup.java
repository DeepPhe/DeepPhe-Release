package org.apache.ctakes.ner.group.dphe;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.ner.group.Group;
import org.apache.ctakes.ner.group.NameOwner;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.*;
import java.util.function.Function;

import static org.apache.ctakes.core.util.annotation.SemanticTui.*;

/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public enum DpheGroup implements Group<DpheGroup> {
   UNKNOWN( "Unknown", SemanticTui.UNKNOWN, Event.class, Event::new ),
   FINDING( "Finding", T033, SignSymptom.class, SignSymptom::new ),
   IMAGING_DEVICE( "Imaging Device", T074, ProcedureDevice.class, ProcedureDevice::new ),
   PROPERTY_OR_ATTRIBUTE( "Property or Attribute", T077, Attribute.class, Attribute::new ),
   DISEASE_QUALIFIER( "Disease Qualifier", T034, Attribute.class, Attribute::new ),
   GENERAL_QUALIFIER( "General Qualifier", T034, Attribute.class, Attribute::new ),
   SPATIAL_QUALIFIER( "Spatial Qualifier", T082, Attribute.class, Attribute::new ),
   COURSE_OF_DISEASE( "Clinical Course of Disease", T033, Attribute.class, Attribute::new ),
   POSITION( "Position", T082, Attribute.class, Attribute::new ),
   LATERALITY( "Side", T082, BodyLaterality.class, BodyLaterality::new ),
   SIZE( "Size", T081, Attribute.class, Attribute::new ),
   DIMENSION( "Dimension", T081, Attribute.class, Attribute::new ),
   DOSE( "Dose", T081, Dose.class, Dose::new ),
   NUMBER( "Number", T081, Element.class, Element::new ),
   QUANTITATIVE_CONCEPT( "Quantitative Concept", T081, Element.class, Element::new ),
   QUANTITY( "Quantity", T081, Attribute.class, Attribute::new ),
   UNIT_OF_MEASURE( "Unit of Measure", T081, Element.class, Element::new ),
   SEVERITY( "Severity", T080, Severity.class, Severity::new ),
   STATUS( "Status", T080, Attribute.class, Attribute::new ),
   SUSCEPTIBILITY( "Susceptibility", T080, Attribute.class, Attribute::new ),
   AGE_UNIT( "Age Unit", T079, Element.class, Element::new ),
   UNIT_OF_TIME( "Unit of Time", T079, Element.class, Element::new ),
   TEMPORAL_QUALIFIER( "Temporal Qualifier", T079, Attribute.class, Attribute::new ),
   PATHOLOGIC_PROCESS( "Pathologic Process", T046, SignSymptom.class, SignSymptom::new ),
   DISEASE_OR_DISORDER( "Disease or Disorder", T047, DiseaseDisorder.class, DiseaseDisorder::new ),
   PHARMACOLOGIC_SUBSTANCE( "Pharmacologic Substance", T121, Medication.class, Medication::new ),
   // Agent Combination, Regimen, after drugs.
   THERAPY_REGIMEN( "Chemo/immuno/hormone Therapy Regimen", T061, Procedure.class, Procedure::new ),
   INTERVENTION_OR_PROCEDURE( "Intervention or Procedure", T061, Procedure.class, Procedure::new ),
   // Anatomic Sites
   BODY_PART( "Body Part", T023, AnatomicalSite.class, AnatomicalSite::new ),
//   ORGAN_SYSTEM( "Organ System", T023, AnatomicalSite.class, AnatomicalSite::new ),
   ORGAN_SYSTEM( "Organ System", T022, AnatomicalSite.class, AnatomicalSite::new ),
   BODY_CAVITY( "Body Cavity", T023, AnatomicalSite.class, AnatomicalSite::new ),
   BODY_REGION( "Body Region", T023, AnatomicalSite.class, AnatomicalSite::new ),
   ORGAN( "Organ", T023, AnatomicalSite.class, AnatomicalSite::new ),
//   VASCULAR_SYSTEM( "Vascular System", T023, AnatomicalSite.class, AnatomicalSite::new ),
   LYMPH_NODE( "Lymph Node", T023, AnatomicalSite.class, AnatomicalSite::new ),
   // Tissue after Body Parts
   TISSUE( "Tissue", T024, SignSymptom.class, SignSymptom::new ),
   BODY_FLUID_OR_SUBSTANCE( "Body Fluid or Substance", T031, SignSymptom.class, SignSymptom::new ),
   // Cells, Genes, etc.
   ABNORMAL_CELL( "Abnormal Cell", T025, SignSymptom.class, SignSymptom::new ),
   GENE( "Gene", T028, SignSymptom.class, SignSymptom::new ),
   MOLECULAR_SEQUENCE_VARIATION( "Molecular Sequence Variation", T049, SignSymptom.class, SignSymptom::new ),
   GENE_PRODUCT( "Gene Product", T116, SignSymptom.class, SignSymptom::new ),
   TEST_RESULT( "Clinical Test Result", T034, Lab.class, Lab::new ),
   CANCER( "Neoplasm", T191, DiseaseDisorder.class, DiseaseDisorder::new ),
   MASS( "Mass", T184, SignSymptom.class, SignSymptom::new ),
   DISEASE_STAGE_QUALIFIER( "Disease Stage Qualifier", T201, Attribute.class, Attribute::new ),
   DISEASE_GRADE_QUALIFIER( "Disease Grade Qualifier", T201, Attribute.class, Attribute::new ),
   BEHAVIOR( "Behavior", T201, Attribute.class, Attribute::new ),
   GENERIC_TNM_FINDING( "Generic TNM Finding", T201, Attribute.class, Attribute::new ),
   PATHOLOGIC_TNM_FINDING( "Pathologic TNM Finding", T201, Attribute.class, Attribute::new );

   static public final String DPHE_GROUPING_SCHEME = "DpheSemanticGrouping";
   static public final String DPHE_GROUP = "DPHE_GROUP";


   private final String _name;
   private final SemanticTui _tui;
   private final Class<? extends Element> _clazz;
   private final Function<JCas, ? extends Element> _creator;

   DpheGroup( final String name,
              final SemanticTui tui,
              final Class<? extends Element> clazz,
              final Function<JCas, ? extends Element> creator ) {
      _name = name;
      _tui = tui;
      _clazz = clazz;
      _creator = creator;
   }

   @Override
   final public String getName() {
      return _name;
   }

   @Override
   final public int compareName( final NameOwner otherOwner ) {
      return 0;
   }

   @Override
   final public int getOrdinal() {
      return ordinal();
   }

   @Override
   final public int compareGroup( final DpheGroup otherGroup ) {
      return DpheGroupAccessor.getInstance().compare( this, otherGroup );
   }

   final public SemanticTui getTui() {
      return _tui;
   }

   final public Function<JCas, ? extends Element> getCreator() {
      return _creator;
   }


   /**
    *
    * @param mentions group of mentions
    * @return best SemanticTui among the mentions.  Best is last/highest index SemanticGrouping among the mentions.
    */
   static public <M extends Mention> DpheGroup getBestMentionGroup( final Collection<M> mentions ) {
      return mentions.stream()
                     .map( Mention::getDpheGroup )
                     .map( DpheGroupAccessor.getInstance()::getByName )
                     .filter( Objects::nonNull )
                     .min( DpheGroupAccessor.getInstance() )
                     .orElse( DpheGroupAccessor.getInstance().getNullGroup() );
   }

   /**
    *
    * @param annotation -
    * @return grouping for annotation according to its ontology concept
    */
   static public DpheGroup getBestAnnotationGroup( final IdentifiedAnnotation annotation ) {
      return DpheGroupAccessor.getInstance()
                              .getBestGroup( DpheGroupAccessor.getInstance().getAnnotationGroups( annotation ) );
   }

}
