package org.healthnlp.deepphe.uima.drools;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.util.FHIRConstants;

public class Receptor {
   private Fact nameFact;
   private Set<Fact> interprFacts, methodFacts;

   private boolean fromPathologyReport = false;
   private boolean fromFISH = false;

   Date mentionDate;

   public Receptor( Fact nameFact, Set<Fact> interprFacts, Set<Fact> methodFacts ) {
      this.nameFact = nameFact;
      this.interprFacts = interprFacts;
      this.methodFacts = methodFacts;
      setParams();
   }

   private void setParams() {
      mentionDate = (Date) nameFact.getRecordedDate();

      String docType = nameFact.getDocumentType();
      if ( docType.equals( FHIRConstants.PATHOLOGY_REPORT ) || docType.equals( FHIRConstants.SURGICAL_PATHOLOGY_REPORT ) )
         setFromPathologyReport( true );

      String method = getValue( FHIRConstants.HAS_METHOD );
      if ( method != null && !method.startsWith( "Immunohistochemical" ) )
         setFromFISH( true );
   }

   public Fact getNameFact() {
      return nameFact;
   }

   public Set<Fact> getInterpretations() {
      return interprFacts;
   }

   public Set<Fact> getMethods() {
      return methodFacts;
   }

	/*public String getMethod(){
      return getValue(FHIRConstants.HAS_METHOD);
	}
	
	public String getInterpretation(){
		return getValue(FHIRConstants.HAS_INTERPRETATION);
	}*/

   public Date getMentionDate() {
      return mentionDate;
   }

   public void setFromPathologyReport( boolean b ) {
      fromPathologyReport = b;
   }

   public boolean isFrojmPathologyReport() {
      return fromPathologyReport;
   }

   public void setFromFISH( boolean b ) {
      fromFISH = b;
   }

   public boolean isFromFISH() {
      return fromFISH;
   }


   public String getName() {
      if ( nameFact != null )
         return nameFact.getName();
      else return null;
   }

   public String getValue( String switchStr ) {
      String toret = null;
      Set<Fact> curSet = null;
      switch ( switchStr ) {
         case FHIRConstants.HAS_INTERPRETATION:
            curSet = interprFacts;
            break;
         case FHIRConstants.HAS_METHOD:
            curSet = methodFacts;
            break;
      }
      if ( curSet != null ) {
         boolean hasError = false;
         int pos = 0;
         for ( Fact f : curSet ) {
            if ( pos == 0 )
               toret = f.getName();
            else {
               if ( !hasError && !toret.equals( f.getName() ) )
                  hasError = true;
            }
         }

         if ( hasError ) {
            System.err.println( "RECEPTOR FACT: " + nameFact.getInfo() + " has different " + switchStr + ":" );
            for ( Fact f : curSet ) {
               System.err.println( switchStr + " FACT: " + f.getInfo() );
            }
         }
      }

      return toret;
   }

}
