package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.ArrayList;
import java.util.List;

public class TNMFact extends ConditionFact {


   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private Fact prefix, suffix;

   public TNMFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.TNM_STAGE );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   public Fact getPrefix() {
      return prefix;
   }

   public void setPrefix( final Fact prefix ) {
      this.prefix = prefix;
   }

   public Fact getSuffix() {
      return suffix;
   }

   public void setSuffix( final Fact suffix ) {
      this.suffix = suffix;
   }

   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<>();
      if ( prefix != null ) {
         addContainedFact( facts, prefix );
      }
      if ( suffix != null ) {
         addContainedFact( facts, suffix );
      }
      return facts;
   }

   public String getSummaryText() {
      final StringBuilder sb = new StringBuilder();
      sb.append( getLabel() );
      if ( prefix != null ) {
         sb.append( " | prefix: " )
           .append( getPrefix() );
      }
      if ( suffix != null ) {
         sb.append( " | suffix: " )
           .append( getSuffix() );
      }
      return sb.toString();
   }

   /**
    * @param property -
    * @return a value for this fact for a given property
    */
   public Fact getValue( final String property ) {
      if ( FHIRConstants.HAS_TNM_PREFIX.equals( property ) ) {
         return getPrefix();
      }
      if ( FHIRConstants.HAS_TNM_SUFFIX.equals( property ) ) {
         return getSuffix();
      }
      return this;
   }


}
