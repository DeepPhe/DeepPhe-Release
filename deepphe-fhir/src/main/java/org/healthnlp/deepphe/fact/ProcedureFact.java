package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.ArrayList;
import java.util.List;

public class ProcedureFact extends Fact implements MethodOwner, BodySiteOwner {


   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private FactList bodySite;
   private Fact method;

   public ProcedureFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.PROCEDURE );
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

   @Override
   public FactList getBodySite() {
      if ( bodySite == null ) {
         bodySite = new DefaultFactList();
         bodySite.addType( FHIRConstants.BODY_SITE );
      }
      return bodySite;
   }

   @Override
   public void setBodySite( final FactList bodySite ) {
      this.bodySite = bodySite;
   }

   @Override
   public Fact getMethod() {
      return method;
   }

   @Override
   public void setMethod( final Fact method ) {
      this.method = method;
   }

   @Override
   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<>();
      if ( method != null ) {
         addContainedFact( facts, method );
      }
      for ( Fact fact : getBodySite() ) {
         addContainedFact( facts, fact );
      }
      return facts;
   }

   public String getSummaryText() {
      return getLabel() + getBodySiteSnippet() + getMethodSnippet();
   }

   public void append( final Fact fact ) {
      if ( !ProcedureFact.class.isInstance( fact ) ) {
         return;
      }
      super.append( fact );
      final ProcedureFact otherProcedure = (ProcedureFact)fact;
      appendBodySites( otherProcedure );
      setOrAppendMethod( otherProcedure.getMethod() );
   }

   /**
    * @return "Procedure"
    */
   protected String createDefaultType() {
      return FHIRConstants.PROCEDURE;
   }

   protected String createDefaultCategory() {
      return FHIRConstants.HAS_METHOD;
   }

}
