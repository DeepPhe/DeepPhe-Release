package org.healthnlp.deepphe.uima.ae;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fhir.Condition;
import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.*;
import org.healthnlp.deepphe.fhir.summary.*;
import org.healthnlp.deepphe.uima.fhir.PhenotypeResourceFactory;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.hl7.fhir.instance.model.CodeableConcept;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

import static org.apache.ctakes.core.ae.OwlRegexSectionizer.OWL_FILE_DESC;
import static org.apache.ctakes.core.ae.OwlRegexSectionizer.OWL_FILE_PATH;

/**
 * create composition cancer and tumor summaries on document level
 *
 * @author tseytlin
 */

public class CompositionSummarizer extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger( "CompositionCancerSummary" );

   public static final String PARAM_ONTOLOGY_PATH = "ONTOLOGY_PATH";
   private IOntology ontology;
   private EpisodeClassifier episodeClassifier;
   private Set<String> validTumorDiagnoses;

   @ConfigurationParameter(
         name = OWL_FILE_PATH,
         description = OWL_FILE_DESC
   )
   private String _owlFilePath;

   //TODO: don't hard-code this
   public static final List<String> tumorTriggers = Arrays.asList( "Lesion", "Neoplasm" );
   public static final List<String> benignTumorTriggers = Arrays.asList( "Benign_Neoplasm" );
   public static List<String> cancerTriggers; //.asList("Malignant_Breast_Neoplasm"); //"Metastatic_Neoplasm"
   public static final List<String> documentTypeTriggers = Arrays.asList( "SP" );

   public void initialize( UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Composition Summarizer ..." );
      super.initialize( context );
      try {
         ontology = OwlConnectionFactory.getInstance().getOntology( _owlFilePath );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         throw new ResourceInitializationException( multE );
      }
      // get cancer triggers
      cancerTriggers = getCancerTriggers();

      // initialize new episode classifier
      episodeClassifier = new EpisodeClassifier();
      LOGGER.info( "Finished initializing." );
   }

   private List<String> getCancerTriggers() {
      List<String> triggers = new ArrayList<>();
      for ( IClass c : ontology.getClass( FHIRConstants.TUMOR ).getDirectSubClasses() ) {
         for ( IRestriction r : OntologyUtils.getRestrictions( c.getEquivalentRestrictions() ) ) {
            if ( r.getProperty().getName().equals( FHIRConstants.HAS_DIAGNOSIS ) ) {
               for ( IClass cc : OntologyUtils.getClasses( r.getParameter() ) ) {
                  triggers.add( cc.getName() );
               }
            }
         }
      }
      return triggers;
   }

   /**
    * process each composition to generate cancer/tumor summaries
    */
   public void process( JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Summarizing Compositions ..." );
      MedicalRecord record = new MedicalRecord();
      record.setPatient( PhenotypeResourceFactory.loadPatient( jcas ) );

      LOGGER.info( "Processing compositions for " + record + " .." );

      for ( Report report : PhenotypeResourceFactory.loadReports( jcas ) ) {
         upgradeReportFacts( report );

         PatientSummary patient = createPatientSummary( report );
         List<Summary> summaries = createSummaries( report );

         report.addCompositionSummaries( summaries );
         report.addCompositionSummary( patient );


         PhenotypeResourceFactory.saveReport( report, jcas );

         // print out
         record.addReport( report );
         //System.out.println(report.getSummaryText());
      }

      PhenotypeResourceFactory.saveMedicalRecord( record, jcas );
      LOGGER.info( "Finished summarizing." );
   }

   /**
    * some report-level facts are more generic then they need to be s.a. Tumor_Size
    * maybe we can upgrade them
    *
    * @param report
    */
   private void upgradeReportFacts( Report report ) {
      for ( Fact fact : report.getReportFacts() ) {
         Map<IClass, ILogicExpression> upgradeMap = getUpgradeExpressionMap( fact );
         for ( IClass upgradeCls : upgradeMap.keySet() ) {
            if ( isSatisified( fact, upgradeMap.get( upgradeCls ) ) ) {
               fact.setName( upgradeCls.getName() );
               fact.setUri( "" + upgradeCls.getURI() );
               fact.setLabel( upgradeCls.getLabel() );
               break;
            }
         }
      }
   }

   /**
    * is the upgrade expression satisfied
    *
    * @param fact
    * @param upgradeExpression
    * @return
    */
   private boolean isSatisified( Fact fact, ILogicExpression upgradeExpression ) {
      IClass cls = ontology.getClass( fact.getUri() );
      List<IInstance> instList = new ArrayList<>();
      IInstance inst = cls.createInstance();

      // go over restrictions in expression
      for ( Object o : upgradeExpression ) {
         if ( o instanceof IRestriction ) {
            IRestriction r = (IRestriction) o;
            IClass partCls = null;
            if ( FHIRConstants.HAS_DOC_TYPE.equals( r.getProperty().getName() ) ) {
               partCls = ontology.getClass( fact.getDocumentType() );
            } else if ( FHIRConstants.HAS_SECTION.equals( r.getProperty().getName() ) ) {
               partCls = ontology.getClass( fact.getDocumentSection() );
            } else if ( r.getParameter().evaluate( cls ) ) {
               partCls = cls;
            }

            // add property value if class is actually there
            if ( partCls != null ) {
               IInstance i = partCls.createInstance();
               inst.addPropertyValue( r.getProperty(), i );
               instList.add( i );
            }
         }
      }
      instList.add( inst );

      // figure out answer
      boolean answer = upgradeExpression.evaluate( inst );

      // clean up
      for ( IInstance i : instList ) {
         i.delete();
      }

      return answer;
   }

   /**
    * upgrade tumor size
    *
    * @param fact
    */
   private Map<IClass, ILogicExpression> getUpgradeExpressionMap( Fact fact ) {
      Map<IClass, ILogicExpression> map = new HashMap<>();
      IClass cls = ontology.getClass( fact.getUri() );
      if ( cls != null ) {
         for ( IClass child : cls.getDirectSubClasses() ) {
            ILogicExpression exp = getUpgradeExpression( child, cls );
            if ( exp != null ) {
               map.put( child, exp );
            }
         }
      }
      return map;
   }

   /**
    * upgrade tumor size
    */
   private ILogicExpression getUpgradeExpression( IClass child, IClass parent ) {
      for ( Object o : child.getEquivalentRestrictions() ) {
         if ( o instanceof ILogicExpression && OntologyUtils.getContainedClasses( (ILogicExpression) o ).contains( parent ) ) {
            return (ILogicExpression) o;
         }
      }
      return null;
   }


   private PatientSummary createPatientSummary( Report report ) {
      PatientSummary summary = new PatientSummary();
      PatientPhenotype phenotype = summary.getPhenotype();
      summary.setComposition( report );

      summary.loadTemplate( ontology );
      phenotype.loadTemplate( ontology );

      // is related to given set of tumors in diagnosis
      //summary.loadElementsFromReport(report,ontology);
      //phenotype.loadElementsFromReport(report,ontology);
      summary.loadReportFacts( report.getReportFacts(), ontology );
      phenotype.loadReportFacts( report.getReportFacts(), ontology );

      return summary;
   }


   private boolean hasTrigger( List<String> triggers, Condition cond ) {
      for ( String s : triggers ) {
         IClass trigger = ontology.getClass( s );
         IClass cls = ontology.getClass( "" + FHIRUtils.getConceptURI( cond.getCode() ) );
         if ( cls != null && (cls.hasSuperClass( trigger ) || cls.equals( trigger )) ) {
            return true;
         }
      }
      return false;
   }

   private boolean hasTrigger( List<String> triggers, Fact cond ) {
      for ( String s : triggers ) {
         IClass trigger = ontology.getClass( s );
         IClass cls = ontology.getClass( cond.getUri() );
         if ( cls != null && (cls.hasSuperClass( trigger ) || cls.equals( trigger )) ) {
            return true;
         }
      }
      return false;
   }


   /**
    * get conditions by locations
    *
    * @param report
    * @return
    */
   private Map<BodySiteFact, ConditionFact> getConditionsByLocation( Report report ) {
      Map<BodySiteFact, ConditionFact> map = new LinkedHashMap<BodySiteFact, ConditionFact>();
      for ( Element e : report.getReportElements() ) {
         // if we have a tumor trigger
         if ( e instanceof Condition && hasTrigger( tumorTriggers, (Condition) e ) && !hasTrigger( benignTumorTriggers, (Condition) e ) ) {
            Condition c = (Condition) e;
            for ( CodeableConcept cc : c.getBodySite() ) {
               Fact bsf = FactFactory.createFact( cc );
               if ( bsf instanceof BodySiteFact ) {
                  BodySiteFact site = (BodySiteFact) bsf;
                  //System.err.println(c.getDisplayText()+"\tat\t"+site.getSummaryText());
                  if ( isRelevant( report, c ) ) {
                     addConditionToMap( site, c, map );
                  }
               } else {
                  LOGGER.error( "Error: condition " + c.getDisplayText() + " has a body site that is not a body site " + bsf.getName() );
               }
            }
         }
      }
      // now lets look at it
      LOGGER.info( "tumors with locations in " + report.getTitle() );
      for ( BodySiteFact f : map.keySet() ) {
         LOGGER.info( "\t" + f.getSummaryText() + "\t\t -> \t" + map.get( f ).getSummaryText() );
      }

      return map;
   }


   /**
    * get conditions by locations
    *
    * @param report
    * @return
    */
   private Map<BodySiteFact, ConditionFact> getConditionsByLocationFact( Report report ) {
      Map<BodySiteFact, ConditionFact> map = new LinkedHashMap<BodySiteFact, ConditionFact>();
      List<ConditionFact> problemList = new ArrayList<>();
      for ( Fact e : report.getReportFacts() ) {
         // if we have a tumor trigger
         if ( e instanceof ConditionFact && hasTrigger( tumorTriggers, e ) && !hasTrigger( benignTumorTriggers, e ) ) {
            ConditionFact condition = (ConditionFact) e;
            for ( Fact bsf : condition.getBodySite() ) {
               if ( bsf instanceof BodySiteFact ) {
                  BodySiteFact site = (BodySiteFact) bsf;
                  //System.err.println(c.getDisplayText()+"\tat\t"+site.getSummaryText());
                  if ( isRelevant( report, condition ) ) {
                     //cleanBodySite(site);
                     addConditionToMap( site, condition, map );
                  }
               } else {
                  LOGGER.error( "Error: condition " + condition.getName() + " has a body site that is not a body site " + bsf.getName() );
               }
            }
            if ( condition.getBodySite().isEmpty() ) {
               problemList.add( condition );
            }
         }
      }
      // now lets look at it
      LOGGER.info( "---------" );
      LOGGER.info( "tumors with locations in " + report.getTitle() );
      for ( BodySiteFact f : map.keySet() ) {
         LOGGER.info( "\t" + f.getSummaryText() + "\t\t -> \t" + map.get( f ).getSummaryText() );
      }
      LOGGER.info( "---------" );
      LOGGER.info( "tumors without locations in " + report.getTitle() );
      for ( Fact f : problemList ) {
         LOGGER.info( "\t" + f.getSummaryText() );
      }
      LOGGER.info( "---------" );
      return map;
   }

   /**
    * remove wrong laterality in the context of tumors
    * <p>
    * private void cleanBodySite(BodySiteFact site) {
    * Fact side = site.getBodySide();
    * if( side != null && FHIRConstants.BILATERAL.equals(side.getName())){
    * site.getModifiers().remove(side);
    * site.setBodySide(null);
    * // clean the FHIR object
    * AnatomicalSite fhir = (AnatomicalSite) FHIRRegistry.getInstance().getElement(site.getIdentifier());
    * for(CodeableConcept cc: new ArrayList<CodeableConcept>(fhir.getModifier())){
    * if(FHIRConstants.BILATERAL.equals(FHIRUtils.getConceptName(cc))){
    * fhir.getModifier().remove(cc);
    * }
    * }
    * }
    * <p>
    * }
    */


   private boolean isRelevant( Report report, ConditionFact c ) {
      // make sure it is not in synoptic section
      if ( FHIRUtils.isInSection( c, FHIRConstants.SYNOPTIC_SECTION ) )
         return false;

      // check if diagnosis is relevant
      if ( !isRelevantDiagnosis( c ) )
         return false;

      // check section and linguistics
      return FHIRUtils.isRelevant( report, c, ontology );
   }


   private boolean isRelevant( Report report, Condition c ) {
      // make sure it is not in synoptic section
      if ( FHIRUtils.isInSection( c, FHIRConstants.SYNOPTIC_SECTION ) )
         return false;

      // check if diagnosis is relevant
      if ( !isRelevantDiagnosis( c ) )
         return false;

      // check section and linguistics
      return FHIRUtils.isRelevant( report, c, ontology );
   }

   private boolean isRelevantDiagnosis( Condition c ) {
      // check if Diagnosis is valid by restrictions in the tumorPhenotype
      for ( String parent : getTumorDiagnosesClasses() ) {
         if ( OntologyUtils.getInstance().hasSuperClass( c, parent ) || parent.equals( c.getDisplayText() ) ) {
            return true;
         }
      }
      LOGGER.info( "skipping element '" + c.getSummaryText() + "' because it is not a legit tumor Dx: " + getTumorDiagnosesClasses() );
      return false;
   }

   private boolean isRelevantDiagnosis( ConditionFact c ) {
      // check if Diagnosis is valid by restrictions in the tumorPhenotype
      for ( String parent : getTumorDiagnosesClasses() ) {
         if ( OntologyUtils.getInstance().hasSuperClass( c, parent ) || parent.equals( c.getName() ) ) {
            return true;
         }
      }
      LOGGER.info( "skipping element '" + c.getSummaryText() + "' because it is not a legit tumor Dx: " + getTumorDiagnosesClasses() );
      return false;
   }


   private Set<String> getTumorDiagnosesClasses() {
      if ( validTumorDiagnoses == null ) {
         validTumorDiagnoses = new LinkedHashSet<String>();
         for ( IClass summaryClass : ontology.getClass( FHIRConstants.TUMOR ).getDirectSubClasses() ) {
            // now lets pull all of the properties
            for ( IRestriction r : summaryClass.getRestrictions( ontology.getProperty( FHIRConstants.HAS_DIAGNOSIS ) ) ) {
               for ( Object o : r.getParameter() ) {
                  if ( o instanceof IClass ) {
                     validTumorDiagnoses.add( ((IClass) o).getName() );
                  }
               }
            }
         }
      }
      return validTumorDiagnoses;
   }


   /**
    * add condition map
    *
    * @param site
    * @param c
    * @param map
    */
   private void addConditionToMap( BodySiteFact site, Condition c, Map<BodySiteFact, ConditionFact> map ) {
      addConditionToMap( site, FactFactory.createFact( c ), map );
   }

   /**
    * add condition map
    *
    * @param site
    * @param map
    */
   private void addConditionToMap( BodySiteFact site, ConditionFact tumor, Map<BodySiteFact, ConditionFact> map ) {
      boolean added = false;
      for ( BodySiteFact s : new HashSet<BodySiteFact>( map.keySet() ) ) {
         BodySiteFact merged_site = mergeFact( site, s );
         ConditionFact merged_condition = mergeFact( tumor, map.get( s ) );
         // if merge was succesfull, then replace the old value in map
         if ( merged_site != null && merged_condition != null ) {
            map.remove( s );
            map.put( merged_site, merged_condition );
            added = true;
            break;
         }
      }
      if ( !added ) {
         map.put( site, tumor );
      }
   }

   private ConditionFact mergeFact( ConditionFact a, ConditionFact b ) {
      if ( !isSameTime( a, b ) )
         return null;
      // find the most specific fact between the two
      ConditionFact specific = (ConditionFact) OntologyUtils.getInstance().getSpecificFact( a, b );

      if ( specific == null && FHIRConstants.LESION.equals( a.getName() ) ) {
         specific = b;
      } else if ( specific == null && FHIRConstants.LESION.equals( b.getName() ) ) {
         specific = a;
      }


      if ( specific != null ) {
         ConditionFact fact = (ConditionFact) FactFactory.createFact( specific );
         fact.addProvenanceFact( a );
         fact.addProvenanceFact( b );
         fact.getRelatedEvidenceIds().addAll( a.getRelatedEvidenceIds() );
         fact.getRelatedEvidenceIds().addAll( b.getRelatedEvidenceIds() );
         return fact;
      }
      return null;
   }

   private boolean isSameTime( ConditionFact a, ConditionFact b ) {
      String a_time = a.getProperty( FHIRUtils.LANGUAGE_ASPECT_DOC_TIME_REL_URL );
      String b_time = b.getProperty( FHIRUtils.LANGUAGE_ASPECT_DOC_TIME_REL_URL );

      //return a_time != null && b_time != null ? a_time.equals(b_time): true;
      return true;
   }

   private BodySiteFact mergeFact( BodySiteFact a, BodySiteFact b ) {
      BodySiteFact specific = (BodySiteFact) OntologyUtils.getInstance().getSpecificFact( a, b );
      if ( specific != null ) {
         //TODO: right now check side, but merge the rest of modifiers (will see what happens)
         Fact side = getCommonModifier( a.getBodySide(), b.getBodySide() );
         if ( side != null || (a.getBodySide() == null && b.getBodySide() == null) ) {
            BodySiteFact fact = (BodySiteFact) FactFactory.createFact( specific );
            for ( Fact m : a.getModifiers() )
               fact.addModifier( m );
            for ( Fact m : b.getModifiers() )
               fact.addModifier( m );
            fact.addProvenanceFact( a );
            fact.addProvenanceFact( b );

            return fact;
         }
      }
      return null;
   }

   private Fact getCommonModifier( Fact a, Fact b ) {
      if ( a != null && b != null )
         return a.getUri().equals( b.getUri() ) ? a : null;
      /*else if(a != null)
         return a;
		else if(b != null)
			return b;*/
      return null;
   }

   /**
    * create Tumor and Cancer summaries
    *
    * @param report
    * @return
    */
   private List<Summary> createSummaries( Report report ) {
      List<Summary> list = new ArrayList<Summary>();

      Map<BodySiteFact, ConditionFact> tumors = getConditionsByLocationFact( report );
      if ( !tumors.isEmpty() ) {
         // gether evidence that is actually linked to tumors
         Set<String> relatedEvidenceToTumors = new HashSet<>();
         for ( BodySiteFact site : tumors.keySet() ) {
            relatedEvidenceToTumors.addAll( tumors.get( site ).getRelatedEvidenceIds() );
         }

         List<TumorSummary> tumorSummaries = new ArrayList<>();
         for ( BodySiteFact site : tumors.keySet() ) {
            ConditionFact condition = tumors.get( site );
            TumorSummary tumor = createTumorSummary( report, condition, site, relatedEvidenceToTumors );
            if ( tumor != null )
               tumorSummaries.add( tumor );
         }
         // add provenance document
         CancerSummary cancer = createCancerSummary( report, tumorSummaries );
         cancer.getContainedFacts();

         list.add( cancer );
      }

      return list;
   }


   /**
    * create cancer summary
    *
    * @param report
    * @return
    */
   private CancerSummary createCancerSummary( Report report, List<TumorSummary> tumors ) {
      CancerSummary cancer = new CancerSummary( report.getTitle() );
      CancerPhenotype phenotype = cancer.getPhenotype();

      cancer.setComposition( report );
      cancer.setConceptURI( getCancerSummaryURI() );
      phenotype.setConceptURI( getPhenotypeURI( cancer.getConceptURI() ) );

      // load templates
      cancer.loadTemplate( ontology );
      phenotype.loadTemplate( ontology );

      // is related to given set of tumors in diagnosis
      //cancer.loadElementsFromReport(report,ontology);
      //phenotype.loadElementsFromReport(report,ontology);
      cancer.loadReportFacts( report.getReportFacts(), ontology );
      phenotype.loadReportFacts( report.getReportFacts(), ontology );

      // set Dx, remove what got sucked out from report
      cancer.clearFactList( FHIRConstants.HAS_BODY_SITE );
      cancer.clearFactList( FHIRConstants.HAS_DIAGNOSIS );
      phenotype.clearFactList( FHIRConstants.HAS_HISTOLOGIC_TYPE );
      phenotype.clearFactList( FHIRConstants.HAS_TUMOR_EXTENT );
      phenotype.clearFactList( FHIRConstants.HAS_CANCER_TYPE );


      for ( TumorSummary tumor : tumors ) {
         // add as a tumor
         cancer.addTumor( tumor );

         // get body site and diagnosis from a tumor, it is OK to assume there is only one
         // bodySite and diagnosis as this is what the tumor is made of earlier in the code
         BodySiteFact bodySite = (BodySiteFact) tumor.getBodySite().get( 0 );
         ConditionFact diagnosis = (ConditionFact) tumor.getDiagnosis().get( 0 );

         // add body location
         cancer.addFact( FHIRConstants.HAS_BODY_SITE, bodySite );
         cancer.addFact( FHIRConstants.HAS_DIAGNOSIS, diagnosis );

         // infer stuff from Dx
         Fact histType = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_HISTOLOGIC_TYPE, FHIRConstants.HISTOLOGIC_TYPE_URI );
         if ( histType != null ) {
            histType.addProvenanceFact( diagnosis );
            phenotype.addFact( FHIRConstants.HAS_HISTOLOGIC_TYPE, histType );
         }
         // insitu vs invasive
         Fact tumorExtent = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_TUMOR_EXTENT, FHIRConstants.TUMOR_EXTENT_URI );
         if ( tumorExtent != null ) {
            tumorExtent.addProvenanceFact( diagnosis );
            phenotype.addFact( FHIRConstants.HAS_TUMOR_EXTENT, tumorExtent );
         }

         // adnecarcionma vs sarcoma
         Fact cancerType = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_CANCER_CELL_LINE, FHIRConstants.CANCER_TYPE_URI );
         if ( cancerType != null ) {
            cancerType.addProvenanceFact( diagnosis );
            phenotype.addFact( FHIRConstants.HAS_CANCER_CELL_LINE, cancerType );
         }

      }

      // upgrade TNM to Clinical or Pathologic
      //inferClinicalPathologicTNM(report, phenotype);

      return cancer;
   }

   /**
    * get phenotype URI based on summary URI
    *
    * @param conceptURI
    * @return
    */
   private URI getPhenotypeURI( URI conceptURI ) {
      IClass cls = ontology.getClass( "" + conceptURI );
      if ( cls != null ) {
         for ( IRestriction r : cls.getRestrictions( ontology.getProperty( FHIRConstants.HAS_PHENOTYPE ) ) ) {
            for ( Object o : r.getParameter() ) {
               if ( o instanceof IClass ) {
                  return ((IClass) o).getURI();
               }
            }
         }
      }
      return null;
   }

   /**
    * get domain specific cancer summary URL
    *
    * @return
    */
   private URI getCancerSummaryURI() {
      // pick a default first
      IClass cancerCls = ontology.getClass( FHIRConstants.CANCER );
      for ( IClass cls : cancerCls.getDirectSubClasses() ) {
         // if we don't have any equivelance restrictions, then this is our default class
         if ( cls.getEquivalentRestrictions().isEmpty() ) {
            cancerCls = cls;
         } else {
            // if we do have some equivelence restrictions, perhaps we should check them
            //TODO: skip Paget's disease for now
         }
      }
      return cancerCls.getURI();
   }

   private void inferClinicalPathologicTNM( Report report, CancerPhenotype phenotype ) {
      // upgrade TNM to Clinical or Pathologic
      boolean pathologic = report.getType() != null ? FHIRUtils.getDocumentType( "SP" ).getText().equals( report.getType().getText() ) : false;
      boolean clinical = !pathologic;

      for ( Fact fact : phenotype.getFacts( FHIRConstants.HAS_T_CLASSIFICATION ) ) {
         if ( hasTNMModifier( fact, FHIRConstants.P_MODIFIER ) ) {
            pathologic = true;
            clinical = !pathologic;
         } else if ( hasTNMModifier( fact, FHIRConstants.C_MODIFIER ) ) {
            clinical = true;
            pathologic = !clinical;
         }

         if ( pathologic ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getPathologicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_PATHOLOGIC_T_CLASSIFICATION, cfact );
            }
         } else if ( clinical ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getClinicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_CLINICAL_T_CLASSIFICATION, cfact );
            }
         }
      }

      for ( Fact fact : phenotype.getFacts( FHIRConstants.HAS_N_CLASSIFICATION ) ) {
         if ( pathologic ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getPathologicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_PATHOLOGIC_N_CLASSIFICATION, cfact );
            }
         } else if ( clinical ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getClinicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_CLINICAL_N_CLASSIFICATION, cfact );
            }
         }
      }

      for ( Fact fact : phenotype.getFacts( FHIRConstants.HAS_M_CLASSIFICATION ) ) {
         if ( pathologic ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getPathologicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_PATHOLOGIC_M_CLASSIFICATION, cfact );
            }
         } else if ( clinical ) {
            Fact cfact = FactFactory.createFact( FHIRUtils.getCodeableConcept( FHIRUtils.getClinicalTNM_URI( fact.getName() ) ) );
            if ( cfact != null ) {
               cfact.addProvenanceFact( fact );
               phenotype.addFact( FHIRConstants.HAS_CLINICAL_M_CLASSIFICATION, cfact );
            }
         }
      }
   }


   private boolean hasTNMModifier( Fact fact, String modifier ) {
      if ( fact instanceof TNMFact ) {
         TNMFact tnm = (TNMFact) fact;
         if ( tnm.getPrefix() != null && modifier.equals( tnm.getPrefix().getName() ) ) {
            return true;
         }
      }
      return false;
   }


   private TumorSummary createTumorSummary( Report report, ConditionFact diagnosis, BodySiteFact bodySite, Set<String> relatedEvidenceToTumors ) {
      //System.out.println("creating tumor report: "+report.getTitle()+" tumor: "+diagnosis.getName()+" site: "+bodySite.getSummaryText());

      TumorSummary tumor = new TumorSummary( report.getTitle() + "_" + Summary.createLocationIdentifier( diagnosis, bodySite ) );
      TumorPhenotype phenotype = tumor.getPhenotype();
      tumor.setComposition( report );
      tumor.setConceptURI( getTumorSummaryURI( diagnosis, bodySite ) );
      phenotype.setConceptURI( getPhenotypeURI( tumor.getConceptURI() ) );

      // check if we found a valid tumor summary, if not, then skip this
      if ( FHIRConstants.TUMOR.equals( FHIRUtils.getConceptName( tumor.getConceptURI() ) ) ) {
         LOGGER.info( "failed to create a tumor " + diagnosis + " at " + bodySite + " because no meaningful container could be found" );
         return null;
      }


      // load template
      tumor.loadTemplate( ontology );
      phenotype.loadTemplate( ontology );

      // add everyting for NOW
      Set<String> evidenceIds = diagnosis.getRelatedEvidenceIds();

      // now add evidence if it is NOT related to any tumor
      for ( Fact f : report.getReportFacts() ) {
         if ( !relatedEvidenceToTumors.contains( f.getIdentifier() ) ) {
            evidenceIds.add( f.getIdentifier() );
         }
      }

      LOGGER.info( "create tumor " + diagnosis + " at " + bodySite + " with " + evidenceIds );

      // is related to given set of tumors in diagnosis
      tumor.loadReportFacts( report.getReportFacts( evidenceIds ), ontology );
      phenotype.loadReportFacts( report.getReportFacts( evidenceIds ), ontology );

      //tumor.loadReportFacts(report.getReportFacts(),ontology);
      //phenotype.loadReportFacts(report.getReportFacts(),ontology);
      // end Olga


      // set Dx, remove what got sucked out from report
      tumor.clearFactList( FHIRConstants.HAS_BODY_SITE );
      tumor.clearFactList( FHIRConstants.HAS_DIAGNOSIS );
      phenotype.clearFactList( FHIRConstants.HAS_HISTOLOGIC_TYPE );
      phenotype.clearFactList( FHIRConstants.HAS_TUMOR_EXTENT );

      // add body location
      tumor.addFact( FHIRConstants.HAS_BODY_SITE, bodySite );
      // can we re-construct a pre-coordinated diagnosis?
      //diagnosis = getPreCoordinatedDiagnosis(diagnosis,report);

      tumor.addFact( FHIRConstants.HAS_DIAGNOSIS, diagnosis );

      // get tumor type based on URI
      Fact tumorType = getTumorType( tumor.getConceptURI() );
      if ( tumorType != null ) {
         tumor.addFact( FHIRConstants.HAS_TUMOR_TYPE, tumorType );
      }

      // infer stuff from Dx
      Fact histType = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_HISTOLOGIC_TYPE, FHIRConstants.HISTOLOGIC_TYPE_URI );
      if ( histType != null ) {
         histType.addProvenanceFact( diagnosis );
         phenotype.addFact( FHIRConstants.HAS_HISTOLOGIC_TYPE, histType );
      }
      // insitu vs invasive
      Fact tumorExtent = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_TUMOR_EXTENT, FHIRConstants.TUMOR_EXTENT_URI );
      if ( tumorExtent != null ) {
         tumorExtent.addProvenanceFact( diagnosis );
         phenotype.addFact( FHIRConstants.HAS_TUMOR_EXTENT, tumorExtent );
      }
      // adnecarcionma vs sarcoma
      Fact cancerType = getDiagnosisPartValue( diagnosis, FHIRConstants.HAS_CANCER_CELL_LINE, FHIRConstants.CANCER_TYPE_URI );
      if ( cancerType != null ) {
         cancerType.addProvenanceFact( diagnosis );
         phenotype.addFact( FHIRConstants.HAS_CANCER_CELL_LINE, cancerType );
      }


      // classify episode type if possible
      String episodeType = episodeClassifier.getEpisodeType( tumor );

      // fill in the properties for this tumor
      tumor.addProperty( FHIRConstants.HAS_EPISODE_TYPE, episodeType );
      tumor.addProperty( FHIRConstants.HAS_DOC_TYPE, FHIRUtils.getDocumentType( report.getType() ) );
      tumor.addProperty( FHIRConstants.HAS_COMPOSITION, report.getResourceIdentifier() );
      tumor.addProperty( FHIRConstants.HAS_PATIENT, report.getPatient().getResourceIdentifier() );
      tumor.addProperty( FHIRConstants.HAS_DOC_TITLE, report.getTitle() );


      return tumor;
   }

   /**
    * get tumor type based on diagnosis and location
    * @param dx - diagnosis
    * @param bs - body site
    * @return private IClass getTumorType(IClass dx, IClass bs){
   for(IClass cls: ontology.getClass(FHIRConstants.TUMOR).getDirectSubClasses()){

   }
   return null;
   }
    */

   /**
    * get a subset of expression that only includes restrictions with given property types
    *
    * @param exp
    * @param props
    * @return
    */
   private ILogicExpression getExpessionSubset( ILogicExpression exp, List<String> props ) {
      ILogicExpression newExp = new LogicExpression( exp.getExpressionType() );
      for ( Object o : exp ) {
         if ( o instanceof IRestriction ) {
            IRestriction r = (IRestriction) o;
            if ( props.contains( r.getProperty().getName() ) ) {
               newExp.add( r );
            }
         } else {
            newExp.add( o );
         }
      }
      return newExp;
   }


   private URI getTumorSummaryURI( ConditionFact diagnosis, BodySiteFact bodySite ) {
      List<String> props = Arrays.asList( FHIRConstants.HAS_DIAGNOSIS, FHIRConstants.HAS_BODY_SITE );
      IClass tumorCls = ontology.getClass( FHIRConstants.TUMOR );
      for ( IClass cls : tumorCls.getDirectSubClasses() ) {
         // if we don't have any equivelance restrictions, then this is our default class
         if ( cls.getEquivalentRestrictions().isEmpty() ) {
            tumorCls = cls;
         } else {
            // if we do have some equivelence restrictions, perhaps we should check them
            IInstance inst = cls.createInstance();
            IInstance dx = ontology.getClass( diagnosis.getUri() ).createInstance();
            IInstance bs = ontology.getClass( bodySite.getUri() ).createInstance();

            inst.addPropertyValue( ontology.getProperty( FHIRConstants.HAS_DIAGNOSIS ), dx );
            inst.addPropertyValue( ontology.getProperty( FHIRConstants.HAS_BODY_SITE ), bs );

            ILogicExpression exp = getExpessionSubset( cls.getEquivalentRestrictions(), props );

            boolean valid = exp.evaluate( inst );

            // clean up
            dx.delete();
            bs.delete();
            inst.delete();

            if ( valid ) {
               tumorCls = cls;
               break;
            }

         }
      }
      return tumorCls.getURI();
   }


   /**
    * get tumor type fact
    *
    * @param tumorSummaryURI
    * @return
    */
   private Fact getTumorType( URI tumorSummaryURI ) {
      IClass ts = ontology.getClass( "" + tumorSummaryURI );
      IClass tumorType = null;
      for ( IRestriction r : OntologyUtils.toRestrictions( ts.getEquivalentRestrictions() ) ) {
         if ( r.getProperty().getName().equals( FHIRConstants.HAS_TUMOR_TYPE ) ) {
            for ( IClass c : OntologyUtils.toClasses( r.getParameter() ) ) {
               tumorType = c;
               break;
            }
            break;
         }
      }
      if ( tumorType != null ) {
         return FactFactory.createFact( FHIRConstants.FINDING, "" + tumorType.getURI() );
      }
      return null;
   }


   /**
    * get diagnosis part value (1st from ontology, then lexically)
    *
    * @param diagnosis
    * @param property
    * @param value
    * @return
    */
   private Fact getDiagnosisPartValue( Fact diagnosis, String property, URI value ) {
      IClass dx = ontology.getClass( diagnosis.getName() );
      if ( dx != null ) {
         IClass part = getDiagnosisPart( dx, property );
         if ( part != null ) {
            try {
               return FactFactory.createFact( value.toURL().getRef(), "" + part.getURI() );
            } catch ( MalformedURLException e ) {
               throw new Error( e );
            }
         }
      }
      return getLexicalPartValue( diagnosis, value );
   }


   private IClass getDiagnosisPart( IClass cls, String property ) {
      for ( IRestriction r : cls.getRestrictions( ontology.getProperty( property ) ) ) {
         if ( r.getParameter().getOperand() instanceof IClass ) {
            return (IClass) r.getParameter().getOperand();
         }
      }
      return null;
   }


   /**
    * get lexical part value
    *
    * @param value
    * @return
    */
   private Fact getLexicalPartValue( Fact fact, URI value ) {
      // get values from histologictype and search all synonyms up the tree
      IClass dx = ontology.getClass( fact.getName() );
      if ( dx != null ) {
         IClass[] values = ontology.getClass( "" + value ).getSubClasses();
         Arrays.sort( values );
         for ( IClass cls : values ) {
            if ( isLexicalPartOf( cls, dx ) ) {
               try {
                  return FactFactory.createFact( value.toURL().getRef(), "" + cls.getURI() );
               } catch ( MalformedURLException e ) {
                  throw new Error( e );
               }
            }
         }
      }
      return null;
   }


   /**
    * is class a lexical part of a given diagnosis
    *
    * @param cls
    * @param dx
    * @return
    */
   private boolean isLexicalPartOf( IClass cls, IClass dx ) {
      // create a list of words for each
      List<List<String>> partWordList = new ArrayList<List<String>>();
      for ( String t : cls.getConcept().getSynonyms() ) {
         partWordList.add( TextTools.getWords( t.toLowerCase() ) );
      }

      // create list of terms of diagnosis and its parent
      Set<String> terms = new LinkedHashSet<String>();
      Collections.addAll( terms, dx.getConcept().getSynonyms() );
      for ( IClass parent : dx.getSuperClasses() ) {
         Collections.addAll( terms, parent.getConcept().getSynonyms() );
      }

      // now see if one is part of the other
      for ( String term : terms ) {
         List<String> words = TextTools.getWords( term.toLowerCase() );
         for ( List<String> w : partWordList ) {
            if ( words.containsAll( w ) ) {
               return true;
            }
         }
      }
      return false;
   }


}
