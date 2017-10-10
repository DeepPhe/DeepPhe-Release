package org.healthnlp.deepphe.uima.ae;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

public class GraphDBConstants {


   public static final String PROPERTY_IDENTIFIER = "id";
   public static final String PROPERTY_URI = "uri";

   public static RelationshipType getRelationship( final String category ) {
      return new RelationshipType() {
         @Override
         public String name() {
            return category;
         }
      };
   }

   public enum Nodes implements Label {
      Episode, Cancer, Tumor, Patient, Report, TextMention, Fact
   }

   public enum Relationships implements RelationshipType {
      hasReport, hasCancer, hasTumor, hasEpisode, hasProvenance, hasTextProvenance
   }
}
