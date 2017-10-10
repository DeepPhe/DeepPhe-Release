package org.healthnlp.deepphe.uima.drools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.ObservationFact;
import org.healthnlp.deepphe.fhir.fact.FactFactory;
import org.healthnlp.deepphe.util.FHIRConstants;

/**
 * Represents tumors, merged across all documents. Used in drools only
 *
 * @author opm1
 */
public class MergedTumor {
   private String bodySite = "", mergedTumorId, bodySide = "", quadrant = "", clockFacePos = "", histologicType = "";
   private Set<Fact> tumorSiteFactSet, bodySideFactSet, quadrantFactSet, clockfacePosFactSet;
   private Set<Receptor> prStatus, erStatus, her2Status;
   private Set<String> tumorSummaryIdSet;
   private Map<String, List<String>> documentTypeMap = new HashMap<String, List<String>>(); //<docType, List of docID>
   private boolean readyForRetraction = false;
   private boolean fromPathologyReport = false;
   private boolean gotStatistics = false;
   private boolean checkedForPR = false;


   public MergedTumor() {
      setMergedTumorId( "MergedTumor-" + hashCode() );
      tumorSiteFactSet = new HashSet<Fact>();
      bodySideFactSet = new HashSet<Fact>();
      quadrantFactSet = new HashSet<Fact>();
      clockfacePosFactSet = new HashSet<Fact>();
      setTumorSummaryIdSet( new HashSet<String>() );

      prStatus = new HashSet<Receptor>();
      erStatus = new HashSet<Receptor>();
      her2Status = new HashSet<Receptor>();
   }

   public void addReceptorStatus( Fact nameFact, Set<Fact> valueFact, Set<Fact> methodFact ) {
      String receptorStatusName = nameFact.getName();
      Set<Receptor> curSet = getCurrentSet( receptorStatusName );

      if ( curSet != null ) {
         Receptor receptor = new Receptor( nameFact, valueFact, methodFact );
         curSet.add( receptor );
      }
   }

   public Set<Receptor> getCurrentSet( String switchStr ) {
      Set<Receptor> curSet = null;
      switch ( switchStr ) {
         case "Estrogen_Receptor_Status":
            curSet = erStatus;
            break;
         case "Progesterone_Receptor_Status":
            curSet = prStatus;
            break;
         case "HER2_Neu_Status":
            curSet = her2Status;
            break;
      }
      return curSet;
   }

   /**
    * Al Receptor rules are here
    *
    * @param switchStr - receptor status name
    * @return
    */
   public Receptor getBestReceptor( String switchStr ) {
      Set<Receptor> curSet = getCurrentSet( switchStr );
      // get most recent FISH from Pathology report
      Receptor curReceptor = getMostRecentPathologyRS_FISH( curSet );
      // get most recent pathology RS
      if ( curReceptor == null )
         curReceptor = getMostRecentPathologyRS( curSet );
      // get most recent from NOT PR with FISH
      if ( curReceptor == null )
         getMostRecentNotPathologyRS_FISH( curSet );
      // just most recent not from PR
      if ( curReceptor == null )
         getMostRecentNotPathologyRS( curSet );
      return curReceptor;

   }

   public Fact getTumorSummaryReceptor( String statusName, String receptorName, Fact cancerFact ) {
      Receptor receptor = getBestReceptor( receptorName );
      Fact tf = null;
      if ( receptor != null ) {
         Fact recFact = receptor.getNameFact();
         String newId = FHIRConstants.TUMOR_PHENOTYPE + "-" + getMergedTumorId();
         tf = FactFactory.createFact( recFact, recFact.getType(), recFact.getUri(), FHIRConstants.RECORD_SUMMARY );
         tf.addContainerIdentifier( FHIRConstants.TUMOR_SUMMARY + "-" + getMergedTumorId() );
         tf.setIdentifier( tf.getName() + "-" + newId );
         tf.setSummaryId( newId );
         tf.setSummaryType( FHIRConstants.TUMOR_PHENOTYPE );
         tf.setCategory( statusName );
         tf.addContainerIdentifier( FHIRConstants.TUMOR_SUMMARY + "-" + getMergedTumorId() );
         tf.addContainerIdentifier( cancerFact.getIdentifier() );
         tf.addProvenanceFact( recFact );


         //method
         Set<Fact> methods = receptor.getMethods();
         if ( methods.size() > 0 ) {
            Fact mFact = FactFactory.createTumorFactModifier( methods.iterator().next().getUri(), tf,
                  cancerFact, FHIRConstants.TUMOR_PHENOTYPE, FHIRConstants.HAS_METHOD,
                  FHIRConstants.RECORD_SUMMARY, FHIRConstants.DIAGNOSTIC_PROCEDURE );
            mFact.addContainerIdentifier( tf.getSummaryId() );
            ((ObservationFact) tf).setMethod( mFact );

            for ( Fact f : methods )
               mFact.addProvenanceFact( f );
         }

         // interpretation
         Set<Fact> interpretations = receptor.getInterpretations();
         if ( interpretations.size() > 0 ) {

            Fact recValueFact = FactFactory.createTumorFactModifier( interpretations.iterator().next().getUri(), tf,
                  cancerFact, FHIRConstants.TUMOR_PHENOTYPE, FHIRConstants.HAS_INTERPRETATION,
                  FHIRConstants.RECORD_SUMMARY, FHIRConstants.ORDINAL_INTERPRETATION );
            recValueFact.addContainerIdentifier( tf.getSummaryId() );
            ((ObservationFact) tf).setInterpretation( recValueFact );
            for ( Fact f : interpretations )
               recValueFact.addProvenanceFact( f );
         }
      }
      return tf;

   }

   public Receptor getMostRecentPathologyRS( Set<Receptor> curSet ) {
      Receptor toret = null;
      int pos = 0;
      for ( Receptor r : curSet ) {
         if ( pos == 0 && r.isFrojmPathologyReport() ) {
            toret = r;
            pos++;
         } else if ( pos > 0 && toret != null ) {
            if ( r.isFrojmPathologyReport() && toret.getMentionDate().before( r.getMentionDate() ) )
               toret = r;
         }
      }
      return toret;
   }

   public Receptor getMostRecentPathologyRS_FISH( Set<Receptor> curSet ) {
      Receptor toret = null;
      int pos = 0;
      for ( Receptor r : curSet ) {
         if ( pos == 0 && r.isFrojmPathologyReport() && r.isFromFISH() ) {
            toret = r;
            pos++;
         } else if ( pos > 0 && toret != null ) {
            if ( r.isFrojmPathologyReport() && r.isFromFISH() && toret.getMentionDate().before( r.getMentionDate() ) )
               toret = r;
         }
      }
      return toret;
   }

   public Receptor getMostRecentNotPathologyRS_FISH( Set<Receptor> curSet ) {
      Receptor toret = null;
      int pos = 0;
      for ( Receptor r : curSet ) {
         if ( pos == 0 && !r.isFrojmPathologyReport() && r.isFromFISH() ) {
            toret = r;
            pos++;
         } else if ( pos > 0 && toret != null ) {
            if ( !r.isFrojmPathologyReport() && r.isFromFISH() && toret.getMentionDate().before( r.getMentionDate() ) )
               toret = r;
         }
      }
      return toret;
   }

   public Receptor getMostRecentNotPathologyRS( Set<Receptor> curSet ) {
      Receptor toret = null;
      int pos = 0;
      for ( Receptor r : curSet ) {
         if ( pos == 0 && !r.isFrojmPathologyReport() ) {
            toret = r;
            pos++;
         } else if ( pos > 0 && toret != null ) {
            if ( !r.isFrojmPathologyReport() && toret.getMentionDate().before( r.getMentionDate() ) )
               toret = r;
         }
      }
      return toret;
   }

   public void addToDocumentTypeMap( Set<Fact> facts ) {
      for ( Fact f : facts ) {
         addToDocumentTypeMap( f.getDocumentType(), f.getDocumentIdentifier() );
      }
   }

   public void addToDocumentTypeMap( String docType, String docId ) {
      List<String> docList = documentTypeMap.get( docType );
      if ( docList == null ) {
         docList = new ArrayList<String>();
         documentTypeMap.put( docType, docList );
         if ( docType.equals( FHIRConstants.PATHOLOGY_REPORT ) || docType.equals( FHIRConstants.SURGICAL_PATHOLOGY_REPORT ) )
            setFromPathologyReport( true );
      }
      if ( !docList.contains( docId ) )
         docList.add( docId );
   }

   public Map<String, List<String>> getDocumentTypeMap() {
      return documentTypeMap;
   }

   public String getBodySite() {
      return bodySite;
   }

   public void setBodySite( String bodySite ) {
      this.bodySite = bodySite;
   }

   public String getMergedTumorId() {
      return mergedTumorId;
   }

   public void setMergedTumorId( String mergedTumorId ) {
      this.mergedTumorId = mergedTumorId;
   }

   public String getBodySide() {
      return bodySide;
   }

   public void setBodySide( String bodySide ) {
      this.bodySide = bodySide;
   }

   public String getQuadrant() {
      return quadrant;
   }

   public void setQuadrant( String quadrant ) {
      this.quadrant = quadrant;
   }

   public String getClockFacePos() {
      return clockFacePos;
   }

   public void setClockFacePos( String clockFacePos ) {
      this.clockFacePos = clockFacePos;
   }

   public Set<Fact> getTumorSiteFactSet() {
      return tumorSiteFactSet;
   }

   public void setTumorSiteFactSet( Set<Fact> tumorSiteFact ) {
      this.tumorSiteFactSet = tumorSiteFact;
      addToDocumentTypeMap( tumorSiteFact );
   }

   public Set<Fact> getBodySideFactSet() {
      return bodySideFactSet;
   }

   public void setBodySideFactSet( Set<Fact> bodySideFact ) {
      this.bodySideFactSet = bodySideFact;
      addToDocumentTypeMap( bodySideFact );
   }

   public Set<Fact> getQuadrantFactSet() {
      return quadrantFactSet;
   }

   public void setQuadrantFactSet( Set<Fact> quadrantFact ) {
      this.quadrantFactSet = quadrantFact;
      addToDocumentTypeMap( quadrantFact );
   }

   public Set<Fact> getClockfacePosFactSet() {
      return clockfacePosFactSet;
   }

   public void setClockfacePosFactSet( Set<Fact> clockfacePosFact ) {
      this.clockfacePosFactSet = clockfacePosFact;
      addToDocumentTypeMap( clockfacePosFact );
   }

   public String getHistologicType() {
      return histologicType;
   }

   public void setHistologicType( String histologicType ) {
      this.histologicType = histologicType;
   }

   public void addTumorFact( String setId, Fact f ) {
      addToDocumentTypeMap( f.getDocumentType(), f.getDocumentIdentifier() );

      tumorSummaryIdSet.add( f.getSummaryId() );
      switch ( setId ) {
         case FHIRConstants.BODY_SITE:
            tumorSiteFactSet.add( f );
            break;
         case FHIRConstants.LATERALITY:
            bodySideFactSet.add( f );
            break;
         case FHIRConstants.QUADRANT:
            quadrantFactSet.add( f );
            break;
         case FHIRConstants.CLOCKFACE_POSITION:
            clockfacePosFactSet.add( f );
            break;
      }
   }

   public String getInfo() {
      StringBuffer b = new StringBuffer();
      b.append( "mergedTumorId: " + mergedTumorId + "|" );
      b.append( "histologicType: " + getHistologicType() + "|" );
      b.append( "bodySite: " + getBodySite() + "|" );
      b.append( "Laterality: " + getBodySide() + "|" );
      b.append( "Quadrant: " + getQuadrant() + "|" );
      b.append( "clockFacePos: " + getClockFacePos() + "\n" );

      b.append( "Doc TumorSummaryIds: " );
      for ( String s : tumorSummaryIdSet ) {
         b.append( s + ", " );
      }
      b.append( "\n" );
      if ( bodySideFactSet != null )
         b.append( "bodySideFacts: " + bodySideFactSet.size() + "\n" );
      if ( quadrantFactSet != null )
         b.append( "quadrantFacts: " + quadrantFactSet.size() + "\n" );
      if ( clockfacePosFactSet != null )
         b.append( "clockfacePosFacts: " + clockfacePosFactSet.size() + "\n" );
      return b.toString();
   }

   public String toString() {
      return getInfo();
   }

   public boolean isReadyForRetraction() {
      return readyForRetraction;
   }

   public void setReadyForRetraction( boolean readyForRetraction ) {
      this.readyForRetraction = readyForRetraction;
   }

   public Set<String> getTumorSummaryIdSet() {
      return tumorSummaryIdSet;
   }

   public void setTumorSummaryIdSet( Set<String> tumorSummaryIdSet ) {
      this.tumorSummaryIdSet = tumorSummaryIdSet;
   }

   public boolean isFromPathologyReport() {
      return fromPathologyReport;
   }

   public void setFromPathologyReport( boolean fromPathologyReport ) {
      this.fromPathologyReport = fromPathologyReport;
   }

   public String getDocStatistics() {
      System.out.println( "for " + mergedTumorId + " map: " + documentTypeMap );
      StringBuffer b = new StringBuffer();
      b.append( "\n=====================\n" );
      b.append( "Tumor ID: " + mergedTumorId + "\n" );
      b.append( "To be removed?: " + !fromPathologyReport + "\n" );
      b.append( "BodySite: " + bodySite + "|" );
      b.append( "Laterality: " + bodySide + "\n" );
      String[] docTypes = { FHIRConstants.PATHOLOGY_REPORT, FHIRConstants.SURGICAL_PATHOLOGY_REPORT, FHIRConstants.RADIOLOGY_REPORT, FHIRConstants.PROGRESS_NOTE_REPORT };
      int count = 0;
      for ( String dt : docTypes ) {
         try {
            count = documentTypeMap.get( dt ).size();
         } catch ( NullPointerException e ) {
            count = 0;
         }
         b.append( dt + ": " + count + "|" );
      }
      b.append( "\n=====================\n" );
      return b.toString();

   }

   public boolean isGotStatistics() {
      return gotStatistics;
   }

   public void setGotStatistics( boolean gotStatistics ) {
      this.gotStatistics = gotStatistics;
   }

   public boolean isCheckedForPR() {
      return checkedForPR;
   }

   public void setCheckedForPR( boolean checkedForPR ) {
      this.checkedForPR = checkedForPR;
   }

}