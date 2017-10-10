package org.healthnlp.deepphe.uima.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.util.PathHelper;

/**
 * create a custom dictionary that will map a dictionary of intest to our existing ontology
 * to give a list of terms, cuis values and mapped URIs
 *
 * @author tseytlin
 */
public class DictionaryGenerator {
   private static final String I = "|";
   private NobleCoderTerminology terminology, metathesaurus, ancestryTerminology;
   private Terminology ontologyTerminology;
   private List<SemanticType> semanticTypeFilter;
   private List<Source> sourceFilter;
   private URI defaultNOS;
   private IOntology ontology;
   private Set<String> visited;
   private boolean pullSynonymsFromMetathesaurus, filter = true;
   private PathHelper pathHelper;
   private Writer writer;

   public DictionaryGenerator( NobleCoderTerminology terminology, IOntology ontology ) throws IOException, TerminologyException, IOntologyException {
      this.terminology = terminology;
      this.ontologyTerminology = new NobleCoderTerminology( ontology );
      this.ontology = ontology;
   }


   public List<SemanticType> getSemanticTypeFilter() {
      if ( semanticTypeFilter == null )
         semanticTypeFilter = new ArrayList<SemanticType>();
      return semanticTypeFilter;
   }


   public boolean isFilter() {
      return filter;
   }


   public void setFilter( boolean filter ) {
      this.filter = filter;
   }


   public void setSemanticTypeFilter( List<SemanticType> semanticTypeFilter ) {
      this.semanticTypeFilter = semanticTypeFilter;
   }


   public PathHelper getPathHelper() {
      if ( pathHelper == null ) {
         if ( ancestryTerminology != null ) {
            pathHelper = new PathHelper( ancestryTerminology );
         } else {
            pathHelper = new PathHelper( terminology );
         }
      }
      return pathHelper;
   }


   public NobleCoderTerminology getAncestryTerminology() {
      if ( ancestryTerminology == null ) {
         ancestryTerminology = terminology;
      }
      return ancestryTerminology;
   }


   public void setAncestryTerminology( NobleCoderTerminology ancestryTerminology ) {
      this.ancestryTerminology = ancestryTerminology;
   }


   public boolean isPullSynonymsFromMetathesaurus() {
      return pullSynonymsFromMetathesaurus;
   }


   public void setPullSynonymsFromMetathesaurus( boolean pullSynonymsFromMetathesaurus ) {
      this.pullSynonymsFromMetathesaurus = pullSynonymsFromMetathesaurus;
   }


   public List<Source> getSourceFilter() {
      if ( sourceFilter == null )
         sourceFilter = new ArrayList<Source>();
      return sourceFilter;
   }


   public void setSourceFilter( List<Source> sourceFilter ) {
      this.sourceFilter = sourceFilter;
   }

   private Terminology getMetathesaurus() {
      if ( metathesaurus == null )
         try {
            metathesaurus = new NobleCoderTerminology( "NCI_Metathesaurus" );
         } catch ( IOException e ) {
            e.printStackTrace();
         }
      return metathesaurus;
   }


   public URI getDefaultNOS() {
      return defaultNOS;
   }


   public void setDefaultNOS( URI defaultNOS ) {
      this.defaultNOS = defaultNOS;
   }


   /**
    * is concept filtered out by sem
    *
    * @param c
    * @param c
    * @return
    */
   private boolean isFilteredOut( Concept c ) {
      if ( !isFilter() )
         return false;

      boolean filteredOut = true;

      if ( !getSemanticTypeFilter().isEmpty() ) {
         for ( SemanticType st : c.getSemanticTypes() ) {
            if ( getSemanticTypeFilter().contains( st ) ) {
               filteredOut = false;
               break;
            }
         }
      }
      // optional source filter
      if ( !filteredOut && !getSourceFilter().isEmpty() ) {
         filteredOut = true;
         for ( Source st : c.getSources() ) {
            if ( getSourceFilter().contains( st ) ) {
               filteredOut = false;
               break;
            }
         }
      }

      return filteredOut;
   }

   /**
    * generate dictionary by recursively pulling synonym
    *
    * @throws Exception
    */


   public void generateByHierarchy( List<Concept> roots, File output ) throws Exception {
      writer = new BufferedWriter( new FileWriter( output ) );
      visited = new HashSet<String>();
      for ( Concept c : roots ) {
         exportConcept( c, defaultNOS );
      }
      writer.close();
   }

   /**
    * only use
    *
    * @throws Exception
    */
   public void generateBySemanticTypes( File output ) throws Exception {
      writer = new BufferedWriter( new FileWriter( output ) );
      for ( String code : terminology.getAllConcepts() ) {
         exportConcept( terminology.lookupConcept( code ) );
      }
      writer.close();
   }

   /**
    * export concept information without going through hierarchy
    *
    * @param c
    * @throws TerminologyException
    * @throws IOException
    */
   private void exportConcept( Concept c ) throws TerminologyException, IOException {
      // first make sure that it fits the filter
      if ( c == null || isFilteredOut( c ) ) {
         return;
      }

      // find mapped URL
      URI url = getMatchingURL( c );
      if ( url == null )
         url = defaultNOS;

      // print concept information
      for ( String s : getTerms( c ) ) {
         StringBuffer tui = new StringBuffer();
         String sep = "";
         for ( SemanticType st : c.getSemanticTypes() ) {
            tui.append( sep + st.getCode() );
            sep = ",";
         }
         writer.write( c.getCode() + I + s + I + tui + I + url + "\n" );
      }
   }

   /**
    * get UMLS cui if available
    *
    * @param c
    * @return
    */
   private String getCUI( Concept c ) {
      String cuiRE = "CL?\\d{6,7}";
      if ( !c.getCode().matches( cuiRE ) ) {
         Pattern pt = Pattern.compile( cuiRE );
         for ( Object code : c.getCodes().values() ) {
            Matcher m = pt.matcher( code.toString() );
            if ( m.find() )
               return m.group();
         }
      }
      return c.getCode();
   }

   /**
    * export single concept as class
    *
    * @param c
    * @param c
    * @param defaultURL
    */
   private void exportConcept( Concept c, URI defaultURL ) throws Exception {
      // first make sure that it fits the filter
      if ( c == null || isFilteredOut( c ) || visited.contains( c.getCode() ) ) {
         return;
      }
      // find mapped URL
      URI url = getMatchingURL( c );
      if ( url == null )
         url = defaultURL;

      // print concept information
      for ( String s : getTerms( c ) ) {
         StringBuffer tui = new StringBuffer();
         String sep = "";
         for ( SemanticType st : c.getSemanticTypes() ) {
            tui.append( sep + st.getCode() );
            sep = ",";
         }
         //System.out.println(c.getCode()+I+s+I+tui+I+url);
         writer.write( getCUI( c ) + I + s + I + tui + I + url + "\n" );
      }

      // remember
      visited.add( c.getCode() );

      // now go into children
      for ( Concept child : c.getChildrenConcepts() ) {
         exportConcept( child, url );
      }
   }

   private Set<String> getTerms( Concept c ) throws TerminologyException {
      Set<String> terms = new LinkedHashSet<String>();
      terms.add( c.getName() );
      Collections.addAll( terms, c.getSynonyms() );
      // lookup in UMLS
      if ( pullSynonymsFromMetathesaurus ) {
         Concept cc = getMetathesaurus().lookupConcept( c.getCode() );
         if ( cc != null ) {
            Collections.addAll( terms, cc.getSynonyms() );
         }
      }
      return terms;

   }


   private Concept getMatchingOntologyConcept( Concept c ) throws TerminologyException {
      Concept cc = ontologyTerminology.lookupConcept( c.getCode() );
      // if not found try to match by other codes
      if ( cc == null ) {
         for ( Object code : c.getCodes().values() ) {
            if ( code.toString().startsWith( "NOCODE" ) )
               continue;
            cc = ontologyTerminology.lookupConcept( "" + code );
            if ( cc != null )
               break;
         }
      }
      // try search
      if ( cc == null ) {
         for ( String s : c.getSynonyms() ) {
            Concept[] ans = ontologyTerminology.search( s );
            if ( ans.length > 0 && ans[ 0 ].getMatchedTerm().equals( s ) ) {
               cc = ans[ 0 ];
               break;
            }

         }
      }
      // filter out matched concepts that are outside the branch of interest
      if ( cc != null && ontology.getClass( "" + defaultNOS ) != null ) {
         IClass cls = ontology.getClass( cc.getCode() );
         if ( cls != null ) {
            for ( IClass parent : ontology.getClass( "" + defaultNOS ).getDirectSuperClasses() ) {
               // if the found class is not a subclass of general class, don't include it
               if ( !cls.hasSuperClass( parent ) )
                  return null;
            }
         }
      }

      return cc;
   }


   private URI getMatchingURL( Concept c ) throws TerminologyException {
      Concept cc = getMatchingOntologyConcept( c );
      // try ancestors
      if ( cc == null ) {
         Concept ccc = getAncestryTerminology().lookupConcept( c.getCode() );
         if ( ccc != null ) {
            for ( Concept ac : getPathHelper().getAncestors( ccc ).keySet() ) {
               cc = getMatchingOntologyConcept( ac );
               if ( cc != null )
                  break;
            }
         }
      }

      if ( cc != null ) {
         String uri = cc.getCode();
         if ( cc.getCode( Source.URI ) != null ) {
            uri = cc.getCode( Source.URI );
         }
         return URI.create( uri );
      }
      return null;
   }


   public static void main( String[] args ) throws Exception {
      /*
		 
		// Anatomic Sites
		 	SNOMED_CT 
		 	(Body Part, Organ, or Organ Component )
		// Medications
			RxNORM
		// Procedure :
		    SNOMED_CT (Procedure branch)
			Semantic Types: 
			Laboratory Procedure
        	Diagnostic Procedure
        	Therapeutic or Preventive Procedure 
		*/


      //generate dictionary
      File dir = new File( "/home/tseytlin/Work/DeepPhe/data/ontology/" );
      NobleCoderTerminology term = new NobleCoderTerminology( "NCI_Metathesaurus" );
      NobleCoderTerminology aterm = new NobleCoderTerminology( "NCI_Thesaurus" );
      NobleCoderTerminology bodySites = new NobleCoderTerminology( OOntology.loadOntology( new File( dir, "anatomicSite.owl" ) ) );

      IOntology ont = OOntology.loadOntology( new File( dir, "nlpBreastCancer.owl" ) );

      DictionaryGenerator dg = new DictionaryGenerator( term, ont );
      dg.setAncestryTerminology( aterm );

      // Anatomical Sites
		/*System.out.println("creating body site dictionary ...");
		dg.setDefaultNOS(URI.create("http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl#OtherBodySite"));
		dg.setSemanticTypeFilter(Arrays.asList(SemanticType.getSemanticTypes(new String [] {"T021","T022","T023","T024","T025","T026","T029","T030"})));
		dg.setSourceFilter(Arrays.asList(Source.getSources(new String [] {"SNOMEDCT_US"})));
		dg.generateBySemanticTypes(new File(dir,"bodysite-dictionary.bsv"));
		dg.setFilter(false);
		dg.generateByHierarchy(Arrays.asList(bodySites.getRootConcepts()), new File(dir,"bodysite-dictionary.bsv"));
		dg.setFilter(true);*/

      // Procedures (diagnostic)
      System.out.println( "creating diagnostic procedure dictionary ..." );
      dg.setDefaultNOS( URI.create( "http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl#OtherDiagnositcProcedure" ) );
      dg.setSemanticTypeFilter( Arrays.asList( SemanticType.getSemanticTypes( new String[]{ "T060" } ) ) );
      dg.setSourceFilter( Arrays.asList( Source.getSources( new String[]{ "SNOMEDCT_US" } ) ) );
      dg.generateBySemanticTypes( new File( dir, "dx_procedure-dictionary.bsv" ) );

      // Procedures (theraputic)
      System.out.println( "creating theraputic procedure dictionary ..." );
      dg.setDefaultNOS( URI.create( "http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl#OtherTherapeuticProcedure" ) );
      dg.setSemanticTypeFilter( Arrays.asList( SemanticType.getSemanticTypes( new String[]{ "T061" } ) ) );
      dg.setSourceFilter( Arrays.asList( Source.getSources( new String[]{ "SNOMEDCT_US" } ) ) );
      dg.generateBySemanticTypes( new File( dir, "tx_procedure-dictionary.bsv" ) );


      // Drugs
      System.out.println( "creating medication dictionary ..." );
      dg.setAncestryTerminology( aterm );
      dg.setDefaultNOS( URI.create( "http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl#OtherMedication" ) );
      dg.setSemanticTypeFilter( Arrays.asList( SemanticType.getSemanticTypes( new String[]{ "T109",
            "T110",
            "T114",
            "T115",
            "T116",
            "T118",
            "T119",
            "T121",
            "T122",
            "T123",
            "T124",
            "T125",
            "T126",
            "T127",
            "T129",
            "T130",
            "T131",
            "T195",
            "T196",
            "T197",
            "T200",
            "T203" } ) ) );
      dg.setSourceFilter( Arrays.asList( Source.getSources( new String[]{ "RXNORM" } ) ) );
      dg.generateBySemanticTypes( new File( dir, "medication-dictionary.bsv" ) );
      System.out.println( "done" );
   }

}
