package org.healthnlp.deepphe.fhir.fact;

import java.util.ArrayList;
import java.util.List;

import org.healthnlp.deepphe.util.FHIRConstants;

public class TNMFact extends ConditionFact {
   private Fact prefix, suffix;

   public TNMFact() {
      setType( FHIRConstants.TNM_STAGE );
   }

   public Fact getPrefix() {
      return prefix;
   }

   public void setPrefix( Fact prefix ) {
      this.prefix = prefix;
   }

   public Fact getSuffix() {
      return suffix;
   }

   public void setSuffix( Fact suffix ) {
      this.suffix = suffix;
   }

   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<Fact>();
      if ( prefix != null )
         addContainedFact( facts, prefix );
      if ( suffix != null )
         addContainedFact( facts, suffix );
      return facts;
   }

   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getLabel() );
      if ( prefix != null )
         b.append( " | prefix: " + getPrefix() );
      if ( suffix != null )
         b.append( " | suffix: " + getSuffix() );
      return b.toString();
   }

   /**
    * get a value for this fact for a given property
    *
    * @param property
    * @return
    */
   public Fact getValue( String property ) {
      if ( FHIRConstants.HAS_TNM_PREFIX.equals( property ) )
         return getPrefix();
      if ( FHIRConstants.HAS_TNM_SUFFIX.equals( property ) )
         return getSuffix();
      return this;
   }
}
