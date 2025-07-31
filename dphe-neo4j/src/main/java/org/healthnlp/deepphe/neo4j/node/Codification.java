package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
public class Codification {

   private String source;

   private List<String> codes;


   public String getSource() {
      return source;
   }

   public void setSource( final String source ) {
      this.source = source;
   }

   public List<String> getCodes() {
      return codes;
   }

   public void setCodes( final List<String> codes ) {
      this.codes = codes;
   }

}
