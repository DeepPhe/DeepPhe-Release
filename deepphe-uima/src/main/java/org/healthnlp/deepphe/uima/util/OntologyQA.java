package org.healthnlp.deepphe.uima.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

public class OntologyQA {

   public static void main( String[] args ) throws IOntologyException, IOException, TerminologyException {
      if ( args.length == 0 ) {
         System.out.println( "Usage: java OntologyQA <file or link to ontology>" );
         System.exit( 1 );
      }
      String ontology = args[ 0 ];
      IOntology ont = OOntology.loadOntology( ontology );
      NobleCoderTerminology term = new NobleCoderTerminology( ont );

      printPolysemousTerms( term, System.out );
      printAbbreviatedTerms( term, System.out );
      printLateralityTerms( term, System.out );
      printSingleWordSynonyms( ont, System.out );
      printMultiplePreferredTerms( ont, System.out );
   }


   private static void printPolysemousTerms( NobleCoderTerminology term, PrintStream out ) {
      out.println( "#POLYSEMOUS TERMS" );
      for ( String t : term.getStorage().getTermMap().keySet() ) {
         Set<String> codes = term.getStorage().getTermMap().get( t );
         if ( codes.size() > 1 ) {
            out.println( t + "\t" + codes );
         }
      }

   }

   private static void printLateralityTerms( NobleCoderTerminology term, PrintStream out ) throws TerminologyException {
      out.println( "#LATERALITY TERMS" );
      for ( Concept c : term.getConcepts() ) {
         if ( c.getCode( Source.URI ).matches( ".*(Left|Right)" ) )
            continue;
         for ( String t : c.getSynonyms() ) {
            if ( t.toLowerCase().contains( "left" ) || t.toLowerCase().contains( "right" ) ) {
               out.println( t + "\t" + c.getCode() ); //+"\t"+c.getCode(Source.URI
            }
         }
      }
   }

   private static void printAbbreviatedTerms( NobleCoderTerminology term, PrintStream out ) throws TerminologyException {
      out.println( "#LIKELY ABBREVIATIONS TERMS" );
      for ( Concept c : term.getConcepts() ) {
         for ( String t : c.getSynonyms() ) {
            if ( !t.contains( " " ) && TextTools.isLikelyAbbreviation( t ) ) {
               out.println( t + "\t" + c.getCode() );
            }
         }
      }
   }

   private static void printSingleWordSynonyms( IOntology ont, PrintStream out ) {
      out.println( "#SINGLE WORD SYNONYMS FOR MULTI-WORD CONCEPTS" );
      for ( IClass cls : ont.getRoot().getSubClasses() ) {
         Concept c = cls.getConcept();
         if ( c.getName().contains( " " ) ) {
            for ( String s : c.getSynonyms() ) {
               if ( !s.contains( " " ) ) {
                  out.println( s + "\t" + cls.getURI() + "\t" + c.getName() );
               }
            }
         }
      }
   }

   private static void printMultiplePreferredTerms( IOntology ont, PrintStream out ) {
      out.println( "#MULTIPLE PREFERRED TERMS FOR CLASS" );
      IProperty prop = ont.getProperty( "preferredTerm" );
      for ( IClass cls : ont.getRoot().getSubClasses() ) {
         Object[] terms = cls.getPropertyValues( prop );
         if ( terms.length > 1 ) {
            out.println( cls.getName() + "\t" + cls.getURI() + "\t" + Arrays.asList( terms ) );
         }
      }
   }

}
