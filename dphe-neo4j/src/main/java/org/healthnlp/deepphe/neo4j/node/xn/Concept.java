package org.healthnlp.deepphe.neo4j.node.xn;

import org.healthnlp.deepphe.neo4j.node.Codification;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {12/13/2023}
 */
public class Concept extends InfoNode {

   private String dpheGroup;
   private String preferredText;
   private List<String> mentionIds;
   private List<Codification> codifications;


   public String getDpheGroup() {
      return dpheGroup;
   }

   public void setDpheGroup( final String semanticGrouping ) {
      this.dpheGroup = semanticGrouping;
   }

   public String getPreferredText() {
      return preferredText;
   }

   public void setPreferredText( final String preferredText ) {
      this.preferredText = preferredText;
   }

   public List<String> getMentionIds() {
      return mentionIds;
   }

   public void setMentionIds(final List<String> mentionIds) {
      this.mentionIds = mentionIds;
   }

   public List<Codification> getCodifications() {
      return codifications;
   }

   public void setCodifications( final List<Codification> codifications ) {
      this.codifications = codifications;
   }

//   public void setdConfidence( final double dConfidence ) {
//      super.setdConfidence( dConfidence );
//      setConfidence( (int)Math.round( dConfidence*100 ) );
//   }

}
