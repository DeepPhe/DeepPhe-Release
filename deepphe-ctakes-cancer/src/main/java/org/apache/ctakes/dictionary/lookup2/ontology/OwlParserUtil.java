package org.apache.ctakes.dictionary.lookup2.ontology;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton used to obtain information relevant to a ctakes UmlsConcept from an owl iClass or Concept
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2015
 */
public enum OwlParserUtil {
   INSTANCE;

   static public OwlParserUtil getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "OwlParserUtil" );

   static private final Pattern CUI_PATTERN = Pattern.compile( "(CL?\\d{6,7})( .+)?" );
   static private final Pattern SNOMED_PATTERN = Pattern.compile( "(\\d+) \\[SNOMEDCT_US\\]" );
   static private final Pattern RXNORM_PATTERN = Pattern.compile( "(\\d+) \\[RXNORM\\]" );
   static private final Pattern ICD9CM_PATTERN = Pattern.compile( "(\\d+) \\[ICD9CM\\]" );
   static private final Pattern ICD10CM_PATTERN = Pattern.compile( "(\\d+) \\[ICD10CM\\]" );

   private final Collection<String> _unwantedUriRoots = new HashSet<>();
   private final Collection<String> _unwantedUris = new HashSet<>();

   synchronized public void addUnwantedUriRoot( final String unwantedUriRoot ) {
      _unwantedUriRoots.add( unwantedUriRoot );
   }

   synchronized public void addUnwantedUris( final Collection<String> unwantedUris ) {
      _unwantedUris.addAll( unwantedUris );
   }

   synchronized public void updateUnwantedUris() {
      if ( !_unwantedUriRoots.isEmpty() ) {
         _unwantedUriRoots.stream().flatMap( OwlOntologyConceptUtil::getUriBranchStream ).forEach( _unwantedUris::add );
         _unwantedUriRoots.clear();
      }
   }

   public boolean isUnwantedUri( final String uri ) {
      return _unwantedUris.contains( uri );
   }


   /**
    * @param iClass -
    * @return cui from iclass or null if none exists
    */
   static public String getCui( final IClass iClass ) {
      return getCui( iClass.getConcept() );
   }

   /**
    * @param concept -
    * @return cui from concept or null if none exists
    */
   static public String getCui( final Concept concept ) {
      final Collection<Object> allCodes = concept.getCodes().values();
      if ( allCodes.isEmpty() ) {
         return "H" + Math.abs( (concept.hashCode() % 1000000) );
      }
      for ( Object conceptCodes : allCodes ) {
         final Matcher matcher = CUI_PATTERN.matcher( conceptCodes.toString() );
         if ( matcher.matches() ) {
            return matcher.group( 1 );
         }
      }
      return "H" + Math.abs( (concept.hashCode() % 1000000) );
   }

   /**
    * @param iClass -
    * @return tui from iclass or null if none exists
    */
   static public String getTui( final IClass iClass ) {
      return getTui( iClass.getConcept() );
   }

   /**
    * @param concept -
    * @return tui from concept or null if none exists
    */
   static public String getTui( final Concept concept ) {
      final SemanticType[] semanticTypes = concept.getSemanticTypes();
      if ( semanticTypes.length > 0 ) {
         return semanticTypes[ 0 ].getCode();
      }
      return null;
   }

   /**
    * @param iClass -
    * @return uri from iClass as an ascii string
    */
   static public String getUriString( final IClass iClass ) {
      return getUri( iClass ).toASCIIString();
   }

   /**
    * @param iClass -
    * @return uri from iClass
    */
   static public URI getUri( final IClass iClass ) {
      return iClass.getURI();
   }


   /**
    * @param concept -
    * @return any existing snomedct codes from a concept
    */
   static public Collection<String> getSnomedCt( final Concept concept ) {
      return getConceptCodes( concept, SNOMED_PATTERN );
   }

   /**
    * @param concept -
    * @return any existing rxnorm codes from a concept
    */
   static public Collection<String> getRxNorm( final Concept concept ) {
      return getConceptCodes( concept, RXNORM_PATTERN );
   }

   /**
    * @param concept -
    * @return any existing icd9 codes from a concept
    */
   static public Collection<String> getIcd9( final Concept concept ) {
      return getConceptCodes( concept, ICD9CM_PATTERN );
   }

   /**
    * @param concept -
    * @return any existing icd10 codes from a concept
    */
   static public Collection<String> getIcd10( final Concept concept ) {
      return getConceptCodes( concept, ICD10CM_PATTERN );
   }

   /**
    * @param iClass -
    * @return preferred text for the given iClass
    */
   static public String getPreferredText( final IClass iClass ) {
      return getPreferredText( iClass.getConcept() );
   }

   /**
    * @param concept -
    * @return preferred text for the given Concept
    */
   static public String getPreferredText( final Concept concept ) {
      return concept.getPreferredTerm().getText();
   }

   /**
    * @param concept -
    * @param pattern regex pattern
    * @return any existing codes from a concept with the given Pattern
    */
   static private Collection<String> getConceptCodes( final Concept concept, final Pattern pattern ) {
      final Collection<Object> allCodes = concept.getCodes().values();
      if ( allCodes.isEmpty() ) {
         return Collections.emptyList();
      }
      return getConceptCodes( allCodes, pattern );
   }

   /**
    * @param allCodes -
    * @param pattern  regex pattern
    * @return any codes in the list with the given Pattern
    */
   static private Collection<String> getConceptCodes( final Iterable<Object> allCodes, final Pattern pattern ) {
      final Collection<String> conceptCodes = new ArrayList<>();
      for ( Object code : allCodes ) {
         final Matcher matcher = pattern.matcher( code.toString() );
         if ( matcher.matches() ) {
            conceptCodes.add( matcher.group( 1 ) );
         }
      }
      return conceptCodes;
   }


}
