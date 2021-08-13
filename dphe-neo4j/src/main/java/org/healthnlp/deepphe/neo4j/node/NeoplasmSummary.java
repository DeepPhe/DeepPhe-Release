package org.healthnlp.deepphe.neo4j.node;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
public class NeoplasmSummary {

   private String id;
   private String classUri;

   private List<NeoplasmSummary> subSummaries;
   private List<NeoplasmAttribute> attributes;

   private String pathologic_t;
   private String pathologic_n;
   private String pathologic_m;
   private String er;
   private String pr;
   private String her2;


   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public String getClassUri() {
      return classUri;
   }

   public void setClassUri( final String classUri ) {
      this.classUri = classUri;
   }

   public List<NeoplasmSummary> getSubSummaries() {
      return subSummaries;
   }

   public void setSubSummaries( final List<NeoplasmSummary> subSummaries ) {
      this.subSummaries = subSummaries;
   }

   public List<NeoplasmAttribute> getAttributes() {
      return attributes;
   }

   public void setAttributes( final List<NeoplasmAttribute> attributes ) {
      this.attributes = attributes;
   }

   public String getPathologic_t() {
      return pathologic_t;
   }

   public void setPathologic_t( final String pathologic_t ) {
      this.pathologic_t = pathologic_t;
   }

   public String getPathologic_n() {
      return pathologic_n;
   }

   public void setPathologic_n( final String pathologic_n ) {
      this.pathologic_n = pathologic_n;
   }

   public String getPathologic_m() {
      return pathologic_m;
   }

   public void setPathologic_m( final String pathologic_m ) {
      this.pathologic_m = pathologic_m;
   }

   public String getEr() {
      return er;
   }

   public void setEr( final String er ) {
      this.er = er;
   }

   public String getPr() {
      return pr;
   }

   public void setPr( final String pr ) {
      this.pr = pr;
   }

   public String getHer2() {
      return her2;
   }

   public void setHer2( final String her2 ) {
      this.her2 = her2;
   }


}
