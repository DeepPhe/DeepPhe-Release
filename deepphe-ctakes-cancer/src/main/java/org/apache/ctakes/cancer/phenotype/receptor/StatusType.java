package org.apache.ctakes.cancer.phenotype.receptor;

import org.apache.ctakes.cancer.phenotype.property.Type;
import org.apache.ctakes.cancer.phenotype.property.Value;
import org.apache.ctakes.cancer.uri.UriConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2015
 */
@Deprecated
public enum StatusType implements Type {
   ER( "Estrogen receptor",
         UriConstants.ER_STATUS,
         "(?:Estrogen|ER(?!B))",
         "C1719706", "C1719707", "C0279758" ),
   PR( "Progesterone receptor",
         UriConstants.PR_STATUS,
         "(?:Progesterone|PR)",
         "C0279759", "C0279766", "C0279768" ),
   HER2( "Human epidermal growth factor receptor 2",
         UriConstants.HER2_STATUS,
         "(?:HER-? ?2(?: ?/ ?neu)?(?:\\s*\\(?ERBB2\\)?)?)",
         "C2348909", "C2348908", "C2348910" ),
   NEG_3( "Triple Negative",
         "Receptor_Status",
         "Triple",
         "C2348819", "C2348819", "C2348819" );

   static private final String RECEPTOR_EX = "(?:\\s*-?\\s*?Receptors?\\s*-?)?\\s*(?:status|expression)?";
   final private String _title;
   final private String _uri;
   final private String _positiveCui;
   final private String _negativeCui;
   final private String _unknownCui;
   final private Pattern _pattern;

   final private String _regex;

   StatusType( final String title, final String uri, final String regex,
               final String positiveCui, final String negativeCui, final String unknownCui ) {
      _title = title;
      _uri = uri;
      _pattern = Pattern.compile( "\\b" + regex + RECEPTOR_EX, Pattern.CASE_INSENSITIVE );
      _positiveCui = positiveCui;
      _negativeCui = negativeCui;
      _unknownCui = unknownCui;
      _regex = regex + RECEPTOR_EX;
   }


   public String getRegex() {
      return _regex;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTitle() {
      return _title;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getUri() {
      return _uri;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui( final Value statusValue ) {
      if ( statusValue == StatusValue.POSITIVE ) {
         return _positiveCui;
      } else if ( statusValue == StatusValue.NEGATIVE ) {
         return _negativeCui;
      }
      return _unknownCui;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Matcher getMatcher( final CharSequence lookupWindow ) {
      return _pattern.matcher( lookupWindow );
   }

}
