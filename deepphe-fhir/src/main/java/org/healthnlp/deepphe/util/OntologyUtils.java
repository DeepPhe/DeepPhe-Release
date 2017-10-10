package org.healthnlp.deepphe.util;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.fact.BodySiteFact;
import org.healthnlp.deepphe.fhir.fact.ConditionFact;
import org.healthnlp.deepphe.fhir.fact.DefaultFactList;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactFactory;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.fhir.fact.ObservationFact;
import org.healthnlp.deepphe.fhir.fact.ProcedureFact;
import org.hl7.fhir.instance.model.CodeableConcept;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;

public class OntologyUtils {
   private static OntologyUtils instance;
   private IOntology ontology;
   private Map<String, IClass> clsMap;
   private Map<String, Terminology> terminologyMap;

   private OntologyUtils( IOntology ont ) {
      ontology = ont;
   }


   public static OntologyUtils getInstance() {
      return instance;
   }

   public static OntologyUtils getInstance( IOntology ont ) {
      instance = new OntologyUtils( ont );
      return instance;
   }

   public static boolean hasInstance() {
      return instance != null;
   }


   public IOntology getOntology() {
      return ontology;
   }

   public void addAncestors( Fact fact ) {
      IClass cls = ontology.getClass( fact.getUri() );
      if ( cls == null ) {
         cls = ontology.getClass( fact.getName() );
         if ( cls != null )
            fact.setUri( cls.getURI().toString() );
      }
      if ( cls != null ) {
         Queue<IClass> parents = new LinkedList<IClass>();
         parents.add( cls );
         while ( !parents.isEmpty() ) {
            IClass c = parents.remove();
            for ( IClass parent : c.getDirectSuperClasses() ) {
               parents.add( parent );
               fact.addAncestor( parent.getName() );
               // stop, if we have a parent that is defined in upper level ontology
               if ( parent.getURI().toString().startsWith( FHIRConstants.SCHEMA_URL ) || parent.getURI().toString().startsWith( FHIRConstants.CONTEXT_URL ) ) {
                  return;
               }

            }
         }
      }
   }

   /**
    * create a fact from codeable concept
    *
    * @param cc
    * @return
    */
   public Fact createFact( CodeableConcept cc ) {
      Fact fact = null;
      URI uri = FHIRUtils.getConceptURI( cc );
      if ( uri != null ) {
         IClass cls = ontology.getClass( "" + uri );
         if ( cls != null ) {
            fact = new Fact();
            if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.OBSERVATION_URI ) ) )
               fact = new ObservationFact();
            else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.CONDITION_URI ) ) )
               fact = new ConditionFact();
            else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.BODY_SITE_URI ) ) )
               fact = new BodySiteFact();
            else if ( cls.hasSuperClass( ontology.getClass( "" + FHIRConstants.PROCEDURE_URI ) ) )
               fact = new ProcedureFact();
            fact = FactFactory.createFact( cc, fact );
            addAncestors( fact );

         } else {
            //TODO:
            System.err.println( "ERROR: WTF no class; " + cc.getText() + " " + uri );
         }
      }
      return fact;
   }


   public boolean hasSuperClass( Element e, String entryClass ) {
      return hasSuperClass( "" + e.getConceptURI(), entryClass );
   }

   public boolean hasSuperClass( Fact fact, String entryClass ) {
      return hasSuperClass( fact.getName(), entryClass );
   }


   public boolean hasSuperClass( String fact, String entryClass ) {
      if ( ontology == null )
         throw new Error( "Ontology is not defined" );

      IClass cls = ontology.getClass( fact );
      IClass ent = ontology.getClass( entryClass );
      return (cls != null && ent != null && cls.hasSuperClass( ent ));
   }


   public boolean hasSubClass( Element fact, String entryClass ) {
      return hasSuperClass( "" + fact.getConceptURI(), entryClass );
   }

   public boolean hasSubClass( Fact fact, String entryClass ) {
      return hasSuperClass( fact.getName(), entryClass );
   }

   public boolean hasSubClass( String fact, String entryClass ) {
      if ( ontology == null )
         throw new Error( "Ontology is not defined" );

      IClass cls = ontology.getClass( fact );
      IClass ent = ontology.getClass( entryClass );
      return (cls != null && ent != null && cls.hasSubClass( ent ));
   }


   /**
    * get class for a given concept code
    *
    * @param code
    * @return
    */
   public IClass getClass( String code ) {
      if ( code != null && code.startsWith( "http://" ) )
         return ontology.getClass( code );
      return getClassMap().get( code );
   }


   private Map<String, IClass> getClassMap() {
      if ( clsMap == null ) {
         clsMap = new HashMap<String, IClass>();
         for ( IClass c : ontology.getRoot().getSubClasses() ) {
            for ( String code : getUMLS_Codes( c ) ) {
               clsMap.put( code, c );
            }
         }
      }
      return clsMap;
   }

   /**
    * save dictionary as BSV file
    *
    * @param root
    * @param output
    * @throws IOException
    */
   public void saveDictionary( File output ) throws IOException {
      saveDictionary( ontology.getRoot(), output );
   }

   /**
    * save dictionary as BSV file
    *
    * @param root
    * @param output
    * @throws IOException
    */
   public static void saveDictionary( IClass root, File output ) throws IOException {
      // write out BSV file
      BufferedWriter w = new BufferedWriter( new FileWriter( output ) );
      w.write( convertCls( root ) );
      for ( IClass c : root.getSubClasses() ) {
         w.write( convertCls( c ) );
      }
      w.close();
   }

   public static List<String> getUMLS_Codes( IClass cls ) {
      List<String> codes = new ArrayList<String>();
      // find UMLS CUIS
      for ( Object cc : cls.getConcept().getCodes().values() ) {
         Matcher m = Pattern.compile( "(CL?\\d{6,7})( .+)?" ).matcher( cc.toString() );
         if ( m.matches() ) {
            codes.add( m.group( 1 ) );
         }
      }
      return codes;
   }

   public static List<String> getRXNORM_Codes( IClass cls ) {
      return getRXNORM_Codes( cls.getConcept() );
   }

   public static List<String> getRXNORM_Codes( Concept cls ) {
      List<String> codes = new ArrayList<String>();
      // find UMLS CUIS
      for ( Object cc : cls.getCodes().values() ) {
         Matcher m = Pattern.compile( "(\\d+) \\[RXNORM\\]" ).matcher( cc.toString() );
         if ( m.matches() ) {
            codes.add( m.group( 1 ) );
         }
      }
      return codes;
   }


   public static String getCode( IClass cls ) {
      List<String> codes = getUMLS_Codes( cls );
      return codes == null || codes.isEmpty() ? null : codes.get( 0 );
   }

   /**
    * convert Class to BSV entry
    *
    * @param root
    * @return
    */
   private static String convertCls( IClass cls ) {
      Concept c = cls.getConcept();
      // find UMLS CUIS
      String cui = getCode( cls );
      if ( cui != null ) {
         String tui = "";
         if ( c.getSemanticTypes().length > 0 )
            tui = c.getSemanticTypes()[ 0 ].getCode();
         StringBuffer b = new StringBuffer();
         for ( String s : c.getSynonyms() ) {
            b.append( cui + "|" + tui + "|" + s + "\n" );
         }
         return b.toString();
      } else {
         System.out.println( "No CUI in cls " + cls.getName() );
      }
      return "";
   }


   private static void printClass( IClass c, String s ) {
      System.out.println( s + c.getName() );
      Concept cc = c.getConcept();
      cc.getCode();
      cc.getCodes();
      cc.getSynonyms();
      cc.getDefinition();

      for ( IClass ch : c.getDirectSubClasses() ) {
         printClass( ch, s + "  " );
      }
   }

   /**
    * get all classes contained in the expression
    *
    * @param exp - logic expression
    * @return list of classes
    */
   public static List<IClass> getClasses( ILogicExpression exp ) {
      List<IClass> list = new ArrayList<IClass>();
      for ( Object o : exp ) {
         if ( o instanceof IClass ) {
            list.add( (IClass) o );
         } else if ( o instanceof ILogicExpression ) {
            list.addAll( getClasses( (ILogicExpression) o ) );
         }
      }
      return list;
   }

   /**
    * get all restrictions associated with this class equivalent or necessary
    *
    * @param cls - class in question
    * @return list of restrictions
    */
   public static List<IRestriction> getRestrictions( IClass cls ) {
      List<IRestriction> list = new ArrayList<IRestriction>();
      list.addAll( getRestrictions( cls.getEquivalentRestrictions() ) );
      list.addAll( getRestrictions( cls.getNecessaryRestrictions() ) );
      return list;
   }

   /**
    * get all restrictions that are contained in expression
    * This method is recursive
    *
    * @param exp - expression in question
    * @return a list of restrictions
    */
   public static List<IRestriction> getRestrictions( ILogicExpression exp ) {
      List<IRestriction> list = new ArrayList<IRestriction>();
      for ( Object o : exp ) {
         if ( o instanceof IRestriction ) {
            list.add( (IRestriction) o );
         } else if ( o instanceof ILogicExpression ) {
            list.addAll( getRestrictions( (ILogicExpression) o ) );
         }
      }
      return list;
   }

   /**
    * get ALL classes contained in nested expressions
    *
    * @param exp
    * @return
    */
   public static Set<IClass> getContainedClasses( ILogicExpression exp ) {
      return getContainedClasses( exp, new LinkedHashSet<IClass>() );
   }

   private static Set<IClass> getContainedClasses( ILogicExpression exp, Set<IClass> list ) {
      for ( Object o : exp ) {
         if ( o instanceof IRestriction ) {
            getContainedClasses( ((IRestriction) o).getParameter(), list );
         } else if ( o instanceof ILogicExpression ) {
            getContainedClasses( (ILogicExpression) o, list );
         } else if ( o instanceof IClass ) {
            list.add( (IClass) o );
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
   public Map<String, List<IClass>> getRelatedClassMap( IClass summaryClass ) {
      Map<String, List<IClass>> map = new LinkedHashMap<String, List<IClass>>();
      if ( summaryClass != null ) {
         // see if there is a more specific
         for ( IClass cls : summaryClass.getDirectSubClasses() ) {
            summaryClass = cls;
            break;
         }

         // now lets pull all of the properties
         for ( Object o : summaryClass.getNecessaryRestrictions() ) {
            if ( o instanceof IRestriction ) {
               IRestriction r = (IRestriction) o;
               if ( isSummarizableRestriction( r ) ) {
                  if ( !map.containsKey( r.getProperty().getName() ) ) {
                     map.put( r.getProperty().getName(), getClasses( r.getParameter() ) );
                  } else {
                     map.get( r.getProperty().getName() ).addAll( getClasses( r.getParameter() ) );
                  }
               }
            }
         }
      }
      return map;
   }


   /**
    * get tumor related class map
    *
    * @return
    */
   public Map<String, List<IClass>> getTumorRelatedClassMap() {
      Map<String, List<IClass>> map = getRelatedClassMap( ontology.getClass( FHIRConstants.TUMOR ) );
      map.putAll( getRelatedClassMap( ontology.getClass( FHIRConstants.TUMOR_PHENOTYPE ) ) );
      return map;
   }


   /**
    * get tumor related class map
    *
    * @return
    */
   public Map<String, List<IClass>> getCancerRelatedClassMap() {
      Map<String, List<IClass>> map = getRelatedClassMap( ontology.getClass( FHIRConstants.CANCER ) );
      map.putAll( getRelatedClassMap( ontology.getClass( FHIRConstants.CANCER_PHENOTYPE ) ) );
      return map;
   }

   /**
    * get tumor related class map
    *
    * @return
    */
   public Map<String, List<IClass>> getPatientRelatedClassMap() {
      Map<String, List<IClass>> map = getRelatedClassMap( ontology.getClass( FHIRConstants.PATIENT ) );
      map.putAll( getRelatedClassMap( ontology.getClass( FHIRConstants.PATIENT_PHENOTYPE ) ) );
      return map;
   }


   /**
    * get related classes based on the map that was calculated earlier
    *
    * @param map
    * @return
    */
   public Set<IClass> getRelatedClases( Map<String, List<IClass>> map ) {
      Set<IClass> classes = new LinkedHashSet<IClass>();
      for ( List<IClass> cc : map.values() ) {
         classes.addAll( cc );
      }
      return classes;
   }


   /**
    * should this restriction be used for summarization
    *
    * @param r
    * @return
    */
   public static boolean isSummarizableRestriction( IRestriction r ) {
      IOntology ontology = r.getOntology();
      IClass bs = ontology.getClass( FHIRConstants.BODY_SITE );
      IClass event = ontology.getClass( FHIRConstants.EVENT );

      if ( r.getProperty().isObjectProperty() ) {
         for ( IClass cls : getClasses( r.getParameter() ) ) {
            return cls.hasSuperClass( event ) || cls.equals( bs ) || cls.hasSuperClass( bs );
         }
      }
      return false;
   }


   public Fact getSpecificFact( Fact a, Fact b ) {
      if ( a == null || b == null )
         return null;
      if ( a.getName().equals( b.getName() ) )
         return a;
      if ( hasSuperClass( a.getName(), b.getName() ) )
         return a;
      if ( hasSubClass( a.getName(), b.getName() ) )
         return b;
      return null;
   }

   public Fact getGeneralFact( Fact a, Fact b ) {
      return getSpecificFact( b, a );
   }

   /**
    * lookup class names based
    *
    * @param ontologyBranch
    * @param text
    * @return
    * @throws TerminologyException
    */
   public String lookupClassName( String ontologyBranch, String text ) throws TerminologyException {
      Terminology term = getTerminology( ontologyBranch );
      if ( term != null ) {
         for ( Concept c : term.search( text ) ) {
            return c.getCode();
         }
      }
      return null;
   }


   private Terminology getTerminology( String ontologyBranch ) throws TerminologyException {
      if ( getOntology().getClass( ontologyBranch ) == null )
         return null;

      if ( terminologyMap == null ) {
         terminologyMap = new HashMap<String, Terminology>();
      }
      Terminology term = terminologyMap.get( ontologyBranch );
      if ( term == null ) {
         term = new NobleCoderTerminology();
         for ( IClass cls : getOntology().getClass( ontologyBranch ).getSubClasses() ) {
            term.addConcept( cls.getConcept() );
         }
         terminologyMap.put( ontologyBranch, term );
      }
      return term;
   }


   /**
    * load template based on the ontology
    *
    * @param summary
    * @param uri
    */
   private Set<IClass> getRelatedClassSet( IClass summaryClass ) {
      Set<IClass> list = new HashSet<IClass>();
      if ( summaryClass != null ) {
         // now lets pull all of the properties
         for ( IRestriction r : getRestrictions( summaryClass ) ) {
            if ( isSummarizableRestriction( r ) ) {
               list.addAll( getClasses( r.getParameter() ) );
            }
         }
      }
      return list;
   }


   /**
    * creates a mapping between neoplasm and lesion entity triggers and possible related
    * container attributes, this map can be used to figure out if matched entity should be
    * associated with some attribute mentioned in a given span of text
    *
    * @return map trigger class to set of related class
    */
   public Map<IClass, Set<IClass>> createRelatedEvidenceMap() {
      Map<IClass, Set<IClass>> map = new HashMap<IClass, Set<IClass>>();

      // first lets build a list of container classes
      List<IClass> clss = new ArrayList<IClass>();
      clss.add( ontology.getClass( FHIRConstants.CANCER ) );
      Collections.addAll( clss, ontology.getClass( FHIRConstants.CANCER ).getDirectSubClasses() );
      clss.add( ontology.getClass( FHIRConstants.TUMOR ) );
      Collections.addAll( clss, ontology.getClass( FHIRConstants.TUMOR ).getDirectSubClasses() );

      //now lets go over each container
      for ( IClass cls : clss ) {
         Map<IClass, Set<IClass>> m = createRelatedEvidenceMap( cls );
         for ( IClass dx : m.keySet() ) {
            Set<IClass> related = map.get( dx );
            if ( related == null ) {
               map.put( dx, m.get( dx ) );
            } else {
               related.addAll( m.get( dx ) );
            }
         }
      }

      return map;
   }

   /**
    * creates a mapping between neoplasm and lesion entity triggers and possible related
    * container attributes, this map can be used to figure out if matched entity should be
    * associated with some attribute mentioned in a given span of text
    *
    * @param container - container class
    * @return map trigger class to set of related class
    */
   private Map<IClass, Set<IClass>> createRelatedEvidenceMap( IClass container ) {
      Map<IClass, Set<IClass>> map = new HashMap<IClass, Set<IClass>>();
      Set<IClass> diagnosis = new LinkedHashSet<IClass>();
      Set<IClass> related = new LinkedHashSet<IClass>();
      Set<IClass> phenotype = new LinkedHashSet<IClass>();
      ;

      // get diagnosis
      for ( IRestriction r : container.getRestrictions( ontology.getProperty( FHIRConstants.HAS_DIAGNOSIS ) ) ) {
         diagnosis.addAll( getClasses( r.getParameter() ) );
      }
      // get phenotype
      for ( IRestriction r : container.getRestrictions( ontology.getProperty( FHIRConstants.HAS_PHENOTYPE ) ) ) {
         phenotype.addAll( getClasses( r.getParameter() ) );
      }
      // get related classes
      for ( IRestriction r : getRestrictions( container ) ) {
         if ( !r.getProperty().getName().equals( FHIRConstants.HAS_DIAGNOSIS ) && isSummarizableRestriction( r ) ) {
            related.addAll( getClasses( r.getParameter() ) );
         }
      }
      // get related classes of phenotypes
      for ( IClass ph : phenotype ) {
         for ( IRestriction r : getRestrictions( ph ) ) {
            if ( !r.getProperty().getName().equals( FHIRConstants.HAS_DIAGNOSIS ) && isSummarizableRestriction( r ) ) {
               related.addAll( getClasses( r.getParameter() ) );
            }
         }
      }

      // add to map
      for ( IClass dx : diagnosis ) {
         map.put( dx, related );
      }

      return map;
   }

   /**
    * sort classes by hierarch where more specific class is first
    *
    * @param set
    * @return
    */
   public static List<IClass> sortClassesByHierarchy( Collection<IClass> set ) {
      List<IClass> list = new ArrayList<IClass>();
      list.addAll( set );
      Collections.sort( list, new Comparator<IClass>() {
         public int compare( IClass o1, IClass o2 ) {
            if ( o1.hasSuperClass( o2 ) )
               return -1;
            if ( o1.hasSubClass( o2 ) )
               return 1;
            return o2.compareTo( o1 );
         }
      } );
      return list;
   }

   public static List<IRestriction> toRestrictions( ILogicExpression exp ) {
      List<IRestriction> list = new ArrayList<>();
      for ( Object o : exp ) {
         if ( o instanceof IRestriction ) {
            list.add( (IRestriction) o );
         }
      }
      return list;
   }

   public static List<IClass> toClasses( ILogicExpression exp ) {
      List<IClass> list = new ArrayList<>();
      for ( Object o : exp ) {
         if ( o instanceof IClass ) {
            list.add( (IClass) o );
         }
      }
      return list;
   }


   public static void main( String[] args ) throws Exception {
      OntologyUtils ou = new OntologyUtils( OOntology.loadOntology( "/home/tseytlin/Work/DeepPhe/data/ontology/nlpBreastCancer.owl" ) );
      Map<IClass, Set<IClass>> map = ou.createRelatedEvidenceMap();
      for ( IClass dx : ou.sortClassesByHierarchy( map.keySet() ) ) {
         System.out.println( dx + " -> " + map.get( dx ) );
      }
   }

}
