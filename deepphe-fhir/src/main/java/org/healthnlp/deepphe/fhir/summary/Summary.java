package org.healthnlp.deepphe.fhir.summary;

import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.BodySiteFact;
import org.healthnlp.deepphe.fhir.fact.ConditionFact;
import org.healthnlp.deepphe.fhir.fact.DefaultFactList;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactFactory;
import org.healthnlp.deepphe.fhir.fact.FactHelper;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.hl7.fhir.instance.model.List_;
import org.hl7.fhir.instance.model.Resource;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


public abstract class Summary extends List_ implements Element {
   protected Report report;
   protected Patient patient;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   protected Map<String, FactList> content;
   private Map<String, String> properties;
   protected String resourceIdentifier;
   protected URI conceptURI;

   public Map<String, FactList> getContent() {
      if ( content == null ) {
         content = new LinkedHashMap<>();
      }
      return content;
   }

   public String toString() {
      String prefix = getClass().getSimpleName();
      URI uri = getConceptURI();
      if ( uri != null )
         prefix = uri.getFragment();
      return prefix + " : " + content;
   }


   /**
    * get facts of a given category
    *
    * @param category
    * @return
    */
   public FactList getFacts( String category ) {
      FactList list = getContent().get( category );
      return list == null ? DefaultFactList.emptyList() : list;
   }

   /**
    * get facts of a given category
    *
    * @param category
    * @return
    */
   protected FactList getFactsOrInsert( String category ) {
      FactList list = getContent().get( category );
      if ( list == null ) {
         list = new DefaultFactList( category );
//			list.setCategory(category);
         getContent().put( category, list );
      }
      return list;
   }

   /**
    * clear list for drools testing
    *
    * @param category
    */
   public void clearFactList( String category ) {
      FactList list = getContent().get( category );
      if ( list != null ) {
         list.clear();
      }
   }


   public void setResourceIdentifier( String resourceIdentifier ) {
      String cname = getClass().getSimpleName() + "-";
      if ( resourceIdentifier.startsWith( cname ) )
         resourceIdentifier = resourceIdentifier.substring( cname.length() );
      this.resourceIdentifier = resourceIdentifier;
   }

   public String getResourceIdentifier() {
      return getClass().getSimpleName() + "-" + resourceIdentifier;
   }

   public String getDisplayText() {
      return getClass().getSimpleName() + " (" + resourceIdentifier + ")";
   }


   /**
    * get fact categories
    *
    * @return
    */
   public Set<String> getFactCategories() {
      return getContent().keySet();
   }

   public void addFact( String category, Fact fact ) {
      FactList list = getContent().get( category );
      if ( list == null ) {
         list = new DefaultFactList( category );
         //list.setCategory(category);
         getContent().put( category, list );
      }
      fact.setCategory( category );
      addFact( fact, list );
   }

   /**
    * add fact to a list
    *
    * @param fact
    * @param list
    */
   private void addFact( Fact fact, FactList list ) {
      /*
      // I commented out this code, because Olga needs to be able to add duplicate facts s.a. Breast, Left and Breast, Right
		// for her own internal reasoning
		// check if similar fact already exists
		boolean merged = false;
		for(Fact f: list){
			if(fact.equivalent(f)){
				f.append(fact);
				merged = true;
				break;
			}
		}
		// if it was not merged
		if(!merged){
			list.add(fact);
		}
		*/
      list.add( fact );
   }


   public Fact getFactByCategoryID( String category, String identifier ) {
      Fact toret = null;
      FactList fl = getFacts( category );
      for ( Fact f : fl ) {
         if ( f.getIdentifier().equals( identifier ) ) {
            toret = f;
         }
      }
      return toret;
   }

   public String getSummaryType() {
      return getClass().getSimpleName();
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( getDisplayText() + ":\n" );
      for ( String category : getFactCategories() ) {
         st.append( "\t" + FHIRUtils.getPropertyDisplayLabel( category ) + ":\n" );
         for ( Fact c : getFacts( category ) ) {
            st.append( "\t\t" + c.getSummaryText() + "\n" );
         }
      }
      return st.toString();
   }

   public Resource getResource() {
      return this;
   }

   public abstract URI getConceptURI();

   public void setConceptURI( URI conceptURI ) {
      this.conceptURI = conceptURI;
   }

   public Report getComposition() {
      return report;
   }

   public void setComposition( Report r ) {
      report = r;
      if ( r.getPatient() != null )
         setPatient( r.getPatient() );
      // set report name to all text mentions
      String id = report.getResourceIdentifier();
      String tp = report.getType() == null ? null : report.getType().getText();
      String tl = r.getTitle();
      for ( Fact f : getContainedFacts() ) {
         f.setDocumentIdentifier( id );
         f.setDocumentType( tp );
         f.setDocumentTitle( tl );
      }
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( Patient patient ) {
      this.patient = patient;
   }

   public Report getReport() {
      return report;
   }

   public void save( File e ) throws Exception {
      // TODO Auto-generated method stub

   }

   public void copy( Resource r ) {
      // TODO Auto-generated method stub

   }

   /**
    * is this summary appendable?
    *
    * @param s
    * @return
    */
   public abstract boolean isAppendable( Summary s );

   /**
    * do a very simple append of data
    *
    * @param ph
    */
   public void append( Summary ph ) {
      // add body site
      for ( String cat : ph.getFactCategories() ) {
         for ( Fact c : ph.getFacts( cat ) ) {
            if ( getFacts( cat ) == null || !FHIRUtils.contains( getFacts( cat ), c ) ) {
               addFact( cat, c );
            }
         }
      }
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }


   private void addIdentifiersToFact( Fact fact, String category ) {
      String reportId = getProperty( FHIRConstants.HAS_COMPOSITION );
      String reportType = getProperty( FHIRConstants.HAS_DOC_TYPE );
      String patientId = getProperty( FHIRConstants.HAS_PATIENT );
      String episodeType = getProperty( FHIRConstants.HAS_EPISODE_TYPE );
      String reportTitle = getProperty( FHIRConstants.HAS_DOC_TITLE );

      // should not happen, but just in case
      if ( reportId == null ) {
         reportId = report != null ? report.getResourceIdentifier() : null;
         reportType = report != null ? (report.getType() == null ? null : report.getType().getText()) : null;
         patientId = patient != null ? patient.getResourceIdentifier() : null;
         reportTitle = report != null ? report.getTitle() : null;
      }

      if ( category != null )
         fact.setCategory( category );
      fact.setDocumentIdentifier( reportId );
      fact.setDocumentType( reportType );
      fact.setDocumentTitle( reportTitle );
      fact.setPatientIdentifier( patientId );
      fact.addContainerIdentifier( getResourceIdentifier() );
      fact.setSummaryType( getSummaryType() );
      fact.setSummaryId( getResourceIdentifier() );

      if ( episodeType != null )
         fact.addPropety( FHIRConstants.HAS_EPISODE_TYPE, episodeType );

   }


   /**
    * return all facts that are contained within this fact
    *
    * @return
    */
   public List<Fact> getContainedFacts() {
      ArrayList<Fact> list = new ArrayList<Fact>();

      for ( String category : getFactCategories() ) {
         for ( Fact fact : getFacts( category ) ) {
            // add IDs from this container and documents
            addIdentifiersToFact( fact, category );
            list.add( fact );
            for ( Fact f : fact.getContainedFacts() ) {
               addIdentifiersToFact( f, null );
               list.add( f );
            }
         }
      }

      return list;
   }


   /**
    * load template based on the ontology
    *
    * @param summary
    * @param uri
    */
   public void loadTemplate() {
      loadTemplate( OntologyUtils.getInstance().getOntology() );
   }

   /**
    * load template based on the ontology
    *
    * @param summary
    * @param uri
    */
   public void loadTemplate( IOntology ontology ) {
      IClass summaryClass = ontology.getClass( "" + getConceptURI() );
      if ( summaryClass != null ) {
         // now lets pull all of the properties
         for ( Object o : summaryClass.getNecessaryRestrictions() ) {
            if ( o instanceof IRestriction ) {
               IRestriction r = (IRestriction) o;
               if ( OntologyUtils.isSummarizableRestriction( r ) ) {
                  if ( !getContent().containsKey( r.getProperty().getName() ) ) {
                     FactList facts = new DefaultFactList();
                     facts.setCategory( r.getProperty().getName() );
                     facts.setTypes( getClassNames( r.getParameter() ) );
                     getContent().put( r.getProperty().getName(), facts );
                  } else {
                     for ( String type : getClassNames( r.getParameter() ) ) {
                        getContent().get( r.getProperty().getName() ).getTypes().add( type );
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * load fact categories from the template
    *
    * @param summary
    * @param report
    */
   public void loadElementsFromReport( Report report ) {
      loadElementsFromReport( report, OntologyUtils.getInstance().getOntology() );
   }

   /**
    * load fact categories from the template
    *
    * @param summary
    * @param report
    */
   public void loadElementsFromReport( Report report, IOntology ontology ) {
      loadElementsFromReport( report, ontology, Collections.EMPTY_SET );
   }

   /**
    * load fact categories from the template
    *
    * @param summary
    * @param report
    */
   public void loadElementsFromReport( Report report, IOntology ontology, Set<String> validIds ) {
      for ( String category : getFactCategories() ) {
         for ( String name : getFacts( category ).getTypes() ) {
            IClass cls = ontology.getClass( name );
            if ( cls != null ) {
               int n = 1;
               for ( Element e : report.getReportElements() ) {
                  URI uri = FHIRUtils.getConceptURI( e.getCode() );
                  if ( uri != null ) {
                     // if valid ids are define, skip stuff that is not defined
                     String id = e.getResourceIdentifier();
                     if ( !validIds.isEmpty() && !validIds.contains( id ) )
                        continue;

                     IClass c = ontology.getClass( "" + uri );
                     if ( c != null ) {
                        if ( c.equals( cls ) || c.hasSuperClass( cls ) ) {
                           // make sure that the FHIR element is relevant based on linguistic info
                           if ( FHIRUtils.isRelevant( report, e, c ) ) {
                              Fact fact = FactFactory.createFact( e );
                              addTemporality( fact, e, report.getDate(), n++ );
                              addAncestors( ontology, fact );
                              addFact( category, fact );
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * load fact categories from the template
    *
    * @param summary
    * @param report
    */
   public void loadReportFacts( Collection<Fact> facts, IOntology ontology ) {
      for ( String category : getFactCategories() ) {
         for ( String name : getFacts( category ).getTypes() ) {
            IClass cls = ontology.getClass( name );
            if ( cls != null ) {
               for ( Fact fact : facts ) {
                  IClass c = ontology.getClass( fact.getUri() );
                  if ( c != null ) {
                     if ( c.equals( cls ) || c.hasSuperClass( cls ) ) {
                        // make sure that the FHIR element is relevant based on linguistic info
                        if ( FHIRUtils.isRelevant( fact, c ) ) {
                           addAncestors( ontology, fact );
                           addFact( category, fact );
                        }
                     }
                  }
               }
            }
         }
      }
   }


   private void addTemporality( Fact fact, Element el, Date dt, int i ) {
      if ( dt != null )
         fact.setRecordedDate( dt );
      //fact.setTemporalOrder(FHIRUtils.createTemporalOrder(el, i));

   }

   private void addAncestors( IOntology ontology, Fact fact ) {
      OntologyUtils.getInstance( ontology ).addAncestors( fact );
      for ( Fact f : fact.getContainedFacts() ) {
         OntologyUtils.getInstance( ontology ).addAncestors( f );
      }
   }


   private List<String> getClassNames( ILogicExpression exp ) {
		/*List<String> list = new ArrayList<String>();
		for(Object o: exp){
			if(o instanceof IClass){
				list.add(((IClass)o).getName());
			}
		}*/
      return OntologyUtils.getClasses( exp ).stream().map( a -> a.getName() ).collect( Collectors.toList() );
   }


   public static String createLocationIdentifier( ConditionFact tumor, BodySiteFact site ) {
      StringBuffer b = new StringBuffer();
      b.append( tumor.getName() + "_" );
      if ( site.getBodySide() != null )
         b.append( site.getBodySide().getName() + "_" );
      b.append( site.getName() );
      List<String> modifiers = new ArrayList<String>();
      for ( Fact mod : site.getModifiers() ) {
         if ( !FactFactory.isBodySide( mod ) )
            modifiers.add( mod.getName() );
      }
      Collections.sort( modifiers, Collections.reverseOrder() );
      for ( String s : modifiers ) {
         b.append( "_" + s );
      }
      return b.toString();
   }

   public Map<String, String> getProperties() {
      if ( properties == null )
         properties = new LinkedHashMap<String, String>();
      return properties;
   }

   public void setProperties( Map<String, String> properties ) {
      this.properties = properties;
   }

   public void addProperty( String key, String val ) {
      getProperties().put( key, val );
   }

   public String getProperty( String key ) {
      String val = getProperties().get( key );
      if ( val == null )
         val = getProperties().get( FHIRUtils.FHIR_VALUE_PREFIX + key );
      return val;
   }

   public void addProperties( Map<String, String> props ) {
      getProperties().putAll( props );
   }

}
