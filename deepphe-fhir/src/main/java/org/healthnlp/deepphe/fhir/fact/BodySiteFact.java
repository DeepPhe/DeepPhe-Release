package org.healthnlp.deepphe.fhir.fact;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.OntologyUtils;

import java.util.ArrayList;
import java.util.List;

public class BodySiteFact extends Fact {
   private FactList modifiers;
   private Fact side;

   public BodySiteFact() {
      setType( FHIRConstants.BODY_SITE );
   }

   public FactList getModifiers() {
      if ( modifiers == null ) {
         modifiers = new DefaultFactList();
      }
      return modifiers;
   }

   public void setModifiers( FactList modifiers ) {
      this.modifiers = modifiers;
   }

   public void addModifier( Fact fact ) {
      getModifiers().add( fact );
      if ( FactFactory.isBodySide( fact ) )
         side = fact;
   }

   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<Fact>();
      for ( Fact fact : getModifiers() ) {
         addContainedFact( facts, fact );
      }
      return facts;
   }

   public String getFullName() {
      StringBuffer s = new StringBuffer();
      if ( side != null )
         s.append( side.getName() + "_" );
      s.append( getName() );
      return s.toString();
   }

   /**
    * get a value for this fact for a given property
    *
    * @param property
    * @return
    */
   public Fact getValue( String property ) {
      if ( FHIRConstants.HAS_LATERALITY.equals( property ) )
         return getBodySide();
      if ( FHIRConstants.HAS_QUADRANT.equals( property ) )
         return getQuadrant();
      if ( FHIRConstants.HAS_CLOCKFACE.equals( property ) )
         return getClockfacePosition();
      return this;
   }

   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getName() );
      for ( Fact f : getModifiers() ) {
         b.append( " | modifier: " + f.getName() );
      }
      return b.toString();
   }

   public Fact getClockfacePosition() {
      if ( OntologyUtils.hasInstance() ) {
         for ( Fact m : getModifiers() ) {
            if ( OntologyUtils.getInstance().hasSuperClass( m, FHIRConstants.CLOCKFACE_POSITION ) )
               return m;
         }
      }
      return null;
   }

   public Fact getQuadrant() {
      if ( OntologyUtils.hasInstance() ) {
         for ( Fact m : getModifiers() ) {
            if ( OntologyUtils.getInstance().hasSuperClass( m, FHIRConstants.QUADRANT ) )
               return m;
         }
      }
      return null;
   }

   public Fact getBodySide() {
      return side;
   }

   public void setBodySide( Fact fact ) {
      side = fact;
      if ( fact != null ) {
         addModifier( fact );
      }
   }

   public boolean equivalent( BodySiteFact fact ) {
      if ( super.equivalent( fact ) ) {
         if ( getBodySide() != null && fact.getBodySide() != null && getBodySide().equivalent( fact.getBodySide() ) ) {
            return true;
         }
      }
      return false;
   }

   public void append( Fact fact ) {
      if ( fact instanceof BodySiteFact && equivalent( (BodySiteFact) fact ) ) {
         super.append( fact );
         BodySiteFact bfact = (BodySiteFact) fact;
         for ( Fact bf : bfact.getModifiers() ) {
            if ( !FactHelper.contains( getModifiers(), bf ) ) {
               addModifier( bf );
            }
         }
      }
   }

   public String getInfo() {
      String info = super.getInfo();
      return info.replace( "\n", "|" ) + "side: " + side + "\n";
   }
}
