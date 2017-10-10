package org.healthnlp.deepphe.fhir.fact;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.healthnlp.deepphe.fhir.AnatomicalSite;
import org.healthnlp.deepphe.fhir.Condition;
import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Finding;
import org.healthnlp.deepphe.fhir.Observation;
import org.healthnlp.deepphe.fhir.Procedure;
import org.healthnlp.deepphe.fhir.Stage;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRRegistry;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.hl7.fhir.instance.model.BackboneElement;
import org.hl7.fhir.instance.model.BodySite;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.DomainResource;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Condition.ConditionEvidenceComponent;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;


/**
 * create different facts
 *
 * @author tseytlin
 */
public class FactFactory {

   /**
    * return codeable concept representation of a given Fact
    *
    * @param fact
    * @return
    */
   public static CodeableConcept createCodeableConcept( Fact fact ) {
      CodeableConcept c = FHIRUtils.getCodeableConcept( fact.getLabel(), fact.getUri(), FHIRUtils.SCHEMA_OWL );
      if ( fact.getIdentifier() != null ) {
         Coding coding = c.addCoding();
         coding.setCode( fact.getIdentifier() );
         coding.setDisplay( fact.getLabel() );
         coding.setSystem( FHIRUtils.SCHEMA_REFERENCE );
      }
      return c;
   }

   /**
    * create a generic fact based on a codeable concept
    */
   public static Fact createFact( CodeableConcept cc ) {
      if ( cc == null )
         return null;

      // do we have an element of this CC registered, then make a fact based on that
      Element e = FHIRRegistry.getInstance().getElement( FHIRUtils.getResourceIdentifer( cc ) );
      if ( e != null )
         return FactFactory.createFact( e );

      // else do a default operation
      Fact fact = new Fact();

      // unless we have an ontology :)
      if ( OntologyUtils.hasInstance() ) {
         URI uri = FHIRUtils.getConceptURI( cc );
         if ( uri != null ) {
            IOntology ontology = OntologyUtils.getInstance().getOntology();
            IClass cls = ontology.getClass( "" + uri );
            if ( cls != null ) {
               if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.OBSERVATION_URI ) ) )
                  fact = new ObservationFact();
               else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.CONDITION_URI ) ) )
                  fact = new ConditionFact();
               else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.BODY_SITE_URI ) ) )
                  fact = new BodySiteFact();
               else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.PROCEDURE_URI ) ) )
                  fact = new ProcedureFact();
            }
         }
      }
      return createFact( cc, fact );
   }


   public static void addAncestors( Fact fact ) {
      if ( OntologyUtils.hasInstance() ) {
         OntologyUtils.getInstance().addAncestors( fact );
         for ( Fact f : fact.getContainedFacts() ) {
            OntologyUtils.getInstance().addAncestors( f );
         }
      }
   }

   public static void addLabel( Fact fact ) {
      if ( OntologyUtils.hasInstance() ) {
         IOntology ont = OntologyUtils.getInstance().getOntology();
         IClass cls = ont.getClass( fact.getUri() );
         if ( cls != null ) {
            String prefTerm = (String) cls.getPropertyValue( ont.getProperty( FHIRConstants.PREF_TERM ) );
            if ( prefTerm == null ) {
               prefTerm = cls.getLabel();
            }
            if ( prefTerm != null )
               fact.setLabel( prefTerm );
         }
      }
   }


   /**
    * create a generic fact based on a codeable concept
    */
   public static Fact createFact( CodeableConcept cc, Fact fact ) {
      URI uri = FHIRUtils.getConceptURI( cc );
      String id = FHIRUtils.getResourceIdentifer( cc );
      fact.setUri( "" + uri );
      fact.setLabel( cc.getText() );

      if ( uri != null ) {
         try {
            fact.setName( uri.toURL().getRef() );
         } catch ( MalformedURLException e ) {
            throw new Error( e );
         }
      } else {
         fact.setName( fact.getLabel() );
      }
      if ( id != null )
         fact.setIdentifier( id );

      addLabel( fact );
      addAncestors( fact );

      return fact;

   }


   public static TextMention createTextMention( String mention ) {
      int[] se = FHIRUtils.getMentionSpan( mention );
      String text = FHIRUtils.getMentionText( mention );
      TextMention tm = new TextMention();
      tm.setText( text );
      tm.setStart( se[ 0 ] );
      tm.setEnd( se[ 1 ] );
      return tm;
   }

   public static Fact createFact( Element resource ) {
      if ( resource instanceof Observation )
         return createFact( (Observation) resource );
      if ( resource instanceof AnatomicalSite )
         return createFact( (AnatomicalSite) resource );
      if ( resource instanceof Finding && OntologyUtils.hasInstance() && OntologyUtils.getInstance().hasSuperClass( resource, FHIRConstants.TNM_STAGE ) )
         return createTNMFact( (Finding) resource );
      if ( resource instanceof Condition )
         return createFact( (Condition) resource );
      if ( resource instanceof Procedure )
         return createFact( (Procedure) resource );
      if ( resource instanceof Quantity )
         return createFact( (Quantity) resource );

      return createFact( resource, new Fact() );
   }

   private static Fact createTNMFact( Finding tnm ) {
      TNMFact fact = (TNMFact) createFact( tnm, new TNMFact() );
      for ( String mod : FHIRUtils.getProperty( tnm, FHIRUtils.TNM_MODIFIER_URL ) ) {
         Fact f = createFact( FHIRUtils.getCodeableConcept( URI.create( mod ) ) );
         f.setType( FHIRConstants.TNM_MODIFIER );
         f.setCategory( FHIRConstants.HAS_TNM_PREFIX );
         fact.setPrefix( f );
      }
      return fact;
   }

   private static Fact createFact( Element resource, Fact fact ) {
      fact = createFact( resource.getCode(), fact );
      fact.setIdentifier( resource.getResourceIdentifier() );
      //fact.setLabel(label);
      for ( String m : FHIRUtils.getMentionExtensions( (DomainResource) resource.getResource() ) ) {
         TextMention mention = createTextMention( m );
         if ( resource.getComposition() != null ) {
            mention.setDocumentIdentifier( resource.getComposition().getResourceIdentifier() );
            mention.setDocumentTitle( resource.getComposition().getTitle() );
            mention.setDocumentType( FHIRUtils.getDocumentType( resource.getComposition().getType() ) );
         }
         fact.addProvenanceText( mention );
      }
      fact.setProperties( FHIRUtils.getProperties( (DomainResource) resource ) );

      return fact;
   }


   /**
    * create Fact from quantity
    *
    * @param q
    * @return
    */
   public static ValueFact createFact( Quantity q ) {
      // get default values
      double[] values = new double[]{ q.getValue().doubleValue() };
      // try to get dimensions through extensions
      List<String> dims = FHIRUtils.getProperty( q, FHIRUtils.DIMENSION_URL );
      if ( !dims.isEmpty() ) {
         values = new double[ dims.size() ];
         for ( int i = 0; i < values.length; i++ ) {
            values[ i ] = Double.parseDouble( dims.get( i ) );
         }
      }
      return createValueFact( values, q.getUnit() );
   }

   /**
    * create Fact from quantity
    *
    * @param q
    * @return
    */
   public static ValueFact createValueFact( double value, String units ) {
      return createValueFact( new double[]{ value }, units );
   }

   /**
    * create Fact from quantity
    *
    * @param q
    * @return
    */
   public static ValueFact createValueFact( double[] value, String units ) {
      URI uri = FHIRConstants.QUANTITY_URI;
      String name = FHIRConstants.QUANTITY;
      ;

      ValueFact fact = new ValueFact();
      fact.setName( name );
      fact.setUri( "" + uri );
      fact.setType( name );
      fact.setValues( value );
      fact.setUnit( units );
      String label = fact.getSummaryText();
      fact.setLabel( label );
      fact.setIdentifier( name.toUpperCase() + "_" + label );

      return fact;
   }


   /**
    * create observation fact
    *
    * @param ob
    * @return
    */
   public static ObservationFact createFact( Observation ob ) {
      ObservationFact fact = (ObservationFact) createFact( ob, new ObservationFact() );
      if ( FHIRUtils.hasConceptURI( ob.getInterpretation() ) ) {
         Fact f = createFact( ob.getInterpretation() );
         f.setType( FHIRConstants.ORDINAL_INTERPRETATION );
         f.setCategory( FHIRConstants.HAS_INTERPRETATION );
         fact.setInterpretation( f );
      }
      if ( ob.getValue() != null && ob.getValue() instanceof Quantity ) {
         try {
            ValueFact f = createFact( ob.getValueQuantity() );
            f.setCategory( FHIRConstants.HAS_NUM_VALUE );
            fact.setValue( UnitConverter.normalizeToUnits( f, fact ) );
         } catch ( Exception e ) {
            throw new Error( e );
         }
      }
      if ( FHIRUtils.hasConceptURI( ob.getMethod() ) ) {
         Fact f = createFact( ob.getMethod() );
         f.setType( FHIRConstants.PROCEDURE );
         f.setCategory( FHIRConstants.HAS_METHOD );
         fact.setMethod( f );
      }
      return fact;
   }

   /**
    * create observation fact
    *
    * @param ob
    * @return
    */
   public static ConditionFact createFact( Condition condition ) {
      ConditionFact fact = (ConditionFact) createFact( condition, new ConditionFact() );
      for ( CodeableConcept cc : condition.getBodySite() ) {
         Fact f = createFact( cc );
         f.setType( FHIRConstants.BODY_SITE );
         f.setCategory( FHIRConstants.HAS_BODY_SITE );
         fact.getBodySite().add( f );
      }

      // add related evidence
      fact.setRelatedEvidenceIds( condition.getRelatedEvidenceIdentifiers() );

      return fact;
   }


   /**
    * create observation fact
    *
    * @param ob
    * @return
    */
   public static ProcedureFact createFact( Procedure condition ) {
      ProcedureFact fact = (ProcedureFact) createFact( condition, new ProcedureFact() );
      for ( CodeableConcept cc : condition.getBodySite() ) {
         Fact f = createFact( cc );
         f.setType( FHIRConstants.BODY_SITE );
         f.setCategory( FHIRConstants.HAS_BODY_SITE );
         fact.getBodySite().add( f );
      }
      //TODO: handle method
      return fact;
   }

   /**
    * create observation fact
    *
    * @param ob
    * @return
    */
   public static BodySiteFact createFact( AnatomicalSite location ) {
      BodySiteFact fact = (BodySiteFact) createFact( location, new BodySiteFact() );
      for ( CodeableConcept cc : location.getModifier() ) {
         Fact modifier = createFact( cc );
         modifier.setCategory( FHIRConstants.HAS_BODY_MODIFIER );
         modifier.setType( isBodySide( modifier ) ? FHIRConstants.LATERALITY : FHIRConstants.BODY_MODIFIER );
         fact.addModifier( modifier );
      }
      return fact;
   }

   public static boolean isBodySide( Fact modifier ) {
      if ( OntologyUtils.hasInstance() ) {
         return OntologyUtils.getInstance().hasSuperClass( modifier, FHIRConstants.LATERALITY );
      }
      return FHIRConstants.BODY_SIDE_LIST.contains( modifier.getName() );
   }

   /**
    * create empty fact of a given type
    *
    * @param type
    * @return
    */
   public static Fact createFact( String type ) {
      Fact fact = null;
      if ( FHIRConstants.BODY_SITE.equals( type ) ) {
         fact = new BodySiteFact();
      } else if ( FHIRConstants.OBSERVATION.equals( type ) ) {
         fact = new ObservationFact();
      } else if ( FHIRConstants.CONDITION.equals( type ) ) {
         fact = new ConditionFact();
      } else if ( type.endsWith( FHIRConstants.PROCEDURE ) ) {
         fact = new ProcedureFact();
      } else if ( FHIRConstants.QUANTITY.equals( type ) ) {
         fact = new ValueFact();
      } else {
         fact = new Fact();
      }
      return fact;
   }

   /**
    * create empty fact of a given type
    *
    * @param type
    * @return
    */
   public static Fact createFact( String type, String uri ) {
      return createFact( FHIRUtils.getCodeableConcept( URI.create( uri ) ), createFact( type ) );
   }

   /**
    * create fact and copy most of the parameters from the old one
    *
    * @param oldF
    * @param type
    * @param uri
    * @return
    */
   public static Fact createFact( Fact oldFact, String type, String uri, String newDocType ) {
      Fact newFact = createFact( FHIRUtils.getCodeableConcept( URI.create( uri ) ), createFact( type ) );
      newFact.setAncestors( oldFact.getAncestors() );
      newFact.setCategory( oldFact.getCategory() );
      newFact.setPatientIdentifier( oldFact.getPatientIdentifier() );
      newFact.setSummaryType( oldFact.getSummaryType() );
      newFact.setSummaryId( oldFact.getSummaryType() + "_" + oldFact.getName() );
      newFact.setDocumentType( newDocType );

      return newFact;
   }

   public static String createIdentifier( Fact fact ) {
      return fact.getType() + "_" + fact.getName().replaceAll( "\\W+", "_" ) + "_" + Math.abs( fact.getProvenanceMentions().hashCode() );
   }

   /**
    * create fact from information of a different fact
    * (without cloning provenance or attributes)
    *
    * @param f
    * @return
    */
   public static Fact createFact( Fact f ) {
      Fact fact = createFact( f.getType() );
      fact.setUri( f.getUri() );
      fact.setName( f.getName() );
      fact.setLabel( f.getLabel() );
      fact.setIdentifier( "Fact_" + fact.getName() + "_" + System.currentTimeMillis() );
      fact.addPropeties( f.getProperties() );
      fact.setDocumentIdentifier( f.getDocumentIdentifier() );
      fact.setDocumentTitle( f.getDocumentTitle() );
      fact.setDocumentType( f.getDocumentType() );
      fact.setPatientIdentifier( f.getPatientIdentifier() );

      return fact;
   }

   public static Fact createTumorFactModifier( String uri, Fact tSummaryF, Fact cSummaryF, String summaryType,
                                               String category, String documentType, String type ) {
      Fact f = FactFactory.createFact( type, uri );

      //f.addProvenanceFact(cSummaryF);
      //f.addProvenanceFact(tSummaryF);

      f.addContainerIdentifier( cSummaryF.getSummaryId() );
      f.addContainerIdentifier( tSummaryF.getSummaryId() );


      f.setCategory( category );
      f.setPatientIdentifier( tSummaryF.getPatientIdentifier() );
      f.setSummaryType( summaryType );
      f.setType( type );

      String name = f.getName();
      f.setSummaryId( tSummaryF.getSummaryId() + "_" + name );
      f.setIdentifier( tSummaryF.getIdentifier() + "-" + name );
      f.setDocumentType( documentType );

      return f;
   }

}
