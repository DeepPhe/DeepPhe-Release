package org.apache.ctakes.cancer.phenotype;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/26/2017
 */
final public class PhenotypeConcept {

   static private final Logger LOGGER = Logger.getLogger( "PhenotypeConcept" );

   public final String __uri;
   public final String __cui;
   public final String __tui;
   public final String __preferredText;

   public PhenotypeConcept( final IClass iClass, final Concept iConcept ) {
      __uri = OwlParserUtil.getUriString( iClass );
      __cui = OwlParserUtil.getCui( iConcept );
      __tui = OwlParserUtil.getTui( iConcept );
      __preferredText = OwlParserUtil.getPreferredText( iConcept );
   }

}
