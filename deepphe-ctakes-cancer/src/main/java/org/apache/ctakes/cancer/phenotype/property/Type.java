package org.apache.ctakes.cancer.phenotype.property;


import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;

import java.util.regex.Matcher;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public interface Type {

   String getTitle();

   String getUri();

   default <T extends Value> String getCui( final T value ) {
      return Neo4jOntologyConceptUtil.getCui( getUri() );
   }

   default String getTui() {
      return "";
   }

   Matcher getMatcher( final CharSequence lookupWindow );

}
