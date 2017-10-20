package org.healthnlp.deepphe.uima.fhir;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFetcher;
import org.apache.ctakes.cancer.concept.instance.ConceptRelationUtil;
import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SeverityModifier;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.fhir.*;
import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Medication;
import org.healthnlp.deepphe.fhir.Observation;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Procedure;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.healthnlp.deepphe.util.TextUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Condition.ConditionStageComponent;
import org.hl7.fhir.instance.model.Procedure.ProcedureStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final public class DocumentResourceFactory {

   static private final Logger LOGGER = Logger.getLogger( "DocumentResourceFactory" );

   static private class SingletonHolder {
      static private DocumentResourceFactory _instance;

      synchronized static private void setInstance( final DocumentResourceFactory instance ) {
         if ( _instance != null ) {
            LOGGER.error( "DocumentResourceFactory singleton being overwritten!" );
         }
         _instance = instance;
      }

      synchronized static private DocumentResourceFactory getInstance() {
         if ( _instance == null ) {
            LOGGER.error( "DocumentResourceFactory singleton being referenced before creation!" );
         }
         return _instance;
      }
   }

   // TODO - made this a little more threadsafe "singleton"
   // private static DocumentResourceFactory instance;
   private IOntology ontology;
   private OntologyUtils ontologyUtils;

   // public DocumentResourceFactory(IOntology ont){
   // ontology = ont;
   // instance = this;
   // }

   static {
      try {
         OntologyUtils.getInstance( OwlConnectionFactory.getInstance().getDefaultOntology() );
      } catch ( FileNotFoundException | IOntologyException e ) {
         LOGGER.error( "DocumentResourceFactory problem initializing domain ontology " + e.getMessage() );
      }
   }

   static public void createInstance( final IOntology iOntology ) {
      SingletonHolder.setInstance( new DocumentResourceFactory( iOntology ) );
   }

   public static DocumentResourceFactory getInstance() {
      // return instance;
      return SingletonHolder.getInstance();
   }

   private DocumentResourceFactory( final IOntology iOntology ) {
      ontology = iOntology;
   }

   public IOntology getOntology() {
      return ontology;
   }

   public void setOntology( IOntology ontology ) {
      this.ontology = ontology;
   }

   public OntologyUtils getOntologyUtils() {
      if ( ontologyUtils == null && ontology != null ) {
         ontologyUtils = OntologyUtils.getInstance( ontology );
      }
      return ontologyUtils;
   }

   /**
    * create a Report object from DocumentAnnotation
    *
    * @param cas -
    * @return -
    */
   public static Report getReport( JCas cas ) {
//      Report r = getReport( cTAKESUtils.getDocumentText( cas ) );
      Report r = createReport( cas );

      // oh, well no document title available
      String title;
      try {
         title = DocumentIDAnnotationUtil.getDocumentIdForFile( cas );
      } catch ( Exception e ) {
         e.printStackTrace();
         title = "Untitled";
      }
      if ( title != null ) {
         r.setTitle( TextUtils.stripSuffix( title ) );
      }
      // find patient if available
      Patient patient = getPatient( cas );
      if ( patient != null ) {
         r.setPatient( patient );
      }

      // now find all anatomical site found in a report
      getAnatomicalSite( cas ).forEach( r::addReportElement );

      // now find all observations found in a report
      getObservations( cas ).forEach( r::addReportElement );

      // now find all observations found in a report
      getFindings( cas ).forEach( r::addReportElement );

      // find all procedures mentioned in each report
      getProcedures( cas ).forEach( r::addReportElement );

      // now find all observations found in a report
      getMedications( cas ).forEach( r::addReportElement );

      // now find all primary disease that are found in a report
      getDiagnoses( cas ).forEach( r::addReportElement );

      return r;
   }

   /**
    * create a Report object from DocumentAnnotation
    *
    * @param text -
    * @return -
    */
   public static Report getReport( String text ) {
      if ( text == null )
         return null;

      Report r = new Report();
      r.setText( text );
      // some hard-coded report type values
      Map<String, String> header = FHIRUtils.getHeaderValues( text );
      String dt = header.get( FHIRUtils.DOCUMENT_HEADER_PRINCIPAL_DATE );
      if ( dt != null )
         r.setDate( FHIRUtils.getDate( dt ) );
      String tp = header.get( FHIRUtils.DOCUMENT_HEADER_REPORT_TYPE );
      if ( tp != null )
         r.setType( tp );
      return r;
   }

   static private final  Collection<String> DOC_TYPES = Arrays.asList( "SP", "RAD", "DS", "PGN", "NOTE" );

   /**
    * create a Report object from DocumentAnnotation
    *
    * @param jCas -
    * @return -
    */
   public static Report createReport( final JCas jCas ) {
      final String docText = jCas.getDocumentText();
      Report r = new Report();
      r.setText( docText );
      // some hard-coded report type values
      Map<String, String> header = FHIRUtils.getHeaderValues( docText );
      String dt = header.get( FHIRUtils.DOCUMENT_HEADER_PRINCIPAL_DATE );
      if ( dt != null ) {
         r.setDate( FHIRUtils.getDate( dt ) );
      } else {
         r.setDate( java.sql.Date.valueOf( LocalDate.now() ) );
      }
      String type = header.get( FHIRUtils.DOCUMENT_HEADER_REPORT_TYPE );
      if ( type == null ) {
         final String id = DocumentIDAnnotationUtil.getDocumentID( jCas );
         final String[] splits = id.split( "_" );
         for ( String split : splits ) {
            if ( DOC_TYPES.contains( split ) ) {
               type = split;
               break;
            }
         }
      }
      if ( type == null ) {
         type = "NOTE";
      }
      r.setType( type );
      return r;
   }


   public static Patient getPatient( JCas cas ) {
//      Patient p = getPatient( cTAKESUtils.getDocumentText( cas ) );
//      // TODO: age and gender
//
//      return p;
      return createPatient( cas );
   }

   /**
    * get patient from the document
    *
    * @param p -
    * @return -
    */
   public static Patient getPatient( org.hl7.fhir.instance.model.Patient p ) {
      Patient patient = new Patient();
      patient.copy( p );
      cTAKESUtils.addResource( patient );
      return patient;
   }

   /**
    * get patient from the document
    *
    * @param text -
    * @return -
    */
   public static Patient getPatient( String text ) {
      if ( text == null )
         return null;
      Map<String, String> header = FHIRUtils.getHeaderValues( text );
      String pn = header.get( FHIRUtils.DOCUMENT_HEADER_PATIENT_NAME );
      if ( pn != null ) {
         Patient p = new Patient();
         p.setPatientName( pn );
         int n = text.indexOf( pn );
         p.addExtension( FHIRUtils.createMentionExtension( pn, n, n + pn.length() ) );

         // register
         cTAKESUtils.addResource( p );

         return p;
      }
      return null;
   }

   public static Patient createPatient( JCas cas ) {
      final String docText = cas.getDocumentText();
      final Map<String, String> header = FHIRUtils.getHeaderValues( docText );
      if ( header != null && !header.isEmpty() ) {
         final String name = header.get( FHIRUtils.DOCUMENT_HEADER_PATIENT_NAME );
         if ( name != null && !name.isEmpty() ) {
            return createPatient( name, docText.indexOf( name ) );
         }
      }
      final String name = DocumentIDAnnotationUtil.getDocumentIdPrefix( cas );
      return createPatient( name, 0-name.length() );
   }

   /**
    * get patient from the document
    *
    * @param name - patient name
    * @param nameIndex of text
    * @return fhir patient
    */
   public static Patient createPatient( final String name, final int nameIndex ) {
      Patient p = new Patient();
      p.setPatientName( name );
      if ( nameIndex >= 0 ) {
         p.addExtension( FHIRUtils.createMentionExtension( name, nameIndex, nameIndex + name.length() ) );
      }
      // register
      cTAKESUtils.addResource( p );
      // TODO: age and gender
      return p;
   }

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param c -
    * @return -
    */
   public IClass getConceptClass( Concept c ) {
      String code = c.getCode();
      if ( code.contains( ":" ) )
         code = code.substring( code.indexOf( ':' ) + 1 );
      return ontology.getClass( code );
   }

   // TODO - added method to simplify specific Element list creation

   /**
    * Obtain a list of Elements of type T in the given cas with the given uri
    *
    * @param jcas   ye olde ...
    * @param uri    uri for the identified annotation
    * @param mapper creates the desired output class T from an identified
    *               annotation
    * @param <T>    some extension of fhir element
    * @return list of Elements of type T in the given cas with the given uri
    */
   static private <T extends Element> List<T> getElementList( final JCas jcas, final URI uri,
                                                              final Function<ConceptInstance, ? extends T> mapper ) {

      return ConceptInstanceFactory.createBranchConceptInstances( jcas, uri.toString() ).stream()
            .filter( ci -> !ci.isNegated() ).filter( notSeverity ).map( mapper ).collect( Collectors.toList() );

   }

   static private final Predicate<ConceptInstance> isSeverity = ci -> ci.getAnnotations().stream()
         .anyMatch( SeverityModifier.class::isInstance );

   static private final Predicate<ConceptInstance> notSeverity = ci -> !ci.getAnnotations().stream()
         .anyMatch( SeverityModifier.class::isInstance );

   /**
    * Obtain a list of Values for Elements of type T in the given cas with the
    * given uri
    *
    * @param jcas   ye olde ...
    * @param uri    uri for the identified annotation
    * @param mapper creates the desired output class T from an identified
    *               annotation
    * @param <T>    some extension of fhir element
    * @return list of Elements of type T in the given cas with the given uri
    */
   static private <T extends Element> List<T> getValueElementList( final JCas jcas, final URI uri,
                                                                   final Function<ConceptInstance, ? extends T> mapper ) {
//		return ConceptInstanceFactory.createBranchConceptInstances(jcas, uri.toString()).stream()
      return ConceptInstanceFetcher.getConceptInstances( jcas, uri.toString() ).stream()
            .map( DocumentResourceFactory::getValuesOrSame ).flatMap( Collection::stream )
            .filter( t -> !t.isNegated() && !t.isGeneric() ).map( mapper ).collect( Collectors.toList() );
   }

   /**
    * @param finding finding or observation that may or may not be a phenotype with
    *                associated values
    * @return the associated values or the original finding if there are none
    */
   static private Collection<ConceptInstance> getValuesOrSame( final ConceptInstance finding ) {
      final Collection<ConceptInstance> values = finding.getValues();
      if ( values.isEmpty() ) {
         return Collections.singletonList( finding );
      }
      return values;
   }

   public static List<Procedure> getProcedures( JCas cas ) {
      return getElementList( cas, FHIRConstants.PROCEDURE_URI, DocumentResourceFactory::createProcedure );
   }

   public static List<Disease> getDiagnoses( JCas cas ) {
      return getElementList( cas, FHIRConstants.DIAGNOSIS_URI, DocumentResourceFactory::createDiagnosis );
   }

   public static List<Medication> getMedications( JCas cas ) {
      return getElementList( cas, FHIRConstants.MEDICATION_URI, DocumentResourceFactory::createMedication );
   }

   public static List<Finding> getFindings( JCas cas ) {
      return getElementList( cas, FHIRConstants.FINDING_URI, DocumentResourceFactory::createFinding );
      // return getValueElementList( cas, FHIRConstants.FINDING_URI,
      // DocumentResourceFactory::createFinding );
   }

   public static List<AnatomicalSite> getAnatomicalSite( JCas cas ) {
      return getElementList( cas, FHIRConstants.BODY_SITE_URI, DocumentResourceFactory::createAnatomicalSite );
   }

   public static List<Observation> getObservations( JCas cas ) {
      return ConceptInstanceFetcher.getObservations( cas ).stream().filter( ci -> !ci.isNegated() )
            .map( DocumentResourceFactory::createObservation ).collect( Collectors.toList() );
   }

   private static boolean isType( File f, Class cls ) {
      return f.getName().endsWith( ".xml" ) && f.getName().startsWith( cls.getSimpleName().toUpperCase() );
   }

   public static Report getReport( Composition c ) {
      if ( c == null )
         return null;
      Report report = new Report();
      report.copy( c );
      cTAKESUtils.addResource( report );
      return report;
   }

   public static Finding getFinding( Condition c ) {
      if ( c == null )
         return null;
      Finding d = new Finding();
      d.copy( c );
      cTAKESUtils.addResource( d );
      return d;
   }

   public static Disease getDiagnosis( Condition c ) {
      if ( c == null )
         return null;
      Disease d = new Disease();
      d.copy( c );
      cTAKESUtils.addResource( d );
      return d;
   }

   public static Procedure getProcedure( org.hl7.fhir.instance.model.Procedure p ) {
      if ( p == null )
         return null;
      Procedure pp = new Procedure();
      pp.copy( p );
      cTAKESUtils.addResource( pp );
      return pp;
   }

   public static Observation getObservation( org.hl7.fhir.instance.model.Observation p ) {
      if ( p == null )
         return null;
      Observation pp = new Observation();
      pp.copy( p );
      cTAKESUtils.addResource( pp );
      return pp;
   }

   public static Medication getMedication( org.hl7.fhir.instance.model.Medication p ) {
      if ( p == null )
         return null;
      Medication pp = new Medication();
      pp.copy( p );
      cTAKESUtils.addResource( pp );
      return pp;
   }

   public static AnatomicalSite getAnatomicalSite( BodySite p ) {
      if ( p == null )
         return null;
      AnatomicalSite pp = new AnatomicalSite();
      pp.copy( p );
      cTAKESUtils.addResource( pp );
      return pp;
   }

   public static Stage getStage( ConditionStageComponent c ) {
      if ( c == null )
         return null;
      Stage s = new Stage();
      s.copy( c );
      return s;
   }

   /**
    * if possible re-create Report FHIR object from REPORT file directory that
    * has FHIR XML files
    *
    * @param reportDir -
    * @return -
    */
   public static Report loadReport( File reportDir ) throws Exception {
      if ( reportDir == null || !reportDir.isDirectory() ) {
         return null;
      }
      final File[] files = reportDir.listFiles();
      if ( files == null ) {
         return null;
      }
      Report report = null;
      Patient patient = null;

      // if(reportDir.exists()){
      // find report & patient first
      for ( File f : files ) {
         if ( isType( f, Report.class ) ) {
            report = getReport( (Composition) FHIRUtils.loadFHIR( f ) );
         } else if ( isType( f, Patient.class ) ) {
            patient = getPatient( (org.hl7.fhir.instance.model.Patient) FHIRUtils.loadFHIR( f ) );
         }
      }
      if ( report == null ) {
         return null;
      }
      // add patient to report
      // if(report != null && patient != null){
      if ( patient != null ) {
         report.setPatient( patient );
      }

      // load other components into report
      for ( File f : files ) {
         if ( isType( f, Finding.class ) ) {
            report.addReportElement( getFinding( (Condition) FHIRUtils.loadFHIR( f ) ) );
         } else if ( isType( f, Disease.class ) ) {
            report.addReportElement( getDiagnosis( (Condition) FHIRUtils.loadFHIR( f ) ) );
         } else if ( isType( f, Procedure.class ) ) {
            report.addReportElement( getProcedure( (org.hl7.fhir.instance.model.Procedure) FHIRUtils.loadFHIR( f ) ) );
         } else if ( isType( f, Observation.class ) ) {
            report.addReportElement(
                  getObservation( (org.hl7.fhir.instance.model.Observation) FHIRUtils.loadFHIR( f ) ) );
         } else if ( isType( f, Medication.class ) ) {
            report.addReportElement( getMedication( (org.hl7.fhir.instance.model.Medication) FHIRUtils.loadFHIR( f ) ) );
         } else if ( isType( f, AnatomicalSite.class ) ) {
            report.addReportElement(
                  getAnatomicalSite( (org.hl7.fhir.instance.model.BodySite) FHIRUtils.loadFHIR( f ) ) );
         }
      }
      // }

      return report;
   }

   public static Disease createDiagnosis( final ConceptInstance conceptInstance ) {
      return load( new Disease(), conceptInstance );
   }

   /**
    * @param anatomicalSite  -
    * @param conceptInstance -
    * @return -
    */
   public static AnatomicalSite load( AnatomicalSite anatomicalSite, final ConceptInstance conceptInstance ) {
      anatomicalSite.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );

      // add mention text
      for ( Extension ex : cTAKESUtils.createMentionExtensions( conceptInstance ) )
         anatomicalSite.addExtension( ex );

      // now we need to get modifiers from neoplasms, how cute .. :)
      for ( IdentifiedAnnotation ia : cTAKESUtils.getBodySiteModifiers( conceptInstance ) ) {
         for ( String uri : OwlOntologyConceptUtil.getUris( ia ) ) {
            // skip bilateral for now as it causes more issues
            // TODO: what to do?
            if ( !FHIRConstants.BILATERAL.equals( FHIRUtils.getConceptName( URI.create( uri ) ) ) ) {
               anatomicalSite.addModifier( FHIRUtils.getCodeableConcept( URI.create( uri ) ) );
               anatomicalSite.addExtension( cTAKESUtils.createMentionExtension( ia ) );
            }
         }
      }

      // add language contexts
      cTAKESUtils.addLanguageContext( conceptInstance, anatomicalSite );

      // create identifier
      FHIRUtils.createIdentifier( anatomicalSite.addIdentifier(), anatomicalSite );

      // register
      cTAKESUtils.addResource( anatomicalSite, conceptInstance );

      return anatomicalSite;
   }

   /**
    * @param finding         -
    * @param conceptInstance -
    * @return -
    */
   public static Finding load( Finding finding, final ConceptInstance conceptInstance ) {
      Collection<ConceptInstance> vals = conceptInstance.getValues();

      // now lets take a look at the location of this disease
//		for (ConceptInstance ci : ConceptInstanceUtil.getLocations(conceptInstance)) {
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.BODY_SITE_URI ) ) {
         // skip null uris
         if ( ci.getUri() == null || ci.getUri().trim().length() == 0 ) {
            LOGGER.error( "Null URI detected for " + ci );
            continue;
         }

         CodeableConcept location = finding.addBodySite();
         cTAKESUtils.setCodeableConcept( location, ci );
         Element site = cTAKESUtils.getResource( ci );
         if ( site != null ) {
            if ( site instanceof AnatomicalSite )
               FHIRUtils.addResourceReference( location, site );
            else
               // System.out.println("WARNING: diagnosis:
               // '"+conceptInstance.getIdentifiedAnnotation().getCoveredText()+
               // "' location:
               // '"+ci.getIdentifiedAnnotation().getCoveredText()+
               // "' expected AnatomicalSite, but got
               // "+site.getClass().getSimpleName());
               System.out.println( "WARNING: diagnosis: '" + conceptInstance.getCoveredText() + "' location: '"
                     + ci.getCoveredText() + "' expected AnatomicalSite, but got "
                     + site.getClass().getSimpleName() );
         }
      }


      // TNM and other Findings have the URL of interest stored in Value
      // TNM and Stage do not have url stored in value, they have value stored in url
      if ( vals.isEmpty() ) {
         finding.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );
      } else {
         for ( ConceptInstance ci : vals ) {
            finding.setCode( cTAKESUtils.getCodeableConcept( ci ) );
         }
      }

      // now lets take a look at the location of this disease
      for ( ConceptInstance ci : cTAKESUtils.getLocations( conceptInstance ) ) {
         CodeableConcept location = finding.addBodySite();
         cTAKESUtils.setCodeableConcept( location, ci );
         Element site = cTAKESUtils.getResource( ci );
         FHIRUtils.addResourceReference( location, site );
      }


      // if TNM, take care of suffixes and prefixes
      for ( ConceptInstance i : cTAKESUtils.getTNM_Modifiers( conceptInstance ) ) {
         finding.addExtension( cTAKESUtils.createTNM_ModifierExtension( i ) );
      }

      cTAKESUtils.addLanguageContext( conceptInstance, finding );

      // add mention text
      for ( Extension ex : cTAKESUtils.createMentionExtensions( conceptInstance ) )
         finding.addExtension( ex );

      // create identifier
      FHIRUtils.createIdentifier( finding.addIdentifier(), finding );

      // register
      cTAKESUtils.addResource( finding, conceptInstance );

      // get all associated conceptsConceptRelationUtil.getRelatedConcepts(
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.EVENT_URI ) ) {
         finding.addEvidence( cTAKESUtils.createConditionEvidence( ci ) );
      }

      return finding;
   }

   /**
    * Initialize disease from a DiseaseDisorderMention in cTAKES typesystem
    *
    * @param dx              -
    * @param conceptInstance -
    * @return -
    */
   public static Disease load( Disease dx, final ConceptInstance conceptInstance ) {
      // set some properties
      dx.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );

      cTAKESUtils.addLanguageContext( conceptInstance, dx );

      // now lets take a look at the location of this disease
      for ( ConceptInstance ci : cTAKESUtils.getLocations( conceptInstance ) ) {
         CodeableConcept location = dx.addBodySite();
         cTAKESUtils.setCodeableConcept( location, ci );
         Element site = cTAKESUtils.getResource( ci );
         FHIRUtils.addResourceReference( location, site );
      }

      // get stage (not sure if this is needed anymore)
      Stage stage = createStage( conceptInstance );
      if ( stage != null )
         dx.setStage( stage );

      // add mention text
      for ( Extension ex : cTAKESUtils.createMentionExtensions( conceptInstance ) )
         dx.addExtension( ex );

      // create identifier
      FHIRUtils.createIdentifier( dx.addIdentifier(), dx );

      // register
      cTAKESUtils.addResource( dx, conceptInstance );

      // get all associated conceptsConceptRelationUtil.getRelatedConcepts(
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.EVENT_URI ) ) {
         dx.addEvidence( cTAKESUtils.createConditionEvidence( ci ) );
      }

      return dx;
   }

   public static Medication createMedication( final ConceptInstance conceptInstance ) {
      return load( new Medication(), conceptInstance );
   }

   /**
    * load medication
    *
    * @param md
    * @param conceptInstance
    * @return
    */
   public static Medication load( Medication md, final ConceptInstance conceptInstance ) {
      md.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );
      for ( Extension ex : cTAKESUtils.createMentionExtensions( conceptInstance ) )
         md.addExtension( ex );
      cTAKESUtils.addLanguageContext( conceptInstance, md );
      // register
      cTAKESUtils.addResource( md, conceptInstance );
      return md;
   }

   public static Observation createObservation( final ConceptInstance conceptInstance ) {
      return load( new Observation(), conceptInstance );
   }

   private static URI getResolvedURL( String name ) {
      try {
         IOntology ont = OwlConnectionFactory.getInstance().getDefaultOntology();
         if ( ont != null ) {
            IResource r = ont.getResource( name );
            return r != null ? r.getURI() : URI.create( ont.getURI() + "#" + name );
         }
      } catch ( FileNotFoundException e ) {
         new Error( e );
      } catch ( IOntologyException e ) {
         new Error( e );
      }

      return URI.create( FHIRConstants.MODEL_CANCER_URL + "#" + name );
   }

   /**
    * Initialize disease from a DiseaseDisorderMention in cTAKES typesystem
    *
    * @param ob              -
    * @param conceptInstance -
    * @return -
    */
   public static Observation load( Observation ob, final ConceptInstance conceptInstance ) {
      // set code
      ob.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );

      // add language context
      cTAKESUtils.addLanguageContext( conceptInstance, ob );

      // setting up mentions
      List<ConceptInstance> annotations = new ArrayList<ConceptInstance>();
      annotations.add( conceptInstance );

      // set interpretation
      ConceptInstance interpretation = cTAKESUtils.getInterpretation( conceptInstance );
      if ( interpretation != null ) {
         ob.setInterpretation( cTAKESUtils.getCodeableConcept( interpretation ) );
         annotations.add( interpretation );
      }

      // set method
      ConceptInstance method = cTAKESUtils.getMethod( conceptInstance );
      if ( method != null ) {
         ob.setMethod( cTAKESUtils.getCodeableConcept( method ) );
         annotations.add( method );
      }

      // set value
      ConceptInstance value = cTAKESUtils.getValue( conceptInstance );
      if ( value != null ) {
         // annotations.add(value);
         String number = value.getCoveredText();
         // TODO: right now the delta between the number and the actual
         // MeasurementAnnotation is the unit
         int offs = conceptInstance.getCoveredText().indexOf( number );
         String unit = (offs > -1) ? conceptInstance.getCoveredText().substring( offs + number.length() ).trim()
               : null;
         ob.setValue( number, unit );
      }

      // add mention text
      for ( ConceptInstance ci : annotations ) {
         for ( Extension ex : cTAKESUtils.createMentionExtensions( ci ) )
            ob.addExtension( ex );
      }

      // add id
      FHIRUtils.createIdentifier( ob.addIdentifier(), ob );

      // register
      cTAKESUtils.addResource( ob, conceptInstance );

      return ob;
   }

   static private final Predicate<ConceptInstance> NotTnmModifier = ci -> !ci.getAnnotations().stream()
         .map( Annotation::getCoveredText ).anyMatch( FHIRConstants.TNM_MODIFIER_LIST::contains );

   public static Procedure createProcedure( final ConceptInstance conceptInstance ) {
      return load( new Procedure(), conceptInstance );
   }

   /**
    * Initialize disease from a DiseaseDisorderMention in cTAKES typesystem
    *
    * @param pr              -
    * @param conceptInstance -
    * @return -
    */
   public static Procedure load( Procedure pr, final ConceptInstance conceptInstance ) {
      // set some properties
      pr.setCode( cTAKESUtils.getCodeableConcept( conceptInstance ) );
      pr.setStatus( ProcedureStatus.COMPLETED );

      cTAKESUtils.addLanguageContext( conceptInstance, pr );

      // now lets take a look at the location of this disease
      // for (ConceptInstance ci :
      // ConceptInstanceUtil.getLocations(conceptInstance)) {
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.BODY_SITE_URI ) ) {
         CodeableConcept location = pr.addBodySite();
         cTAKESUtils.setCodeableConcept( location, ci );
         Element site = cTAKESUtils.getResource( ci );
         if ( site != null )
            if ( site instanceof AnatomicalSite )
               FHIRUtils.addResourceReference( location, site );
            else
               // System.out.println("WARNING: procedure:
               // '"+conceptInstance.getIdentifiedAnnotation().getCoveredText()+
               // "' location:
               // '"+ci.getIdentifiedAnnotation().getCoveredText()+
               // "' expected AnatomicalSite, but got
               // "+site.getClass().getSimpleName());
               System.out.println( "WARNING: procedure: '" + conceptInstance.getCoveredText() + "' location: '"
                     + ci.getCoveredText() + "' expected AnatomicalSite, but got "
                     + site.getClass().getSimpleName() );
      }

      // now lets add observations
      // addEvidence();
      // addRelatedItem();

      // add mention text
      for ( Extension ex : cTAKESUtils.createMentionExtensions( conceptInstance ) )
         pr.addExtension( ex );

      FHIRUtils.createIdentifier( pr.addIdentifier(), pr );

      // register
      cTAKESUtils.addResource( pr, conceptInstance );
      return pr;
   }

   // public static Stage createStage(CancerStage st, TnmClassification tnm) {
   // return load(new Stage(),st,tnm);
   // }
   /*
    * public static Stage createStage( final Collection<IdentifiedAnnotation>
	 * stages, final Collection<IdentifiedAnnotation> tnms ) { return load( new
	 * Stage(),stages, tnms ); }
	 */
   public static Stage createStage( final ConceptInstance neoplasm ) {
      return load( new Stage(), neoplasm );
   }

   public static AnatomicalSite createAnatomicalSite( final ConceptInstance conceptInstance ) {
      return load( new AnatomicalSite(), conceptInstance );
   }

   public static Finding createFinding( final ConceptInstance conceptInstance ) {
      return load( new Finding(), conceptInstance );
   }

   /**
    * load stage object
    */

   // public static Stage load(Stage stage, CancerStage st, TnmClassification
   // tnm) {
   //
   // // set cancer stage, if available
   // if(st != null){
   // CodeableConcept c = cTAKESUtils.getCodeableConcept(st);
   // c.setText(st.getCoveredText());
   // stage.setSummary(c);
   //
   // // add id to cancer stage
   // Finding f = (Finding) cTAKESUtils.getResource(st);
   // if(f != null)
   // FHIRUtils.addResourceReference(c,f);
   //
   // // add extension
   // stage.addExtension(FHIRUtils.createMentionExtension(st.getCoveredText(),st.getBegin(),st.getEnd()));
   // }else{
   //
   // // for now just add a generic TNM
   // CodeableConcept c = cTAKESUtils.getCodeableConcept(tnm);
   // c.setText(tnm.getCoveredText());
   // stage.setSummary(c);
   // }
   //
   //
   // // extract individual Stage levels if values are conflated
   // if(tnm != null){
   // if(tnm.getSize() != null){
   // Finding f = (Finding) cTAKESUtils.getResource(tnm.getSize());
   // if(f == null)
   // f = createFinding(tnm.getSize());
   // stage.addAssessment(f);
   // //stage.setStringExtension(Stage.TNM_PRIMARY_TUMOR,cTAKESUtils.getConceptURI(st.getSize()));
   // }
   // if(tnm.getNodeSpread() != null){
   // Finding f = (Finding) cTAKESUtils.getResource(tnm.getNodeSpread());
   // if(f == null)
   // f = createFinding(tnm.getNodeSpread());
   // stage.addAssessment(f);
   // //stage.setStringExtension(Stage.TNM_REGIONAL_LYMPH_NODES,cTAKESUtils.getConceptURI(st.getNodeSpread()));
   // }
   // if(tnm.getMetastasis() != null){
   // Finding f = (Finding) cTAKESUtils.getResource(tnm.getMetastasis());
   // if(f == null)
   // f = createFinding(tnm.getMetastasis());
   // stage.addAssessment(f);
   // //stage.setStringExtension(Stage.TNM_DISTANT_METASTASIS,cTAKESUtils.getConceptURI(st.getMetastasis()));
   // }
   // stage.addExtension(FHIRUtils.createMentionExtension(tnm.getCoveredText(),tnm.getBegin(),tnm.getEnd()));
   // }
   //
   //
   // // register
   // //cTAKESUtils.addResource(stage,st);
   //
   // return stage;
   // }
	/*
	 * public static Stage load( Stage stage, final
	 * Collection<IdentifiedAnnotation> stageAnnotations, final
	 * Collection<IdentifiedAnnotation> tnmAnnotations ) { // set cancer stage,
	 * if available if ( stageAnnotations != null && !stageAnnotations.isEmpty()
	 * ) { final JCas jcas = stageAnnotations.stream().map( cTAKESUtils::getJcas
	 * ).findFirst().orElse( null ); if ( jcas == null ) { LOGGER.error(
	 * "No Cas exists for Stage annotations" ); } else { final
	 * IdentifiedAnnotation firstStageAnnotation =
	 * stageAnnotations.stream().findFirst().get(); CodeableConcept c =
	 * cTAKESUtils.getCodeableConcept( firstStageAnnotation ); c.setText(
	 * firstStageAnnotation.getCoveredText() ); stage.setSummary( c ); // add id
	 * to cancer stage Finding f = (Finding)cTAKESUtils.getResource(
	 * firstStageAnnotation ); if ( f != null ) {
	 * FHIRUtils.addResourceReference( c, f ); } // add extension
	 * stage.addExtension( FHIRUtils.createMentionExtension(
	 * firstStageAnnotation.getCoveredText(), firstStageAnnotation.getBegin(),
	 * firstStageAnnotation.getEnd() ) ); } } else { // for now just add a
	 * generic TNM if ( tnmAnnotations != null && !tnmAnnotations.isEmpty() ) {
	 * final JCas jcas = tnmAnnotations.stream().map( cTAKESUtils::getJcas
	 * ).findFirst().orElse( null ); if ( jcas == null ) { LOGGER.error(
	 * "No Cas exists for TNM annotations" ); } else { final
	 * IdentifiedAnnotation firstTnmAnnotation =
	 * tnmAnnotations.stream().findFirst().get(); CodeableConcept c =
	 * cTAKESUtils.getCodeableConcept( firstTnmAnnotation ); c.setText(
	 * firstTnmAnnotation.getCoveredText() ); stage.setSummary( c ); } } } //
	 * extract individual Stage levels if values are conflated if (
	 * tnmAnnotations != null && !tnmAnnotations.isEmpty() ) { final JCas jcas =
	 * tnmAnnotations.stream().map( cTAKESUtils::getJcas ).findFirst().orElse(
	 * null ); if ( jcas == null ) { LOGGER.error(
	 * "No Cas exists for TNM annotations" ); } else { for (
	 * IdentifiedAnnotation tnm : tnmAnnotations ) { Finding f =
	 * (Finding)cTAKESUtils.getResource( tnm ); if ( f == null ) { f =
	 * createFinding( new ConceptInstance( tnm ) ); } stage.addAssessment( f );
	 * //stage.setStringExtension(Stage.TNM_PRIMARY_TUMOR,cTAKESUtils.
	 * getConceptURI(st.getSize())); } } final IdentifiedAnnotation
	 * firstTnmAnnotation = tnmAnnotations.stream().findFirst().get();
	 * stage.addExtension( FHIRUtils.createMentionExtension(
	 * firstTnmAnnotation.getCoveredText(), firstTnmAnnotation.getBegin(),
	 * firstTnmAnnotation.getEnd() ) ); } // register
	 * //cTAKESUtils.addResource(stage,st); return stage; }
	 */
   public static Stage load( Stage stage, ConceptInstance neoplasm ) {
      // see if we have cancer stage
      // for (ConceptInstance ci :
      // ConceptInstanceUtil.getNeoplasmStage(neoplasm)) {
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( neoplasm, OwlConstants.CANCER_STAGE_URI ) ) {

         CodeableConcept c = cTAKESUtils.getCodeableConcept( ci );

         // add id to cancer stage
         Element f = cTAKESUtils.getResource( ci );
         if ( f != null ) {
            if ( f instanceof Finding )
               FHIRUtils.addResourceReference( c, f );
            else
               // System.out.println("WARNING: neoplasm:
               // '"+neoplasm.getIdentifiedAnnotation().getCoveredText()+
               // "' location:
               // '"+ci.getIdentifiedAnnotation().getCoveredText()+
               // "' expected Finding, but got
               // "+f.getClass().getSimpleName());
               System.out.println( "WARNING: neoplasm: '" + neoplasm.getCoveredText() + "' location: '"
                     + ci.getCoveredText() + "' expected Finding, but got " + f.getClass().getSimpleName() );

         }
         // add extension
         for ( Extension ex : cTAKESUtils.createMentionExtensions( ci ) )
            stage.addExtension( ex );
         stage.setSummary( c );
      }

      // add other neoplasm assessments
      // for (ConceptInstance ci :
      // ConceptInstanceUtil.getNeoplasmTNM(neoplasm)) {
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( neoplasm, OwlConstants.TNM_STAGING_URI ) ) {
         // Collection<ConceptInstance> vi =
         // ConceptInstanceUtil.getPhenotypeValues( ci);
         // add id to cancer stage
         // Finding f = (Finding)
         // cTAKESUtils.getResource(!vi.isEmpty()?vi.iterator().next():ci);
         Element f = cTAKESUtils.getResource( ci );
         if ( f != null ) {
            if ( f instanceof Finding )
               stage.addAssessment( (Finding) f );
            else
               // System.out.println("WARNING: neoplasm:
               // '"+neoplasm.getIdentifiedAnnotation().getCoveredText()+
               // "' location:
               // '"+ci.getIdentifiedAnnotation().getCoveredText()+
               // "' expected Finding, but got
               // "+f.getClass().getSimpleName());
               System.out.println( "WARNING: neoplasm: '" + neoplasm.getCoveredText() + "' location: '"
                     + ci.getCoveredText() + "' expected Finding, but got " + f.getClass().getSimpleName() );
         }
      }

      // set generic placeholder Stage if needed
      if ( FHIRUtils.isEmpty( stage.getSummary() ) ) {
         if ( stage.getAssessmentTarget().isEmpty() )
            return null;
         stage.setSummary( FHIRUtils.getCodeableConcept( FHIRConstants.GENERIC_TNM ) );
      }

      // register
      // cTAKESUtils.addResource(stage,st);
      return stage;
   }

}
