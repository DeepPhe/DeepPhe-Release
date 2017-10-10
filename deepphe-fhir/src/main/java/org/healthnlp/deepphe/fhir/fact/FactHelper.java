package org.healthnlp.deepphe.fhir.fact;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.healthnlp.deepphe.fhir.summary.CancerSummary;
import org.healthnlp.deepphe.fhir.summary.Summary;
import org.healthnlp.deepphe.fhir.summary.TumorPhenotype;
import org.healthnlp.deepphe.fhir.summary.TumorSummary;
import org.healthnlp.deepphe.util.FHIRConstants;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;


/**
 * Loads Fact templates
 *
 * @author opm1
 */
public class FactHelper {
   private static Map<String, IOntology> ontologiesMap = new HashMap<String, IOntology>();

   public static IOntology getOntology( String ontologyURI ) {
      IOntology ontology = null;
      if ( ontologiesMap.get( ontologyURI ) == null ) {
         try {
            ontology = OOntology.loadOntology( ontologyURI );
            ontologiesMap.put( ontologyURI, ontology );
         } catch ( IOntologyException e ) {
            e.printStackTrace();
         }
      }
      return ontology;
   }

   /**
    * load template based on the ontology
    *
    * @param summary
    */
   public static void loadTemplate( Summary summary, String ontologyURI ) {
      IOntology ontology = getOntology( ontologyURI );
      IClass summaryClass = ontology.getClass( "" + summary.getConceptURI() );
      if ( summaryClass != null ) {
         // see if there is a more specific
         for ( IClass cls : summaryClass.getDirectSubClasses() ) {
            summaryClass = cls;
            break;
         }

         // now lets pull all of the properties
         for ( Object o : summaryClass.getNecessaryRestrictions() ) {
            if ( o instanceof IRestriction ) {
               IRestriction r = (IRestriction) o;
               if ( isSummarizableRestriction( r, ontology ) ) {
                  if ( !summary.getContent().containsKey( r.getProperty().getName() ) ) {
                     FactList facts = new DefaultFactList();
                     facts.setCategory( r.getProperty().getName() );
                     facts.setTypes( getClassNames( r.getParameter(), ontology ) );
                     summary.getContent().put( r.getProperty().getName(), facts );
                  } else {
                     for ( String type : getClassNames( r.getParameter(), ontology ) ) {
                        summary.getContent().get( r.getProperty().getName() ).getTypes().add( type );
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * should this restriction be used for summarization
    *
    * @param r
    * @return
    */
   private static boolean isSummarizableRestriction( IRestriction r, IOntology ontology ) {
      IClass bs = ontology.getClass( FHIRConstants.BODY_SITE );
      IClass event = ontology.getClass( FHIRConstants.EVENT );

      if ( r.getProperty().isObjectProperty() ) {
         for ( String name : getClassNames( r.getParameter(), ontology ) ) {
            IClass cls = ontology.getClass( name );
            return cls.hasSuperClass( event ) || cls.equals( bs ) || cls.hasSuperClass( bs );
         }
      }
      return false;
   }

   private static List<String> getClassNames( ILogicExpression exp, IOntology ontology ) {
      List<String> list = new ArrayList<String>();
      for ( Object o : exp ) {
         if ( o instanceof IClass ) {
            list.add( ((IClass) o).getName() );
         }
      }
      return list;
   }


   public static void addFactToSummary( Fact fact, CancerSummary cancerSummary, String tumorResourceId, String ontologyURI ) {
      String summaryType = fact.getSummaryType();
      switch ( summaryType ) {
         case "CancerSummary":
            cancerSummary.addFact( fact.getCategory(), fact );
            break;
         case "CancerPhenotype":
            cancerSummary.getPhenotype().addFact( fact.getCategory(), fact );
            break;
         case "TumorSummary":
            getTumorSummary( cancerSummary, tumorResourceId, ontologyURI ).addFact( fact.getCategory(), fact );
            break;
         case "TumorPhenotype":
            getTumorSummary( cancerSummary, tumorResourceId, ontologyURI ).getPhenotype().addFact( fact.getCategory(), fact );
            break;

      }
   }


   public static TumorSummary getTumorSummary( CancerSummary cancerSummary, String tumorResourceId, String ontologyURI ) {
      TumorSummary tumor = cancerSummary.getTumorSummaryByIdentifier( tumorResourceId );
      if ( tumor == null ) {
         tumor = new TumorSummary( tumorResourceId );
         loadTemplate( tumor, ontologyURI );
         TumorPhenotype phenotype = tumor.getPhenotype();
         loadTemplate( phenotype, ontologyURI );
         tumor.setResourceIdentifier( tumorResourceId );
         cancerSummary.addTumor( tumor );
      }
      return tumor;
   }


   public static boolean contains( FactList list, Fact fact ) {
      for ( Fact f : list ) {
         if ( f.equivalent( fact ) )
            return true;
      }
      return false;
   }

}
