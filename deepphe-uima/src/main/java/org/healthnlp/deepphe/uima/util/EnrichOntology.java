package org.healthnlp.deepphe.uima.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * enrich ontology with synonyms from NCI Metathesaurus
 *
 * @author tseytlin
 */
public class EnrichOntology {
   public static final String CODE = "Code";
   public static final String SYNONYM = "Synonym";
   public static final String DEFINITION = "Definition";
   public static final String SEM_TYPE = "SemanticType";
   public static final String ALT_CODE = "AlternateCode";
   public static final String PREF_TERM = "PreferredTerm";
   private static Map<String, String> propertyMap;
   public static final String TERMINOLOGY_CORE = "http://blulab.chpc.utah.edu/ontologies/TermMapping.owl";

   private static void addConeptInfoFromUMLS( IClass cls, Terminology umls ) throws TerminologyException {
      String name = cls.getLabels().length > 0 ? cls.getLabels()[ 0 ] : cls.getName().replaceAll( "_", " " );
      if ( umls != null ) {
         for ( Concept c : umls.search( name ) ) {
            if ( c.getMatchedTerm().equals( name ) || TextTools.normalize( c.getMatchedTerm() ).equals( TextTools.normalize( name ) ) ) {
               addConceptInfo( c, cls );
            } else {
               System.err.println( "\tno exact match found: " + c.getName() + " vs " + c.getMatchedTerm() );
            }
         }
      }
   }

   /**
    * get property mapping
    *
    * @return
    */
   private static Map<String, String> getPropertyMapping() {
      if ( propertyMap == null ) {
         propertyMap = new LinkedHashMap<String, String>();
         propertyMap.put( CODE, TERMINOLOGY_CORE + "#code" );
         propertyMap.put( SYNONYM, TERMINOLOGY_CORE + "#synonym" );
         propertyMap.put( DEFINITION, TERMINOLOGY_CORE + "#definition" );
         propertyMap.put( SEM_TYPE, TERMINOLOGY_CORE + "#semanticType" );
         propertyMap.put( ALT_CODE, TERMINOLOGY_CORE + "#alternateCode" );
         propertyMap.put( PREF_TERM, TERMINOLOGY_CORE + "#preferredTerm" );
      }
      return propertyMap;
   }


   /**
    * add concept info from concept object to class
    *
    * @param c
    * @param cls
    */
   private static void addConceptInfo( Concept c, IClass cls ) {
      IOntology ont = cls.getOntology();

      Map<String, String> map = getPropertyMapping();
      IProperty code = ont.getProperty( map.get( CODE ) );
      IProperty synonym = ont.getProperty( map.get( SYNONYM ) );
      IProperty definition = ont.getProperty( map.get( DEFINITION ) );
      IProperty semType = ont.getProperty( map.get( SEM_TYPE ) );
      IProperty altCode = ont.getProperty( map.get( ALT_CODE ) );
      IProperty prefTerm = ont.getProperty( map.get( PREF_TERM ) );


      if ( code == null )
         code = ont.createProperty( map.get( CODE ), IProperty.ANNOTATION );
      if ( synonym == null )
         synonym = ont.createProperty( map.get( SYNONYM ), IProperty.ANNOTATION );
      if ( definition == null )
         definition = ont.createProperty( map.get( DEFINITION ), IProperty.ANNOTATION );
      if ( semType == null )
         semType = ont.createProperty( map.get( SEM_TYPE ), IProperty.ANNOTATION );
      if ( altCode == null )
         altCode = ont.createProperty( map.get( ALT_CODE ), IProperty.ANNOTATION );
      if ( prefTerm == null )
         prefTerm = ont.createProperty( map.get( PREF_TERM ), IProperty.ANNOTATION );

      // add preferred term
      cls.addPropertyValue( prefTerm, c.getName() );


      // add synonyms
      for ( String s : c.getSynonyms() ) {
         if ( !cls.hasPropetyValue( synonym, s ) )
            cls.addPropertyValue( synonym, s );
      }

      // add definitions
      for ( Definition d : c.getDefinitions() ) {
         if ( !cls.hasPropetyValue( definition, d.getDefinition() ) )
            cls.addPropertyValue( definition, d.getDefinition() );
      }

      // get concept code
      cls.setPropertyValue( code, c.getCode() );
      for ( Object src : c.getCodes().keySet() ) {
         String cui = (String) c.getCodes().get( src ) + " [" + src + "]";
         if ( !cls.hasPropetyValue( altCode, cui ) )
            cls.addPropertyValue( altCode, cui );
      }

      // get semantic types
      for ( SemanticType st : c.getSemanticTypes() ) {
         if ( !cls.hasPropetyValue( semType, st.getName() ) )
            cls.addPropertyValue( semType, st.getName() );
      }

   }

   public static void main( String[] args ) throws IOntologyException, IOException, TerminologyException {
      String o = "/home/tseytlin/Work/DeepPhe/data/ontology/nlpBreastCancer.owl";
      System.out.println( "loading " + o );
      Terminology term = new NobleCoderTerminology( "NCI_Metathesaurus" );
      IOntology ont = OOntology.loadOntology( new File( o ) );
      // go over all classes that matter
      for ( String s : Arrays.asList( "Annotation" ) ) {
         for ( IClass cls : ont.getClass( s ).getSubClasses() ) {
            // make sure that the class belongs to this ontology
            if ( cls.getURI().toString().startsWith( ont.getURI().toString() ) ) {
               System.out.println( "\tget synonyms for: " + cls.getName() );
               addConeptInfoFromUMLS( cls, term );
            }
         }
      }
      System.out.print( "saving .. " );
      ont.save();
      System.out.println( "OK" );
   }
}
