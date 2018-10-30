package org.healthnlp.deepphe.fact;


import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.healthnlp.deepphe.neo4j.Neo4jRelationUtil;
import org.healthnlp.deepphe.summary.CancerSummary;
import org.healthnlp.deepphe.summary.Summary;
import org.healthnlp.deepphe.summary.TumorSummary;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.neo4j.graphdb.GraphDatabaseService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Loads Fact templates
 *
 * @author opm1
 */
public class FactHelper {
	
	public static String DROOLS_INFO_HEADER = "name|uri|id|category|type|patientId|documentId|documenType|"+
			"documentTtle|summaryType|summaryId|containerIds|recordDate|ancestors";
	

   public static void addFactToSummary( final Fact fact,
                                        final CancerSummary cancerSummary,
                                        final String tumorResourceId ) {
      addFactToSummary( fact, cancerSummary, tumorResourceId, "" );
   }

   public static void addFactToSummary( final Fact fact,
                                        final CancerSummary cancerSummary,
                                        final String tumorResourceId,
                                        final String ontologyURI ) {
      final String summaryType = fact.getSummaryType();
      switch ( summaryType ) {
         case "CancerSummary":
            cancerSummary.addFact( fact.getCategory(), fact );
            break;
         case "TumorSummary":
            getTumorSummary( cancerSummary, tumorResourceId ).addFact( fact.getCategory(), fact );
            break;
      }
   }

   public static TumorSummary getTumorSummary( final CancerSummary cancerSummary,
                                               final String tumorResourceId ) {
      TumorSummary tumor = cancerSummary.getTumorSummaryByIdentifier( tumorResourceId );
      if ( tumor != null ) {
         return tumor;
      }
      tumor = new TumorSummary( tumorResourceId );
      loadTemplate( tumor );
      cancerSummary.addTumor( tumor );
      return tumor;
   }

   /**
    * load template based on the ontology
    *
    * @param summary -
    */
   public static void loadTemplate( final Summary summary ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      // now lets pull all of the properties
      final Map<String, Collection<String>> possibleRelations
            = Neo4jRelationUtil.getRelatedClassUris( graphDb, summary.getConceptURI() );
      final Map<String, FactList> content = summary.getContent();
      for ( Map.Entry<String, Collection<String>> relations : possibleRelations.entrySet() ) {
         final String category = relations.getKey();
         final FactList factList = content.computeIfAbsent( category, c -> new DefaultFactList( category ) );
         relations.getValue().forEach( factList::addType );
      }
   }

   // This method should only be used in test code, not in the full system.
   // We always want to create a fact from a ConceptInstance.
   public static Fact stringToDroolsFact(String str) {
	   	return stringToDroolsFact(str, "default");
   }

    // This method should only be used in test code, not in the full system.
    // We always want to create a fact from a ConceptInstance.
   public static Fact stringToDroolsFact(String str, String type) {
	   //workaround date formatting: should be no HH:mm:ss:z 
	   SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
	   String NEW_FORMAT = "MMM dd yyyy";
	   SimpleDateFormat ndf = new SimpleDateFormat(NEW_FORMAT);
       sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
	   String[] strArr = str.split("\\|");
	   Fact f = null;
	   switch(type) {
	   	case FHIRConstants.BODY_SITE:
		   f = new BodySiteFact(null);
		   break;
		   
	   		default:
		   f = new Fact();
	   }
	   f.setName(strArr[0]);
       f.setUri(strArr[1]);
       String id = strArr[2];
       if(id == null || "".equals(id))
    	   id = UUID.randomUUID().toString();
       f.setIdentifier(id);
       f.setCategory(strArr[3]);
       f.setType(strArr[4]);
       f.setPatientIdentifier(strArr[5]);
       f.setDocumentIdentifier(strArr[6]);
       f.setDocumentType(strArr[7]);
       f.setDocumentTitle(strArr[8]);
       f.setSummaryType(strArr[9]);
       f.setSummaryId(strArr[10]);
       
       String[] cIdentArr = strArr[11].split(",");
       for( String s: cIdentArr)
       		f.addContainerIdentifier(s.trim());
       
		try {
			Date d = sdf.parse(strArr[12]);
			sdf.applyPattern(NEW_FORMAT);		
			f.setRecordedDate(ndf.parse(sdf.format(d)));
			
		} catch (java.lang.IllegalArgumentException e) { } 
		  catch (ParseException e) { }
		
       String[] aIdentArr = strArr[13].split(",");
       for( String s: aIdentArr)
    	   f.getAncestors().add(s.trim());
       
       
       return f;
   }


  static public boolean isEvent( final Fact fact ) {
       if (fact.getName().equalsIgnoreCase("Event")) {
            return true;
       }
       return false;
  }


   static public boolean isMissingFact( final List<Fact> list, final Fact fact ) {
      return list.stream()
                 .noneMatch( f -> f.equivalent( fact ) );
   }

}
