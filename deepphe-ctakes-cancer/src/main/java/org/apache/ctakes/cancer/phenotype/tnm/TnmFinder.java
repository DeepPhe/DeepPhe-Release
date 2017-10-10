package org.apache.ctakes.cancer.phenotype.tnm;


import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.cancer.phenotype.PhenotypeConcept;
import org.apache.ctakes.cancer.phenotype.PhenotypeUtil;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/26/2017
 */
public enum TnmFinder {
   INSTANCE;

   static public TnmFinder getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "TnmFinder" );

   static private final String T_URI = OwlConstants.T_STAGE_URI;
   static private final String N_URI = OwlConstants.N_STAGE_URI;
   static private final String M_URI = OwlConstants.M_STAGE_URI;


   static private final String PREFIX_REGEX = "(?:c|p|y|r|a|u)?";
   static private final String T_REGEX = "T(?:x|is|a|(?:[I]{1,3}V?)|(?:[0-4][a-z]?))(?![- ](?:weighted|axial))(?:\\((?:m|\\d+)?,?(?:is)?\\))?";
   static private final String N_REGEX = "N(?:x|(?:[I]{1,3})|(?:[0-3][a-z]?))";
   static private final String M_REGEX = "M(?:x|I|(?:[0-1][a-z]?))";

   static private final String FULL_T_REGEX = "\\b(?:" + PREFIX_REGEX + T_REGEX + ")"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")?\\b";

   static private final String FULL_N_REGEX = "\\b(?:" + PREFIX_REGEX + T_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")?\\b";

   static private final String FULL_M_REGEX = "\\b"
         + "(?:" + PREFIX_REGEX + T_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")\\b";

   static private final String FULL_REGEX = "(?:" + FULL_T_REGEX + ")|(?:" + FULL_N_REGEX + ")|(?:" + FULL_M_REGEX + ")";

   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );


   private final Object LOCK = new Object();
   private final Map<String, PhenotypeConcept> _synonymsT = new HashMap<>();
   private final Map<String, PhenotypeConcept> _synonymsN = new HashMap<>();
   private final Map<String, PhenotypeConcept> _synonymsM = new HashMap<>();

   TnmFinder() {
      synchronized (LOCK) {
         PhenotypeUtil.buildUriSynonyms( T_URI, _synonymsT, 2 );
         PhenotypeUtil.buildUriSynonyms( N_URI, _synonymsN, 2 );
         PhenotypeUtil.buildUriSynonyms( M_URI, _synonymsM, 2 );
      }
   }

   public Collection<SignSymptomMention> findTnms( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String lookupWindowText = lookupWindow.getCoveredText();
      if ( lookupWindowText.length() < 2 ) {
         return Collections.emptyList();
      }
      final Collection<SignSymptomMention> tnms = new ArrayList<>();
      final int windowStartOffset = lookupWindow.getBegin();
      try ( RegexSpanFinder finder = new RegexSpanFinder( FULL_PATTERN ) ) {
         final List<Pair<Integer>> fullSpans = finder.findSpans( lookupWindowText );
         for ( Pair<Integer> fullSpan : fullSpans ) {
            final String matchWindow = lookupWindowText.substring( fullSpan.getValue1(), fullSpan.getValue2() );
            if ( matchWindow.trim().isEmpty() ) {
               continue;
            }
            synchronized (LOCK) {
               final SignSymptomMention t = PhenotypeUtil.findPhenotype( jcas, _synonymsT, matchWindow, windowStartOffset + fullSpan.getValue1() );
               final SignSymptomMention n = PhenotypeUtil.findPhenotype( jcas, _synonymsN, matchWindow, windowStartOffset + fullSpan.getValue1() );
               final SignSymptomMention m = PhenotypeUtil.findPhenotype( jcas, _synonymsM, matchWindow, windowStartOffset + fullSpan.getValue1() );
               if ( t != null ) {
                  tnms.add( t );
               }
               if ( n != null ) {
                  tnms.add( n );
               }
               if ( m != null ) {
                  tnms.add( m );
               }
            }
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.warn( iaE.getMessage() );
      }
      return tnms;
   }


}
