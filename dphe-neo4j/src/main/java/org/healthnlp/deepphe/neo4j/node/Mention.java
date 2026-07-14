package org.healthnlp.deepphe.neo4j.node;


import org.healthnlp.deepphe.neo4j.node.xn.InfoNode;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/8/2020
 */
public class Mention extends InfoNode {

   transient private String dpheGroup;
   private int begin;
   private int end;
//   private String span;
//   private String temporality;
   transient private String text;
   transient private List<Codification> codifications;
   transient private String value;

   public String getDpheGroup() {
      return dpheGroup;
   }

   public void setDpheGroup( final String semanticGrouping ) {
      this.dpheGroup = semanticGrouping;
   }

//   public String getSpan() {
//      return span;
//   }
//
   public void setSpan( final int begin, final int end ) {
      this.begin = begin;
      this.end = end;
//      span = begin + "," + end;
   }

   public int getBegin() {
      return begin;
   }

//   public void setBegin( final int begin ) {
//      this.begin = begin;
//   }

   public int getEnd() {
      return end;
   }

//   public void setEnd( final int end ) {
//      this.end = end;
//   }

//   public String getTemporality() {
//      return temporality;
//   }
//
//   public void setTemporality( final String temporality ) {
//      this.temporality = temporality;
//   }

   public String getText() {
      return text;
   }

   public void setText( final String text ) {
      this.text = text;
   }

   public List<Codification> getCodifications() {
      return codifications;
   }

   public void setCodifications( final List<Codification> codifications ) {
      this.codifications = codifications;
   }

   public String getValue() {
      return value;
   }

   public void setValue( final String value ) {
      this.value = value;
   }


}
