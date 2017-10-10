package org.apache.ctakes.cancer.phenotype.property;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;

import java.util.regex.Matcher;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public interface Value {

   String getTitle();
   // Can use the ontology:  iClass.getConcept().getPreferredTerm()

   String getUri();

   default String getCui() {
      final IClass iClass = OwlOntologyConceptUtil.getIClass( getUri() );
      if ( iClass == null ) {
         return null;
      }
      return OwlParserUtil.getCui( iClass );
   }

   default String getTui() {
      final IClass iClass = OwlOntologyConceptUtil.getIClass( getUri() );
      if ( iClass == null ) {
         return null;
      }
      return OwlParserUtil.getTui( iClass );
   }

   Matcher getMatcher( final CharSequence lookupWindow );

}
