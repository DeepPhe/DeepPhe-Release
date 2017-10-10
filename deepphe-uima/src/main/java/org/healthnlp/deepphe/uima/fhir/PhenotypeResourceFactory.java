package org.healthnlp.deepphe.uima.fhir;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.fhir.*;
import org.healthnlp.deepphe.fhir.Condition;
import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Finding;
import org.healthnlp.deepphe.fhir.Medication;
import org.healthnlp.deepphe.fhir.Observation;
import org.healthnlp.deepphe.fhir.Procedure;
import org.healthnlp.deepphe.fhir.Stage;
import org.healthnlp.deepphe.fhir.fact.*;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.fhir.summary.*;
import org.healthnlp.deepphe.fhir.summary.Episode;
import org.healthnlp.deepphe.fhir.summary.MedicalRecord;
import org.healthnlp.deepphe.fhir.summary.PatientPhenotype;
import org.healthnlp.deepphe.fhir.summary.PatientSummary;
import org.healthnlp.deepphe.fhir.summary.TumorPhenotype;
import org.healthnlp.deepphe.uima.types.*;
import org.healthnlp.deepphe.uima.types.BodySite;
import org.healthnlp.deepphe.uima.types.CancerPhenotype;
import org.healthnlp.deepphe.uima.types.Composition;
import org.healthnlp.deepphe.uima.types.HumanName;
import org.healthnlp.deepphe.uima.types.Patient;
import org.healthnlp.deepphe.uima.types.Property;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRRegistry;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.model.Condition.ConditionEvidenceComponent;

import edu.pitt.dbmi.nlp.noble.tools.TextTools;

import java.net.URI;
import java.util.*;


public class PhenotypeResourceFactory {

   private static Annotation getAnnotationByIdentifier( JCas jcas, String id ) {
      if ( id == null )
         return null;

      for ( Annotation a : getAnnotations( jcas, Annotation.type ) ) {
         String a_id = getIdentifier( a );
         if ( a_id != null && a_id.equals( id ) )
            return a;
      }
      return null;
   }

   /**
    * get preferred name
    *
    * @param a
    * @return
    */
   private static String getPrefferedName( Annotation a ) {
      if ( a instanceof org.healthnlp.deepphe.uima.types.Fact ) {
         return ((org.healthnlp.deepphe.uima.types.Fact) a).getHasPreferredName();
      }
      return null;
   }

   /**
    * get preferred name
    *
    * @param a
    * @return
    */
   private static String getIdentifier( Annotation a ) {
      if ( a instanceof org.healthnlp.deepphe.uima.types.Fact ) {
         return ((org.healthnlp.deepphe.uima.types.Fact) a).getHasIdentifier();
      }
      return null;
   }


   private static String getResourceURI( Annotation a ) {
      if ( a instanceof org.healthnlp.deepphe.uima.types.Fact ) {
         return ((org.healthnlp.deepphe.uima.types.Fact) a).getHasURI();
      }
      return null;
   }


   private static Annotation getAnnotationByName( JCas jcas, int type, String name ) {
      for ( Annotation a : getAnnotations( jcas, type ) ) {
         String prefName = getPrefferedName( a );
         if ( prefName != null && prefName.equals( name ) )
            return a;
      }
      return null;
   }

   private static Annotation getAnnotationByURI( JCas jcas, int type, String uri ) {
      for ( Annotation a : getAnnotations( jcas, type ) ) {
         String prefName = getResourceURI( a );
         if ( prefName != null && prefName.equals( uri ) )
            return a;
      }
      return null;
   }


   /**
    * save FHIR report in JCas
    *
    * @param r
    * @param jcas
    */
   public static Composition saveReport( Report r, JCas jcas ) {
      // first lets create Report annotation
      Annotation a = getAnnotationByIdentifier( jcas, r.getResourceIdentifier() );
      Composition comp = a == null ? new Composition( jcas ) : (Composition) a;
      comp.setBegin( r.getOffset() );
      comp.setEnd( r.getOffset() + r.getReportText().length() );
      comp.setHasDateOfComposition( "" + r.getDate() );
      comp.setHasTitle( r.getTitle() );
      comp.setHasDocumentOffset( r.getOffset() );

      // add doc type
      if ( r.getType() != null ) {
         org.healthnlp.deepphe.uima.types.Fact dont = (org.healthnlp.deepphe.uima.types.Fact) getAnnotationByName( jcas, org.healthnlp.deepphe.uima.types.Fact.type, r.getType().getText() );
         if ( dont == null )
            dont = new org.healthnlp.deepphe.uima.types.Fact( jcas );
         addCodeableConcept( dont, r.getType() );
         comp.setHasDocType( getValue( jcas, dont ) );
      }

      // create patient
      Patient patient = savePatient( r.getPatient(), jcas );
      comp.setHasPatient( getValue( jcas, patient ) );

      comp.setHasURI( "" + r.getConceptURI() );
      comp.setHasIdentifier( r.getResourceIdentifier() );
      comp.setHasPreferredName( r.getTitle() );


      // init individual components
      List<FeatureStructure> events = new ArrayList<FeatureStructure>();
      int n = 1;
      for ( Element e : r.getReportElements() ) {
         // add to FSArray
         org.healthnlp.deepphe.uima.types.Fact el = saveElement( e, jcas );
         if ( el != null ) {
            // reset offsets
            int[] st = new int[]{ el.getBegin(), el.getEnd() };
            for ( String m : FHIRUtils.getMentionExtensions( (DomainResource) e.getResource() ) ) {
               st = FHIRUtils.getMentionSpan( m );
               break;
            }
            el.setHasDocumentOffset( r.getOffset() );
            el.setBegin( st[ 0 ] + r.getOffset() );
            el.setEnd( st[ 1 ] + r.getOffset() );

			/*	// add report date and temporal order
            if(r.getDate() != null){
					el.setHasRecordedDate(r.getDate().toString());
				}
				el.setHasTemporalOrder(FHIRUtils.createTemporalOrder(e,n++));*/

            // ommit non-annotations
            if ( el instanceof org.healthnlp.deepphe.uima.types.Annotation )
               events.add( el );
         }
      }
      if ( !events.isEmpty() )
         comp.setHasEvent( getValues( jcas, events ) );

      for ( CancerSummary cs : r.getCancerSummaries() ) {
         comp.setHasCompositionSummaryCancer( getValue( jcas, saveCancerSummary( cs, jcas ) ) );
      }
      for ( TumorSummary ts : r.getTumorSummaries() ) {
         comp.setHasCompositionSummaryTumor( getValue( jcas, saveTumorSummary( ts, jcas ) ) );
      }
      if ( r.getPatientSummary() != null )
         comp.setHasCompositionSummaryPatient( getValue( jcas, savePatientSummary( r.getPatientSummary(), jcas ) ) );

      comp.addToIndexes();

      return comp;
   }


   /**
    * create tumor summary
    *
    * @param summaryFHIR
    * @param jcas
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Tumor saveTumorSummary( TumorSummary summaryFHIR, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, summaryFHIR.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Tumor summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.Tumor( jcas ) : (org.healthnlp.deepphe.uima.types.Tumor) a;

      // add phenotype
      org.healthnlp.deepphe.uima.types.TumorPhenotype phenotypeAnnotation = saveTumorPhenotype( summaryFHIR.getPhenotype(), jcas );
      summaryAnnotation.setHasPhenotype( getValue( jcas, phenotypeAnnotation ) );

      // save episode info
      List<FeatureStructure> elist = new ArrayList<FeatureStructure>();
      for ( Episode episode : summaryFHIR.getEpisodes() ) {
         elist.add( saveEpisode( jcas, episode ) );
      }
      summaryAnnotation.setHasEpisode( getValues( jcas, elist ) );

      // add generic content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : summaryFHIR.getFactCategories() ) {
         FactList facts = summaryFHIR.getFacts( category );
         flist.add( saveFactList( facts, jcas ) );
      }

      // save properties
      saveProperties( summaryFHIR.getProperties(), summaryAnnotation, jcas );

      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );
      summaryAnnotation.setHasURI( "" + summaryFHIR.getConceptURI() );
      summaryAnnotation.setHasIdentifier( summaryFHIR.getResourceIdentifier() );
      summaryAnnotation.setHasAnnotationType( summaryFHIR.getAnnotationType() );
      summaryAnnotation.addToIndexes();

      return summaryAnnotation;
   }

   private static org.healthnlp.deepphe.uima.types.FactList saveFactList( FactList factList, JCas jcas ) {
      //TODO: solve replication of lists
      org.healthnlp.deepphe.uima.types.FactList annotation = new org.healthnlp.deepphe.uima.types.FactList( jcas );
      annotation.setHasCategory( factList.getCategory() );
      annotation.setHasTypes( getStringValues( jcas, factList.getTypes() ) );
      List<FeatureStructure> facts = new ArrayList<FeatureStructure>();
      for ( Fact f : factList ) {
         facts.add( saveFact( f, jcas ) );
      }
      annotation.setHasFacts( getValues( jcas, facts ) );
      annotation.addToIndexes();
      return annotation;
   }

   private static void deleteAnnotations( FSArray arr, JCas jcas ) {
      List<FeatureStructure> list = new ArrayList<FeatureStructure>();
      for ( int i = 0; i < getSize( arr ); i++ ) {
         list.add( arr.get( i ) );
      }
      for ( FeatureStructure st : list ) {
         jcas.removeFsFromIndexes( st );
      }
   }


   private static FactList loadFactList( org.healthnlp.deepphe.uima.types.FactList annotation ) {
      FactList facts = new DefaultFactList();
      facts.setCategory( annotation.getHasCategory() );
      for ( int i = 0; i < getSize( annotation.getHasTypes() ); i++ ) {
         facts.getTypes().add( annotation.getHasTypes( i ) );
      }
      for ( int i = 0; i < getSize( annotation.getHasFacts() ); i++ ) {
         facts.add( loadFact( annotation.getHasFacts( i ) ) );
      }

      return facts;
   }

   private static Fact loadFact( org.healthnlp.deepphe.uima.types.Fact annotation ) {
      // retun fact on annotation
      //if(FHIRRegistry.getInstance().hasFact(annotation))
      //	return FHIRRegistry.getInstance().getFact(annotation);


      // try to convert if annotation
      Element element = loadElement( annotation );

      Fact fact = null;
      if ( element != null ) {
         fact = FactFactory.createFact( element );
      } else {
         fact = FactFactory.createFact( annotation.getHasType() );
      }

      // create a generic fact if possible
      fact.setName( annotation.getHasPreferredName() );
      fact.setUri( annotation.getHasURI() );
      fact.setIdentifier( annotation.getHasIdentifier() );
      fact.setLabel( annotation.getHasLabel() );
      fact.setType( annotation.getHasType() );
      fact.setTemporalOrder( annotation.getHasTemporalOrder() );
      if ( annotation.getHasRecordedDate() != null )
         fact.setRecordedDate( TextTools.parseDate( annotation.getHasRecordedDate() ) );
      if ( annotation.getHasDocumentIdentifier() != null )
         fact.setDocumentIdentifier( annotation.getHasDocumentIdentifier() );
      if ( annotation.getHasDocumentType() != null )
         fact.setDocumentType( annotation.getHasDocumentType() );
      if ( annotation.getHasPatientIdentifier() != null )
         fact.setPatientIdentifier( annotation.getHasPatientIdentifier() );


      // add provinence
      for ( int i = 0; i < getSize( annotation.getHasProvenanceText() ); i++ ) {
         fact.addProvenanceText( FactFactory.createTextMention( annotation.getHasProvenanceText( i ) ) );
      }

      // add ancestors
      for ( int i = 0; i < getSize( annotation.getHasAncestors() ); i++ ) {
         fact.addAncestor( annotation.getHasAncestors( i ) );
      }

      // add provenance facts
      //TODO: load provenance is disabled for now
      for ( int i = 0; i < getSize( annotation.getHasProvenanceFacts() ); i++ ) {
         if ( FHIRRegistry.getInstance().hasFact( annotation.getHasProvenanceFacts( i ) ) )
            fact.addProvenanceFact( FHIRRegistry.getInstance().getFact( annotation ) );
      }

      // load properties
      fact.setProperties( loadProperties( annotation ) );

      return fact;
   }

   /**
    * create saving of fact list
    *
    * @param jcas
    * @param fact
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Fact saveFact( BodySiteFact fact, org.healthnlp.deepphe.uima.types.BodySite annotation, JCas jcas ) {
      // call original fact save
      saveFact( (Fact) fact, (org.healthnlp.deepphe.uima.types.Fact) annotation, jcas );

      if ( !fact.getModifiers().isEmpty() )
         annotation.setHasBodySiteModifier( getValues( jcas, getFeatureList( jcas, fact.getModifiers() ) ) );

      annotation.addToIndexes();
      return annotation;
   }

   /**
    * create saving of fact list
    *
    * @param jcas
    * @param fact
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Fact saveFact( ProcedureFact fact, org.healthnlp.deepphe.uima.types.Procedure annotation, JCas jcas ) {
      saveFact( (Fact) fact, (org.healthnlp.deepphe.uima.types.Fact) annotation, jcas );
      //TODO: implement further???
      return annotation;
   }

   /**
    * create saving of fact list
    *
    * @param jcas
    * @param fact
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Fact saveFact( ObservationFact fact, org.healthnlp.deepphe.uima.types.Observation annotation, JCas jcas ) {
      // call original fact save
      saveFact( (Fact) fact, (org.healthnlp.deepphe.uima.types.Fact) annotation, jcas );

      if ( fact.getInterpretation() != null )
         annotation.setHasInterpretation( getValue( jcas, saveFact( fact.getInterpretation(), jcas ) ) );

      if ( fact.getMethod() != null )
         annotation.setHasMethod( getValue( jcas, saveFact( fact.getMethod(), jcas ) ) );

      if ( fact.getValue() != null )
         annotation.setHasNumValue( getValue( jcas, saveFact( fact.getValue(), jcas ) ) );

      if ( !fact.getBodySite().isEmpty() )
         annotation.setHasBodySite( getValues( jcas, getFeatureList( jcas, fact.getBodySite() ) ) );

      annotation.addToIndexes();

      return annotation;
   }


   private static org.healthnlp.deepphe.uima.types.Fact saveFact( ValueFact fact, org.healthnlp.deepphe.uima.types.Quantity annotation, JCas jcas ) {
      saveFact( (Fact) fact, (org.healthnlp.deepphe.uima.types.Fact) annotation, jcas );

      Fact u = new Fact();
      u.setName( fact.getUnit() );
      u.setLabel( fact.getUnit() );
      u.setIdentifier( fact.getUnit() );

      annotation.setHasUnit( getValue( jcas, saveFact( u, jcas ) ) );
      annotation.setHasQuantityValue( getDoubleValues( jcas, fact.getValues() ) );

      annotation.addToIndexes();
      return annotation;
   }


   /**
    * create saving of fact list
    *
    * @param jcas
    * @param fact
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Fact saveFact( Fact fact, org.healthnlp.deepphe.uima.types.Fact annotation, JCas jcas ) {
      annotation.setHasURI( fact.getUri() );
      annotation.setHasIdentifier( fact.getIdentifier() );
      annotation.setHasPreferredName( fact.getName() );
      annotation.setHasLabel( fact.getLabel() );
      annotation.setHasProvenanceText( getStringValues( jcas, fact.getProvenanceMentions() ) );
      annotation.setHasType( fact.getType() );
      annotation.setHasTemporalOrder( fact.getTemporalOrder() );
      if ( fact.getRecordedDate() != null )
         annotation.setHasRecordedDate( fact.getRecordedDate().toString() );
      if ( fact.getDocumentIdentifier() != null )
         annotation.setHasDocumentIdentifier( fact.getDocumentIdentifier() );
      if ( fact.getDocumentType() != null )
         annotation.setHasDocumentType( fact.getDocumentType() );
      if ( fact.getPatientIdentifier() != null )
         annotation.setHasPatientIdentifier( fact.getPatientIdentifier() );

      // set begin & end if not set prior
      for ( TextMention m : fact.getProvenanceText() ) {
         annotation.setBegin( m.getStart() + annotation.getHasDocumentOffset() );
         annotation.setEnd( m.getEnd() + annotation.getHasDocumentOffset() );
         break;
      }

      // set provinence facts
      List<FeatureStructure> vals = new ArrayList<FeatureStructure>();
      for ( Fact p : fact.getProvenanceFacts() ) {
         Annotation a = getAnnotationByIdentifier( jcas, p.getIdentifier() );
         if ( a != null && !fact.getIdentifier().equals( ((org.healthnlp.deepphe.uima.types.Fact) a).getHasIdentifier() ) )
            vals.add( a );
      }
      if ( !vals.isEmpty() )
         annotation.setHasProvenanceFacts( getValues( jcas, vals ) );

      // save ancestors
      if ( !fact.getAncestors().isEmpty() )
         annotation.setHasAncestors( getStringValues( jcas, fact.getAncestors() ) );

      // save properties
      saveProperties( fact.getProperties(), annotation, jcas );

      // add to registry
      FHIRRegistry.getInstance().addFact( fact, annotation );


      annotation.addToIndexes();
      return annotation;
   }

   private Set<Fact> getAllProvenanceFacts( Fact fact, Set<Fact> list ) {
      for ( Fact f : fact.getProvenanceFacts() ) {
         list.add( f );
         getAllProvenanceFacts( f, list );
      }
      return list;
   }


   private static org.healthnlp.deepphe.uima.types.Fact saveFact( Fact fact, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, fact.getIdentifier() );
      org.healthnlp.deepphe.uima.types.Fact annotation = null;
      if ( a != null )
         annotation = saveFact( fact, (org.healthnlp.deepphe.uima.types.Fact) a, jcas );
      else if ( fact instanceof BodySiteFact )
         annotation = saveFact( (BodySiteFact) fact, new BodySite( jcas ), jcas );
      else if ( fact instanceof ObservationFact )
         annotation = saveFact( (ObservationFact) fact, new org.healthnlp.deepphe.uima.types.Observation( jcas ), jcas );
      else if ( fact instanceof ProcedureFact )
         annotation = saveFact( (ProcedureFact) fact, new org.healthnlp.deepphe.uima.types.Procedure( jcas ), jcas );
      else if ( fact instanceof ValueFact )
         annotation = saveFact( (ValueFact) fact, new org.healthnlp.deepphe.uima.types.Quantity( jcas ), jcas );
      else
         annotation = saveFact( fact, new org.healthnlp.deepphe.uima.types.Fact( jcas ), jcas );
      return annotation;
   }


   private static org.healthnlp.deepphe.uima.types.Fact saveFact( CodeableConcept c, JCas jcas ) {
      return saveFact( FactFactory.createFact( c ), jcas );
   }


   /**
    * create phenotype
    *
    * @param phenotype
    * @param jcas
    * @return
    */

   private static org.healthnlp.deepphe.uima.types.TumorPhenotype saveTumorPhenotype( TumorPhenotype phenotype, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, phenotype.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.TumorPhenotype summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.TumorPhenotype( jcas ) : (org.healthnlp.deepphe.uima.types.TumorPhenotype) a;

      // add content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : phenotype.getFactCategories() ) {
         FactList facts = phenotype.getFacts( category );
         flist.add( saveFactList( facts, jcas ) );
      }
      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );
      summaryAnnotation.setHasURI( "" + phenotype.getConceptURI() );
      summaryAnnotation.setHasIdentifier( phenotype.getResourceIdentifier() );
      summaryAnnotation.addToIndexes();

      return summaryAnnotation;
   }


   /**
    * save cancer summary to typesystem
    *
    * @param summaryFHIR
    * @param jcas
    * @return
    */
   private static org.healthnlp.deepphe.uima.types.Cancer saveCancerSummary( CancerSummary summaryFHIR, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, summaryFHIR.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Cancer summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.Cancer( jcas ) : (org.healthnlp.deepphe.uima.types.Cancer) a;

      // add phenotype
      summaryAnnotation.setHasPhenotype( getValue( jcas, saveCancerPhenotype( summaryFHIR.getPhenotype(), jcas ) ) );

      // add content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : summaryFHIR.getFactCategories() ) {
         FactList facts = summaryFHIR.getFacts( category );
         flist.add( saveFactList( facts, jcas ) );
      }
      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );
      // add tumor
      if ( !summaryFHIR.getTumors().isEmpty() ) {
         int i = 0;
         summaryAnnotation.setRealizes( new FSArray( jcas, summaryFHIR.getTumors().size() ) );
         for ( TumorSummary tumorSummary : summaryFHIR.getTumors() ) {
            Tumor tumorAnnotation = saveTumorSummary( tumorSummary, jcas );
            summaryAnnotation.setRealizes( i++, tumorAnnotation );
         }
      }
      // save properties
      saveProperties( summaryFHIR.getProperties(), summaryAnnotation, jcas );

      summaryAnnotation.setHasURI( "" + summaryFHIR.getConceptURI() );
      summaryAnnotation.setHasIdentifier( summaryFHIR.getResourceIdentifier() );
      summaryAnnotation.setHasAnnotationType( summaryFHIR.getAnnotationType() );
      summaryAnnotation.addToIndexes();

      return summaryAnnotation;
   }

   /**
    * save cancer phenotype to typesystem
    *
    * @param phenotype
    * @param jcas
    * @return
    */

   private static CancerPhenotype saveCancerPhenotype( org.healthnlp.deepphe.fhir.summary.CancerPhenotype phenotype, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, phenotype.getResourceIdentifier() );
      CancerPhenotype summaryAnnotation = (a == null) ? new CancerPhenotype( jcas ) : (CancerPhenotype) a;

      // add content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : phenotype.getFactCategories() ) {
         FactList facts = phenotype.getFacts( category );
         flist.add( saveFactList( facts, jcas ) );
      }
      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );

      // regurlar fluf
      summaryAnnotation.setHasURI( "" + phenotype.getConceptURI() );
      summaryAnnotation.setHasIdentifier( phenotype.getResourceIdentifier() );
      summaryAnnotation.addToIndexes();
      return summaryAnnotation;
   }

   private static org.healthnlp.deepphe.uima.types.PatientSummary savePatientSummary( PatientSummary summaryFHIR, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, summaryFHIR.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.PatientSummary summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.PatientSummary( jcas ) : (org.healthnlp.deepphe.uima.types.PatientSummary) a;

      // add content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : summaryFHIR.getFactCategories() ) {
         FactList facts = summaryFHIR.getFacts( category );
         if ( !facts.isEmpty() )
            flist.add( saveFactList( facts, jcas ) );
      }
      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );
      // add phenotype
      summaryAnnotation.setHasPhenotype( getValue( jcas, savePatientPhenotype( summaryFHIR.getPhenotype(), jcas ) ) );

      //TODO: add other information that is not in content
      // save properties
      saveProperties( summaryFHIR.getProperties(), summaryAnnotation, jcas );

      summaryAnnotation.setHasURI( "" + summaryFHIR.getConceptURI() );
      summaryAnnotation.setHasIdentifier( summaryFHIR.getResourceIdentifier() );
      summaryAnnotation.addToIndexes();

      return summaryAnnotation;
   }


   private static org.healthnlp.deepphe.uima.types.PatientPhenotype savePatientPhenotype( PatientPhenotype phenotype, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, phenotype.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.PatientPhenotype summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.PatientPhenotype( jcas ) : (org.healthnlp.deepphe.uima.types.PatientPhenotype) a;

      // add content
      List<FeatureStructure> flist = new ArrayList<FeatureStructure>();
      for ( String category : phenotype.getFactCategories() ) {
         FactList facts = phenotype.getFacts( category );
         if ( !facts.isEmpty() )
            flist.add( saveFactList( facts, jcas ) );
      }
      deleteAnnotations( summaryAnnotation.getHasContent(), jcas );
      summaryAnnotation.setHasContent( getValues( jcas, flist ) );
      // regurlar fluf
      summaryAnnotation.setHasURI( "" + phenotype.getConceptURI() );
      summaryAnnotation.setHasIdentifier( phenotype.getResourceIdentifier() );
      summaryAnnotation.addToIndexes();
      return summaryAnnotation;
   }

   private static void addCodeableConcept( org.healthnlp.deepphe.uima.types.Fact a, CodeableConcept cc ) {
      URI uri = FHIRUtils.getConceptURI( cc );
      if ( uri != null ) {
         a.setHasURI( "" + uri );
         a.setHasPreferredName( FHIRUtils.getConceptName( uri ) );
      } else {
         //TODO: what to do without URI ????
         a.setHasURI( FHIRUtils.getConceptCode( cc ) );
         a.setHasPreferredName( cc.getText() );
      }
      a.setHasLabel( cc.getText() );
      a.addToIndexes();
   }


   public static org.healthnlp.deepphe.uima.types.Fact saveElement( Element e, JCas jcas ) {
      org.healthnlp.deepphe.uima.types.Fact el = null;
      if ( e instanceof Disease ) {
         el = saveDiagnosis( (Disease) e, jcas );
      } else if ( e instanceof Observation ) {
         el = saveObservation( (Observation) e, jcas );
      } else if ( e instanceof Finding ) {
         el = saveFinding( (Finding) e, jcas );
      } else if ( e instanceof Procedure ) {
         el = saveProcedure( (Procedure) e, jcas );
      } else if ( e instanceof Medication ) {
         el = saveMedication( (Medication) e, jcas );
      } else if ( e instanceof org.healthnlp.deepphe.fhir.Patient ) {
         el = savePatient( (org.healthnlp.deepphe.fhir.Patient) e, jcas );
      } else if ( e instanceof org.healthnlp.deepphe.fhir.AnatomicalSite ) {
         el = saveBodySite( (AnatomicalSite) e, jcas );
      }

      // do common things
      if ( el != null ) {
         saveFact( FactFactory.createFact( e ), el, jcas );
         //saveExtensions(e,el,jcas);

         List<String> mentions = FHIRUtils.getMentionExtensions( (DomainResource) e );
         // save text provinence for annotation
         if ( el instanceof org.healthnlp.deepphe.uima.types.Annotation ) {
            org.healthnlp.deepphe.uima.types.Annotation ael = (org.healthnlp.deepphe.uima.types.Annotation) el;
            ael.setHasAnnotationType( e.getAnnotationType() );
            ael.setHasSpan( getStringValues( jcas, mentions ) );
         }
         el.addToIndexes();
      }

      return el;
   }


   private static void saveExtensions( Element e, org.healthnlp.deepphe.uima.types.Fact el, JCas jcas ) {
      saveProperties( FHIRUtils.getProperties( (DomainResource) e ), el, jcas );
   }

   private static void saveProperties( Map<String, String> props, org.healthnlp.deepphe.uima.types.Fact el, JCas jcas ) {
      //deleteAnnotations(el.getHasProperties(), jcas);
      List<FeatureStructure> list = new ArrayList<FeatureStructure>();
      for ( Object key : props.keySet() ) {
         // skip mentions as they are handled elsewhere
         if ( FHIRUtils.MENTION_URL.equals( key ) )
            continue;
         Property pp = new Property( jcas );
         pp.setName( key.toString() );
         pp.setValue( props.get( key.toString() ) );
         pp.addToIndexes();
         list.add( pp );
      }
      if ( !list.isEmpty() )
         el.setHasProperties( getValues( jcas, list ) );
      el.addToIndexes();
   }


   /**
    * get set of annotations from cas of a given type
    *
    * @param cas
    * @param type
    * @return
    */
   public static List<Annotation> getAnnotations( JCas cas, int type ) {
      List<Annotation> a = new ArrayList<Annotation>();
      Iterator<Annotation> it = cas.getAnnotationIndex( type ).iterator();
      while ( it.hasNext() )
         a.add( it.next() );
      return a;
   }


   private static org.healthnlp.deepphe.uima.types.Patient getPatient( JCas jcas ) {
      // find existing patient
      for ( Annotation a : getAnnotations( jcas, Patient.type ) ) {
         return (Patient) a;
      }
      return null;
   }

   /**
    * @param jcas
    * @param val
    * @return
    */
   private static FSArray getValue( JCas jcas, FeatureStructure val ) {
      return (val == null) ? null : getValues( jcas, Collections.singleton( val ) );

   }

   /**
    * @param jcas
    * @param vals
    * @return
    */
   private static FSArray getValues( JCas jcas, Collection<FeatureStructure> vals ) {
      if ( vals == null )
         return null;
      FSArray fs = new FSArray( jcas, vals.size() );
      int i = 0;
      for ( FeatureStructure val : vals ) {
         fs.set( i++, val );
      }
      fs.addToIndexes();
      return fs;

   }

   /**
    * @param jcas
    * @param vals
    * @return
    */
   private static StringArray getStringValues( JCas jcas, Collection<String> vals ) {
      if ( vals == null )
         return null;
      StringArray fs = new StringArray( jcas, vals.size() );
      int i = 0;
      for ( String val : vals ) {
         fs.set( i++, val );
      }
      fs.addToIndexes();
      return fs;

   }

   private static DoubleArray getDoubleValues( JCas jcas, double[] vals ) {
      if ( vals == null )
         return null;
      DoubleArray fs = new DoubleArray( jcas, vals.length );
      int i = 0;
      for ( double val : vals ) {
         fs.set( i++, val );
      }
      fs.addToIndexes();
      return fs;

   }


   private static org.healthnlp.deepphe.uima.types.Patient savePatient( org.healthnlp.deepphe.fhir.Patient e, JCas jcas ) {
      if ( e == null )
         return null;

      org.healthnlp.deepphe.uima.types.Patient p = (org.healthnlp.deepphe.uima.types.Patient) getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      //org.healthnlp.deepphe.uima.types.Patient p = getPatient(jcas);
      // init the patient
      if ( p == null ) {
         p = new org.healthnlp.deepphe.uima.types.Patient( jcas );
      }

      p.setHasURI( "" + e.getConceptURI() );
      p.setHasIdentifier( e.getResourceIdentifier() );
      p.setHasPreferredName( e.getDisplayText() );

      // add existing mentions
      List<String> mentions = new ArrayList<String>();
	/*	if(p.getHasSpan() != null){
			for(int i=0;i<p.getHasSpan().size();i++){
				mentions.add(p.getHasSpan(i));
			}
		}*/
      mentions.addAll( FHIRUtils.getMentionExtensions( (DomainResource) e ) );
      p.setHasSpan( new StringArray( jcas, mentions.size() ) );
      int i = 0;
      for ( String m : mentions ) {
         int[] ms = FHIRUtils.getMentionSpan( m );
         p.setBegin( ms[ 0 ] );
         p.setEnd( ms[ 1 ] );
         p.setHasSpan( i++, m );
      }


      // set/reset the name
      HumanName hn = (HumanName) getAnnotationByName( jcas, HumanName.type, e.getDisplayText() );
      if ( hn == null )
         hn = new HumanName( jcas );
      hn.setBegin( p.getBegin() );
      hn.setEnd( p.getEnd() );
      hn.setHasPreferredName( e.getDisplayText() );
      hn.setHasFullName( e.getDisplayText() );
      hn.addToIndexes();
      p.setHasName( getValue( jcas, hn ) );

      if ( e.getBirthDate() != null )
         p.setHasBirthDate( e.getBirthDate().toString() );
      if ( e.getGender() != null ) {
         p.setHasGender( getValue( jcas, saveFact( FHIRUtils.getCodeableConcept( e.getGender() ), jcas ) ) );
      }
      if ( e.getDeceased() != null && e.getDeceased() instanceof DateTimeType ) {
         try {
            p.setHasDeathDate( e.getDeceasedDateTimeType().getValue().toString() );
         } catch ( Exception e1 ) {
            e1.printStackTrace();
         }
      }

      p.addToIndexes();
      return p;
   }

   private static org.healthnlp.deepphe.uima.types.MedicationStatement saveMedication( Medication e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.MedicationStatement t = (a == null) ? new org.healthnlp.deepphe.uima.types.MedicationStatement( jcas ) : (org.healthnlp.deepphe.uima.types.MedicationStatement) a;
      return t;
   }

   private static BodySite saveBodySite( AnatomicalSite e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      BodySite t = (a == null) ? new BodySite( jcas ) : (BodySite) a;

      // save modifiers
      t.setHasBodySiteModifier( getValues( jcas, getFactList( jcas, e.getModifier() ) ) );

      return t;
   }

   private static org.healthnlp.deepphe.uima.types.Procedure saveProcedure( Procedure e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Procedure t = (a == null) ? new org.healthnlp.deepphe.uima.types.Procedure( jcas ) : (org.healthnlp.deepphe.uima.types.Procedure) a;

      // add body location
      List<FeatureStructure> sites = getBodySites( jcas, e.getBodySite() );
      if ( !sites.isEmpty() )
         t.setHasBodySite( getValues( jcas, sites ) );

      return t;
   }


   private static org.healthnlp.deepphe.uima.types.Finding saveFinding( CodeableConcept cc, JCas jcas ) {
      String id = FHIRUtils.getResourceIdentifer( cc );

      // fetch a previously registered Finding
      Annotation a = getAnnotationByIdentifier( jcas, id );
      if ( a != null ) {
         return (org.healthnlp.deepphe.uima.types.Finding) a;
      }
      // define new finding and save it
      Finding finding = new Finding();
      finding.setCode( cc );
      if ( id != null )
         FHIRUtils.createIdentifier( finding.addIdentifier(), id );

      return (org.healthnlp.deepphe.uima.types.Finding) saveElement( finding, jcas );
   }


   private static org.healthnlp.deepphe.uima.types.Finding saveFinding( Finding e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Finding t = (a == null) ? new org.healthnlp.deepphe.uima.types.Finding( jcas ) : (org.healthnlp.deepphe.uima.types.Finding) a;

      // add body location
      List<FeatureStructure> sites = getBodySites( jcas, e.getBodySite() );
      if ( !sites.isEmpty() )
         t.setHasBodySite( getValues( jcas, sites ) );

      // add related evidence
      //saveEvidence(e,t, jcas);


      return t;
   }

   private static org.healthnlp.deepphe.uima.types.Observation saveObservation( Observation e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Observation ob = (a == null) ? new org.healthnlp.deepphe.uima.types.Observation( jcas ) : (org.healthnlp.deepphe.uima.types.Observation) a;

      // add interpretation
      if ( e.getInterpretation() != null && FHIRUtils.getConceptURI( e.getInterpretation() ) != null ) {
         ob.setHasInterpretation( getValue( jcas, saveFact( e.getInterpretation(), jcas ) ) );
      }

      // add interpretation
      if ( e.getMethod() != null && FHIRUtils.getConceptURI( e.getMethod() ) != null ) {
         ob.setHasMethod( getValue( jcas, saveFact( e.getMethod(), jcas ) ) );
      }

      if ( e.getValue() != null ) {
         //NumericModifier nm = new NumericModifier(jcas);
         if ( e.getValue() instanceof Quantity ) {
            Quantity q = (Quantity) e.getValue();
            ValueFact value = FactFactory.createFact( q );

            deleteAnnotations( ob.getHasNumValue(), jcas );

            org.healthnlp.deepphe.uima.types.Quantity nm = new org.healthnlp.deepphe.uima.types.Quantity( jcas );
            //nm.setHasQuantityValue((float)(q.getValue().doubleValue()));
            nm.setHasQuantityValue( getDoubleValues( jcas, value.getValues() ) );
            nm.setHasUnit( getValue( jcas, saveFact( value.getUnitFact(), jcas ) ) );

            nm.addToIndexes();
            ob.setHasNumValue( getValue( jcas, nm ) );
         }

      }
      //ob.setHasNumValue(e.getObservationValue());

      // add body location
      List<FeatureStructure> sites = getBodySites( jcas, Collections.singleton( e.getBodySite() ) );
      if ( !sites.isEmpty() )
         ob.setHasBodySite( getValues( jcas, sites ) );

      return ob;
   }

   private static DiseaseDisorder saveDiagnosis( Disease e, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, e.getResourceIdentifier() );
      DiseaseDisorder dd = (a == null) ? new DiseaseDisorder( jcas ) : (DiseaseDisorder) a;

      try {
         if ( e.getAbatement() != null )
            dd.setHasAbatementBoolean( ((BooleanType) e.getAbatement()).getValue() );
         //if(e.getOnsetAge() != null)
         //	dd.setHasAgeOnset(""+e.getOnsetAge().getValue());
         //if(e.getSeverity() != null)
         //	dd.setHas(""+e.getSeverity().getText());
         if ( e.getCategory() != null ) {
            dd.setHasConditionCategory( e.getCategory().getText() );
         }

         // add patient
         dd.setHasPatient( getValue( jcas, getPatient( jcas ) ) );

         // add body location
         List<FeatureStructure> sites = getBodySites( jcas, e.getBodySite() );
         if ( !sites.isEmpty() )
            dd.setHasBodySite( getValues( jcas, sites ) );


         // save stage
         Stage stage = e.getStage();
         if ( stage != null ) {
            deleteAnnotations( dd.getHasStage(), jcas );

            org.healthnlp.deepphe.uima.types.Stage st = new org.healthnlp.deepphe.uima.types.Stage( jcas );
				/*URI uri = FHIRUtils.getConceptURI(stage.getSummary());
				if(uri != null){
					CancerStage cs = new CancerStage(jcas);
					addCodeableConcept(cs,stage.getSummary());
					st.setHasStageSummary(getValue(jcas,cs));
				}*/
            if ( stage.getSummary() != null ) {
               st.setHasStageSummary( getValue( jcas, saveFinding( stage.getSummary(), jcas ) ) );
            }

            List<FeatureStructure> tnm = new ArrayList<FeatureStructure>();
            for ( Resource r : stage.getAssessmentTarget() ) {
               tnm.add( saveElement( (Finding) r, jcas ) );
            }
            if ( !tnm.isEmpty() )
               st.setHasStageAssessment( getValues( jcas, tnm ) );
            st.addToIndexes();

            // add stage
            dd.setHasStage( getValue( jcas, st ) );
         }

         // add related evidence
         //saveEvidence(e, dd, jcas);

         //dd.setHasStage(v);
      } catch ( Exception ex ) {
         ex.printStackTrace();
      }

      return dd;
   }


   private static void saveEvidence( Condition e, org.healthnlp.deepphe.uima.types.Fact dd, JCas jcas ) {
      Set<String> ids = e.getRelatedEvidenceIdentifiers();
      if ( !ids.isEmpty() ) {
         Map<String, String> props = new HashMap<String, String>();
         props.put( FHIRUtils.EVIDENCE, ids.toString() );
         saveProperties( props, dd, jcas );
      }
   }


   /**
    * get a list of BodySites from
    *
    * @param jcas
    * @param bodySites
    * @return
    */
   private static List<FeatureStructure> getBodySites( JCas jcas, Collection<CodeableConcept> bodySites ) {
      List<FeatureStructure> sites = new ArrayList<FeatureStructure>();
      for ( CodeableConcept bs : bodySites ) {
         String id = FHIRUtils.getResourceIdentifer( bs );
         BodySite bodySite = (BodySite) getAnnotationByIdentifier( jcas, id );
         if ( bodySite == null && id != null ) {
            bodySite = new BodySite( jcas );
            bodySite.setHasURI( "" + FHIRUtils.getConceptURI( bs ) );
            bodySite.setHasIdentifier( id );
            bodySite.setHasPreferredName( bs.getText() );
            bodySite.addToIndexes();
         }
         if ( bodySite != null )
            sites.add( bodySite );

      }
      return sites;
   }


   /**
    * get a list of BodySites from
    *
    * @param jcas
    * @param bodySites
    * @return
    */
   private static List<FeatureStructure> getFactList( JCas jcas, Collection<CodeableConcept> facts ) {
      List<FeatureStructure> sites = new ArrayList<FeatureStructure>();
      for ( CodeableConcept cc : facts ) {
         sites.add( saveFact( cc, jcas ) );
      }
      return sites;
   }

   /**
    * get a list of BodySites from
    *
    * @param jcas
    * @param bodySites
    * @return
    */
   private static List<FeatureStructure> getFeatureList( JCas jcas, Collection<Fact> facts ) {
      List<FeatureStructure> sites = new ArrayList<FeatureStructure>();
      for ( Fact cc : facts ) {
         sites.add( saveFact( cc, jcas ) );
      }
      return sites;
   }


   public static Element loadElement( org.healthnlp.deepphe.uima.types.Fact e ) {
      // retun fact on annotation
      if ( FHIRRegistry.getInstance().hasElement( e ) )
         return FHIRRegistry.getInstance().getElement( e );


      Element el = null;
      if ( e instanceof DiseaseDisorder ) {
         el = loadDiagnosis( (DiseaseDisorder) e );
      } else if ( e instanceof org.healthnlp.deepphe.uima.types.Observation ) {
         el = loadObservation( (org.healthnlp.deepphe.uima.types.Observation) e );
      } else if ( e instanceof org.healthnlp.deepphe.uima.types.Finding ) {
         el = loadFinding( (org.healthnlp.deepphe.uima.types.Finding) e );
      } else if ( e instanceof org.healthnlp.deepphe.uima.types.Procedure ) {
         el = loadProcedure( (org.healthnlp.deepphe.uima.types.Procedure) e );
      } else if ( e instanceof org.healthnlp.deepphe.uima.types.MedicationStatement ) {
         el = loadMedication( (org.healthnlp.deepphe.uima.types.MedicationStatement) e );
      } else if ( e instanceof org.healthnlp.deepphe.uima.types.BodySite ) {
         el = loadBodySite( (org.healthnlp.deepphe.uima.types.BodySite) e );
      }
      return el;
   }


   private static AnatomicalSite loadBodySite( BodySite e ) {
      AnatomicalSite ob = new AnatomicalSite();
      ob.setCode( getCodeableConcept( e ) );

      // add modifiers
      for ( int i = 0; i < getSize( e.getHasBodySiteModifier() ); i++ ) {
         ob.addModifier( FactFactory.createCodeableConcept( loadFact( e.getHasBodySiteModifier( i ) ) ) );
      }

      addMention( ob, e );
      addExtensions( ob, e );
      FHIRUtils.createIdentifier( ob.addIdentifier(), e.getHasIdentifier() );
      return ob;
   }

   private static Medication loadMedication( org.healthnlp.deepphe.uima.types.MedicationStatement e ) {
      Medication ob = new Medication();
      ob.setCode( getCodeableConcept( e ) );
      addMention( ob, e );
      addExtensions( ob, e );
      //FHIRUtils.createIdentifier(ob.addIdentifier(),e.getHasIdentifier());
      return ob;
   }

   private static Procedure loadProcedure( org.healthnlp.deepphe.uima.types.Procedure e ) {
      Procedure ob = new Procedure();
      ob.setCode( getCodeableConcept( e ) );
      addMention( ob, e );
      addExtensions( ob, e );
      FHIRUtils.createIdentifier( ob.addIdentifier(), e.getHasIdentifier() );
      return ob;
   }

   private static Finding loadFinding( org.healthnlp.deepphe.uima.types.Finding e ) {
      Finding ob = new Finding();
      ob.setCode( getCodeableConcept( e ) );

      // now lets take a look at the location of this disease
      //TODO: just commented out this code, not because it is wrong, but because it is right
      // unfortunately it causes havoc down the line
      if ( e.getHasBodySite() != null ) {
         for ( int i = 0; i < e.getHasBodySite().size(); i++ ) {
            BodySite bs = e.getHasBodySite( i );
            ob.addBodySite( getCodeableConcept( bs ) );
         }
      }
      addMention( ob, e );
      addExtensions( ob, e );
      addEvidence( ob, e );
      FHIRUtils.createIdentifier( ob.addIdentifier(), e.getHasIdentifier() );
      return ob;
   }

	/*private static Finding loadFinding(org.healthnlp.deepphe.uima.types.TNMValue e) {
		Finding ob = new Finding();
		ob.setCode(getCodeableConcept(e));
		ob.addExtension(FHIRUtils.createMentionExtension(e.getCoveredText(),e.getBegin(),e.getEnd()));
		FHIRUtils.createIdentifier(ob.addIdentifier(),e.getHasIdentifier());
		return ob;
	}*/

   private static Observation loadObservation( org.healthnlp.deepphe.uima.types.Observation e ) {
      Observation ob = new Observation();
      ob.setCode( getCodeableConcept( e ) );
      addMention( ob, e );
      addExtensions( ob, e );

      if ( e.getHasNumValue() != null ) {
         for ( int i = 0; i < e.getHasNumValue().size(); i++ ) {
            //TODO: numeric modifer
            //NumericModifier nm = e.getHasNumValue(i);
            //ob.setValue(nm.getHasValue(),nm.getHasUnits());
            org.healthnlp.deepphe.uima.types.Quantity nm = e.getHasNumValue( i );
            String unit = "";
            if ( nm.getHasUnit() != null && nm.getHasUnit().size() > 0 )
               unit = nm.getHasUnit( 0 ).getHasPreferredName();
            ob.setValue( Arrays.toString( nm.getHasQuantityValue().toArray() ), unit );
         }
      }

      if ( e.getHasInterpretation() != null && e.getHasInterpretation().size() > 0 ) {
         ob.setInterpretation( getCodeableConcept( e.getHasInterpretation( 0 ) ) );
      }

      if ( e.getHasMethod() != null && e.getHasMethod().size() > 0 ) {
         ob.setMethod( getCodeableConcept( e.getHasMethod( 0 ) ) );
      }

      FHIRUtils.createIdentifier( ob.addIdentifier(), e.getHasIdentifier() );
      return ob;
   }

   private static void addMention( DomainResource r, org.healthnlp.deepphe.uima.types.Annotation e ) {
      if ( e != null && e.getHasSpan() != null ) {
         for ( int i = 0; i < e.getHasSpan().size(); i++ )
            r.addExtension( FHIRUtils.createMentionExtension( e.getHasSpan( i ) ) );
      }
   }

   private static void addMention( DomainResource r, org.healthnlp.deepphe.uima.types.Fact e ) {
      for ( int i = 0; i < getSize( e.getHasProvenanceText() ); i++ ) {
         r.addExtension( FHIRUtils.createMentionExtension( e.getHasProvenanceText( i ) ) );
      }
   }

   private static void addExtensions( DomainResource r, org.healthnlp.deepphe.uima.types.Fact e ) {
      for ( int i = 0; i < getSize( e.getHasProperties() ); i++ ) {
         Property p = e.getHasProperties( i );
         r.addExtension( FHIRUtils.createExtension( p.getName(), p.getValue() ) );
      }
   }

   private static Map<String, String> loadProperties( org.healthnlp.deepphe.uima.types.Fact e ) {
      Map<String, String> props = new LinkedHashMap<String, String>();
      for ( int i = 0; i < getSize( e.getHasProperties() ); i++ ) {
         Property p = e.getHasProperties( i );
         props.put( p.getName(), p.getValue() );
      }
      return props;
   }


   private static Disease loadDiagnosis( DiseaseDisorder e ) {
      Disease dx = new Disease();
      dx.setCode( getCodeableConcept( e ) );

      // now lets take a look at the location of this disease
      if ( e.getHasBodySite() != null ) {
         for ( int i = 0; i < e.getHasBodySite().size(); i++ ) {
            BodySite bs = e.getHasBodySite( i );
            dx.addBodySite( getCodeableConcept( bs ) );
         }
      }
      // now lets get the location relationships
      if ( e.getHasStage() != null ) {
         // get stage
         org.healthnlp.deepphe.uima.types.Stage st = e.getHasStage( 0 );

         // init
         Stage stage = new Stage();
         if ( st.getHasStageSummary() != null && st.getHasStageSummary().size() > 0 ) {
            stage.setSummary( getCodeableConcept( st.getHasStageSummary( 0 ) ) );
         }
         for ( int i = 0; i < getSize( st.getHasStageAssessment() ); i++ ) {
            stage.addAssessment( loadFinding( st.getHasStageAssessment( i ) ) );
         }
         dx.setStage( stage );
      }

      // add mention text
      addMention( dx, e );
      addExtensions( dx, e );

      // add related items
      addEvidence( dx, e );


      FHIRUtils.createIdentifier( dx.addIdentifier(), e.getHasIdentifier() );
      return dx;
   }


   /**
    * add related evidence to a condition
    *
    * @param cond
    * @param e
    */
   private static void addEvidence( Condition cond, org.healthnlp.deepphe.uima.types.Fact e ) {
      // add related items
      Map<String, String> properties = loadProperties( e );
      if ( properties.containsKey( FHIRUtils.EVIDENCE ) ) {
         String str = properties.get( FHIRUtils.EVIDENCE );
         if ( str.startsWith( "[" ) && str.endsWith( "]" ) )
            str = str.substring( 1, str.length() - 1 );
         for ( String id : str.split( "," ) ) {
            cond.addEvidence( cTAKESUtils.createConditionEvidence( id ) );
         }
      }
   }


   private static CodeableConcept getCodeableConcept( org.healthnlp.deepphe.uima.types.Fact e ) {
      CodeableConcept cc = FHIRUtils.getCodeableConcept( e.getHasPreferredName(), e.getHasURI(), FHIRUtils.SCHEMA_OWL );
      if ( e.getHasLabel() != null )
         cc.setText( e.getHasLabel() );
      Coding c = cc.addCoding();
      c.setSystem( FHIRUtils.SCHEMA_REFERENCE );
      c.setCode( e.getHasIdentifier() );
      c.setDisplay( e.getHasPreferredName() );
      return cc;
   }


   private static org.healthnlp.deepphe.fhir.Patient loadPatient( Patient pt ) {
      if ( pt == null )
         return null;

      // add patient
      org.healthnlp.deepphe.fhir.Patient pp = new org.healthnlp.deepphe.fhir.Patient();
		/*for(int i=0;i<pt.getHasName().size();i++){
			HumanName hn = pt.getHasName(i);
			org.hl7.fhir.instance.model.HumanName nm = pp.addName();
			nm.addGiven(hn.getHasFirstName());
			nm.addFamily(hn.getHasLastName());
		}*/
      for ( int i = 0; i < getSize( pt.getHasName() ); i++ ) {
         HumanName hn = pt.getHasName( i );
         pp.setPatientName( hn.getHasFullName() );
      }
      //pp.setPatientName(pt.getHasName());

      try {
         // add gender
         for ( int i = 0; i < getSize( pt.getHasGender() ); i++ ) {
            pp.setGender( AdministrativeGender.fromCode( pt.getHasGender( i ).getHasPreferredName() ) );
         }

         // add birth date
         if ( pt.getHasBirthDate() != null )
            pp.setBirthDate( FHIRUtils.getDate( pt.getHasBirthDate() ) );

         // add death date
         if ( pt.getHasDeathDate() != null )
            pp.setDeceased( new BooleanType( true ) );


      } catch ( Exception e ) {
         e.printStackTrace();
      }

      // add identifier
      FHIRUtils.createIdentifier( pp.addIdentifier(), pt.getHasIdentifier() );

      // add mentions
      addMention( pp, pt );

      return pp;
   }

   public static Report loadReport( Composition comp ) {
      Report rr = new Report();

      // setup report
      rr.setIdentifier( FHIRUtils.createIdentifier( comp.getHasIdentifier() ) );
      rr.setText( comp.getCoveredText() );
      rr.setTitle( comp.getHasTitle() );
      if ( comp.getHasDateOfComposition() != null )
         rr.setDate( FHIRUtils.getDate( comp.getHasDateOfComposition() ) );
      for ( int i = 0; i < getSize( comp.getHasDocType() ); i++ ) {
         rr.setType( getCodeableConcept( comp.getHasDocType( i ) ) );
      }
      rr.setOffset( comp.getBegin() );

      // handle patient (there can be only one :)
      if ( comp.getHasPatient() != null ) {
         org.healthnlp.deepphe.fhir.Patient p = loadPatient( comp.getHasPatient( 0 ) );
         rr.setPatient( p );
      }

      // get related items
      for ( int i = 0; i < getSize( comp.getHasEvent() ); i++ ) {
         Element el = loadElement( comp.getHasEvent( i ) );
         if ( el != null )
            rr.addReportElement( el );

      }

      // load summaries
      for ( int i = 0; i < getSize( comp.getHasCompositionSummaryPatient() ); i++ ) {
         PatientSummary ps = loadPatientSummary( comp.getHasCompositionSummaryPatient( i ) );
         ps.setComposition( rr );
         rr.addCompositionSummary( ps );
      }
      for ( int i = 0; i < getSize( comp.getHasCompositionSummaryCancer() ); i++ ) {
         CancerSummary cs = loadCancerSummary( comp.getHasCompositionSummaryCancer( i ) );
         cs.setComposition( rr );
         rr.addCompositionSummary( cs );
      }
      for ( int i = 0; i < getSize( comp.getHasCompositionSummaryTumor() ); i++ ) {
         TumorSummary ps = loadTumorSummary( comp.getHasCompositionSummaryTumor( i ) );
         ps.setComposition( rr );
         rr.addCompositionSummary( ps );
      }

      // register this element
      FHIRRegistry.getInstance().addElement( rr, comp );

      return rr;
   }

   /**
    * get fact from annotation
    *
    * @param a
    * @return
    */

   private static Fact getFact( Annotation a ) {
      CodeableConcept cc = null;

      if ( a instanceof org.healthnlp.deepphe.uima.types.Fact ) {
         cc = getCodeableConcept( (org.healthnlp.deepphe.uima.types.Fact) a );
      }

      if ( cc != null ) {
         return FactFactory.createFact( cc );
      }

      return null;
   }

   private static int getSize( FSArray arr ) {
      return arr == null ? 0 : arr.size();
   }

   private static int getSize( StringArray arr ) {
      return arr == null ? 0 : arr.size();
   }

   /**
    * load cancer summary
    *
    * @param summaryAnnotation
    * @return
    */

   private static PatientSummary loadPatientSummary( org.healthnlp.deepphe.uima.types.PatientSummary summaryAnnotation ) {
      PatientSummary patientSummary = new PatientSummary();

      // add generic values
      for ( int i = 0; i < getSize( summaryAnnotation.getHasContent() ); i++ ) {
         FactList flist = loadFactList( summaryAnnotation.getHasContent( i ) );
         patientSummary.getContent().put( flist.getCategory(), flist );
      }

      // add phenotypes
      for ( int i = 0; i < getSize( summaryAnnotation.getHasPhenotype() ); i++ ) {
         org.healthnlp.deepphe.uima.types.PatientPhenotype pheneAnnotation = summaryAnnotation.getHasPhenotype( i );
         PatientPhenotype phenotype = new PatientPhenotype();

         // add generic values
         for ( int j = 0; j < getSize( pheneAnnotation.getHasContent() ); j++ ) {
            FactList flist = loadFactList( pheneAnnotation.getHasContent( j ) );
            phenotype.getContent().put( flist.getCategory(), flist );
         }
         patientSummary.setPhenotype( phenotype );
      }

      // load properties
      patientSummary.setProperties( loadProperties( summaryAnnotation ) );

      FHIRUtils.createIdentifier( patientSummary.addIdentifier(), summaryAnnotation.getHasIdentifier() );
      return patientSummary;
   }


   /**
    * load cancer summary
    *
    * @param summaryAnnotation
    * @return
    */

   private static CancerSummary loadCancerSummary( Cancer summaryAnnotation ) {
      CancerSummary cancerSummary = new CancerSummary( summaryAnnotation.getHasIdentifier() );

      // add generic values
      for ( int i = 0; i < getSize( summaryAnnotation.getHasContent() ); i++ ) {
         FactList flist = loadFactList( summaryAnnotation.getHasContent( i ) );
         cancerSummary.getContent().put( flist.getCategory(), flist );
      }

      // add phenotypes
      for ( int i = 0; i < getSize( summaryAnnotation.getHasPhenotype() ); i++ ) {
         CancerPhenotype pheneAnnotation = summaryAnnotation.getHasPhenotype( i );
         org.healthnlp.deepphe.fhir.summary.CancerPhenotype phenotype = new org.healthnlp.deepphe.fhir.summary.CancerPhenotype();
         phenotype.setResourceIdentifier( pheneAnnotation.getHasIdentifier() );

         // add generic values
         for ( int j = 0; j < getSize( pheneAnnotation.getHasContent() ); j++ ) {
            FactList flist = loadFactList( pheneAnnotation.getHasContent( j ) );
            phenotype.getContent().put( flist.getCategory(), flist );
         }
         cancerSummary.setPhenotype( phenotype );
      }

      // add tumors
      for ( int i = 0; i < getSize( summaryAnnotation.getRealizes() ); i++ ) {
         cancerSummary.addTumor( loadTumorSummary( summaryAnnotation.getRealizes( i ) ) );
      }

      // load properties
      cancerSummary.setProperties( loadProperties( summaryAnnotation ) );

      FHIRUtils.createIdentifier( cancerSummary.addIdentifier(), summaryAnnotation.getHasIdentifier() );
      return cancerSummary;
   }


   private static TumorSummary loadTumorSummary( Tumor summaryAnnotation ) {
      TumorSummary tumorSummary = new TumorSummary( summaryAnnotation.getHasIdentifier() );

      // add generic values
      for ( int i = 0; i < getSize( summaryAnnotation.getHasContent() ); i++ ) {
         FactList flist = loadFactList( summaryAnnotation.getHasContent( i ) );
         tumorSummary.getContent().put( flist.getCategory(), flist );
      }

      // add phenotypes
      for ( int i = 0; i < getSize( summaryAnnotation.getHasPhenotype() ); i++ ) {
         org.healthnlp.deepphe.uima.types.TumorPhenotype pheneAnnotation = summaryAnnotation.getHasPhenotype( i );
         TumorPhenotype phenotype = new TumorPhenotype();
         phenotype.setResourceIdentifier( pheneAnnotation.getHasIdentifier() );
         // add generic values
         for ( int j = 0; j < getSize( pheneAnnotation.getHasContent() ); j++ ) {
            FactList flist = loadFactList( pheneAnnotation.getHasContent( j ) );
            phenotype.getContent().put( flist.getCategory(), flist );
         }
         tumorSummary.setPhenotype( phenotype );
      }

      // add episodes and its composition
      for ( int i = 0; i < getSize( summaryAnnotation.getHasEpisode() ); i++ ) {
         tumorSummary.addEpisode( loadEpisode( summaryAnnotation.getHasEpisode( i ) ) );
      }

      // load properties
      tumorSummary.setProperties( loadProperties( summaryAnnotation ) );

      FHIRUtils.createIdentifier( tumorSummary.addIdentifier(), summaryAnnotation.getHasIdentifier() );
      return tumorSummary;
   }

   /**
    * load episode information
    *
    * @param episodeAnnotation
    * @return
    */

   private static Episode loadEpisode( org.healthnlp.deepphe.uima.types.Episode episodeAnnotation ) {
      Episode episode = new Episode();
      episode.setResourceIdentifier( episodeAnnotation.getHasIdentifier() );
      episode.setStartDate( FHIRUtils.getDate( episodeAnnotation.getHasStartDate() ) );
      episode.setEndDate( FHIRUtils.getDate( episodeAnnotation.getHasEndDate() ) );
      episode.setType( episodeAnnotation.getHasType() );
      //episode.setEpisodeType(episodeType);
      //TODO: add primary vs recurrent which is episode type
      for ( int j = 0; j < getSize( episodeAnnotation.getHasEventComposition() ); j++ ) {
         Composition r = episodeAnnotation.getHasEventComposition( j );
         Report report = (Report) FHIRRegistry.getInstance().getElement( r );
         if ( report != null ) {
            episode.addReports( report );
         }
      }
      return episode;
   }

   private static org.healthnlp.deepphe.uima.types.Episode saveEpisode( JCas jcas, Episode episode ) {
      Annotation a = getAnnotationByIdentifier( jcas, episode.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.Episode episodeAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.Episode( jcas ) : (org.healthnlp.deepphe.uima.types.Episode) a;
      episodeAnnotation.setHasIdentifier( episode.getResourceIdentifier() );
      episodeAnnotation.setHasStartDate( "" + episode.getStartDate() );
      episodeAnnotation.setHasEndDate( "" + episode.getEndDate() );
      episodeAnnotation.setHasType( episode.getType() );

      List<FeatureStructure> list = new ArrayList<FeatureStructure>();
      for ( Report r : episode.getReports() ) {
         Annotation rr = getAnnotationByIdentifier( jcas, r.getResourceIdentifier() );
         if ( rr != null )
            list.add( rr );
      }
      episodeAnnotation.setHasEventComposition( getValues( jcas, list ) );
      episodeAnnotation.addToIndexes();
      return episodeAnnotation;
   }

   /**
    * load a single patient mention froma a cas (there can only be one)
    *
    * @param cas
    * @return
    */
   public static org.healthnlp.deepphe.fhir.Patient loadPatient( JCas cas ) {
      Patient p = null;
      for ( Annotation a : getAnnotations( cas, Patient.type ) ) {
         p = (Patient) a;
         break;
      }
      return p != null ? loadPatient( p ) : null;
   }


   public static List<Report> loadReports( JCas cas ) {
      List<Report> reports = new ArrayList<Report>();
      Set<Annotation> list = new HashSet<Annotation>();
      for ( Annotation a : getAnnotations( cas, Composition.type ) ) {
         if ( !list.contains( a ) ) {
            Report r = loadReport( (Composition) a );
            reports.add( r );
            list.add( a );
         }
      }
      return reports;
   }

   /**
    * create medical record
    *
    * @param record
    * @param jcas
    */
   public static org.healthnlp.deepphe.uima.types.MedicalRecord saveMedicalRecord( MedicalRecord record, JCas jcas ) {
      Annotation a = getAnnotationByIdentifier( jcas, record.getResourceIdentifier() );
      org.healthnlp.deepphe.uima.types.MedicalRecord summaryAnnotation = (a == null) ? new org.healthnlp.deepphe.uima.types.MedicalRecord( jcas ) : (org.healthnlp.deepphe.uima.types.MedicalRecord) a;

      summaryAnnotation.setBegin( 0 );
      summaryAnnotation.setEnd( jcas.getDocumentText().length() );
      summaryAnnotation.setHasURI( "" + record.getConceptURI() );
      summaryAnnotation.setHasIdentifier( record.getResourceIdentifier() );
      summaryAnnotation.setHasPreferredName( record.getDisplayText() );
      summaryAnnotation.setHasType( FHIRConstants.MEDICAL_RECORD );

      if ( record.getPatient() != null ) {
         org.healthnlp.deepphe.uima.types.Patient patientAnnotation = savePatient( record.getPatient(), jcas );
         summaryAnnotation.setHasPatient( getValue( jcas, patientAnnotation ) );
      }

      if ( record.getPatientSummary() != null ) {
         org.healthnlp.deepphe.uima.types.PatientSummary patientAnnotation = savePatientSummary( record.getPatientSummary(), jcas );
         summaryAnnotation.setHasMedicalRecordSummaryPatient( getValue( jcas, patientAnnotation ) );
      }

      if ( record.getCancerSummary() != null ) {
         Cancer cancerAnnoation = saveCancerSummary( record.getCancerSummary(), jcas );
         summaryAnnotation.setHasMedicalRecordSummaryCancer( getValue( jcas, cancerAnnoation ) );
      }

      List<FeatureStructure> list = new ArrayList<FeatureStructure>();
      for ( Report r : record.getReports() ) {
         Composition c = (Composition) getAnnotationByIdentifier( jcas, r.getResourceIdentifier() );
         if ( c != null )
            list.add( c );
      }
      summaryAnnotation.setHasComposition( getValues( jcas, list ) );
      summaryAnnotation.addToIndexes();

      return summaryAnnotation;
   }


   public static MedicalRecord loadMedicalRecord( JCas jcas ) {
      org.healthnlp.deepphe.uima.types.MedicalRecord annotationRecord = null;
      for ( Annotation a : getAnnotations( jcas, org.healthnlp.deepphe.uima.types.MedicalRecord.type ) ) {
         annotationRecord = (org.healthnlp.deepphe.uima.types.MedicalRecord) a;
      }

      MedicalRecord record = new MedicalRecord();

      // add reports
      record.setReports( loadReports( jcas ) );
      if ( annotationRecord != null ) {
         for ( int i = 0; i < getSize( annotationRecord.getHasMedicalRecordSummaryCancer() ); i++ ) {
            record.setCancerSummary( loadCancerSummary( annotationRecord.getHasMedicalRecordSummaryCancer( i ) ) );
         }
         for ( int i = 0; i < getSize( annotationRecord.getHasMedicalRecordSummaryPatient() ); i++ ) {
            record.setPatientSummary( loadPatientSummary( annotationRecord.getHasMedicalRecordSummaryPatient( i ) ) );
         }
      }
      record.setPatient( loadPatient( jcas ) );

      return record;

   }

}
