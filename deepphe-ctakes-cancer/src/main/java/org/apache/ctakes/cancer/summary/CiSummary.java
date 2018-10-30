package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.ae.ByUriRelationFinder;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps related ConceptInstances into a summary.
 * A summary typically represents a Dphe Cancer, Dphe primary tumor, Dphe metastatic tumor.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/5/2018
 */
@Immutable
final public class CiSummary {

   static private final Logger LOGGER = Logger.getLogger( "CiSummary" );

   static public final String TYPE_CANCER = "Cancer";
   static public final String TYPE_PRIMARY = "Primary";
   static public final String TYPE_METASTASIS = "Metastasis";
   static public final String TYPE_GENERIC = "Generic";


   final private String _type;
   final private String _uri;
   final private Collection<ConceptInstance> _conceptInstances;

   /**
    * @param uri              type uri
    * @param conceptInstances Collection of related Concept Instances within summary.
    */
   public CiSummary( final String uri, final Collection<ConceptInstance> conceptInstances ) {
      this( TYPE_GENERIC, uri, conceptInstances );
   }

   /**
    * @param type             type name for summary.
    * @param uri              type uri
    * @param conceptInstances Collection of related Concept Instances within summary.
    */
   public CiSummary( final String type, final String uri, final Collection<ConceptInstance> conceptInstances ) {
      _type = type;
      _uri = uri;
      _conceptInstances = conceptInstances;
   }

   /**
    * @return some unique id for this summary.
    */
   public String getId() {
      final String patient = getConceptInstances().stream()
                                              .map( ConceptInstance::getPatientId )
                                              .distinct()
                                              .sorted()
                                              .collect( Collectors.joining( "_" ) );
      final String docs = getConceptInstances().stream()
                                               .map( ConceptInstance::getDocumentIds )
                                               .flatMap( Collection::stream )
                                               .distinct()
                                               .sorted()
                                               .collect( Collectors.joining( "_" ) );
      return patient + "_" + docs + "_" + getType() + "_" + getUri() + "_" + Math.abs( getConceptInstances().hashCode() );
   }

   /**
    * @return type name for summary.
    */
   public String getType() {
      return _type;
   }

   /**
    * @return Collection of related Concept Instances within summary.
    */
   public Collection<ConceptInstance> getConceptInstances() {
      return _conceptInstances;
   }

   /**
    * @return the most specific uri for the summary.
    */
   public String getUri() {
      return ConceptInstanceFactory.getMostSpecificUri( getAllUris() );
   }

   /**
    * @return all uris for all concept instances in the summary.
    */
   public Collection<String> getAllUris() {
      return getConceptInstances().stream()
                              .map( ConceptInstance::getUri )
                              .distinct()
                              .collect( Collectors.toList() );
   }

   /**
    * @return Relations in the summary as Uris.
    */
   public Map<String, Collection<String>> getRelationUris() {
      final Map<String, Collection<ConceptInstance>> allRelations = getRelations();
      final Map<String, Collection<String>> uriRelations = new HashMap<>( allRelations.size() );
      for ( Map.Entry<String, Collection<ConceptInstance>> relations : allRelations.entrySet() ) {
         final String relationName = relations.getKey();
         final Collection<String> uris = relations.getValue().stream()
                                                  .map( ConceptInstance::getUri )
                                                  .distinct()
                                                  .collect( Collectors.toList() );
         final Map<String, Collection<String>> collatedRelationUris = ByUriRelationFinder.collateUris( uris );
         for ( Collection<String> collatedUris : collatedRelationUris.values() ) {
            uriRelations.computeIfAbsent( relationName, r -> new ArrayList<>() )
                        .add( ConceptInstanceFactory.getMostSpecificUri( collatedUris ) );
         }
      }
      return uriRelations;
   }

   /**
    * @return Relations in the summary as Concept Instances.
    */
   public Map<String, Collection<ConceptInstance>> getRelations() {
      final Map<String, Collection<ConceptInstance>> allRelations = new HashMap<>();
      for ( ConceptInstance instance : getConceptInstances() ) {
         for ( Map.Entry<String, Collection<ConceptInstance>> instanceRelations : instance.getRelated().entrySet() ) {
            allRelations.computeIfAbsent( instanceRelations.getKey(), ci -> new HashSet<>() )
                        .addAll( instanceRelations.getValue() );
         }
      }
      return allRelations;
   }

   /**
    * Histology most likely does NOT have explicit representation in the document.
    * However, we have a {@link RelationConstants#HAS_HISTOLOGY} relation / attribute.
    *
    * @return String for the histology or an empty string.
    */
   public String getHistology() {
      for ( Map.Entry<String, Collection<String>> histologies : UriConstants.getHistologyMap().entrySet() ) {
         if ( histologies.getValue().contains( getUri() ) ) {
            return histologies.getKey();
         }
      }
      return "";
   }

   /**
    * Cancer type most likely does NOT have explicit representation in the document.
    *
    * @return String for the histology or an empty string.
    */
   public String getCancerType() {
      for ( Map.Entry<String, Collection<String>> cancerTypes : UriConstants.getCancerTypeMap().entrySet() ) {
         if ( cancerTypes.getValue().contains( getUri() ) ) {
            return cancerTypes.getKey();
         }
      }
      return "";
   }

   public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "CiSummary Type: " ).append( getType() )
        .append( "   URI: " ).append( getUri() )
        .append( "   ID: " ).append( getId() ).append( "\n" );
      final Collection<ConceptInstance>  breakers = new ArrayList<>();
      for ( ConceptInstance instance : getConceptInstances().stream()
                                                            .sorted( Comparator.comparing( ConceptInstance::getUri ) )
                                                            .collect( Collectors.toList() ) ) {
         sb.append( appendRelated( instance, "  ", breakers ) );
      }
      return sb.toString();
   }

   static private String appendRelated( final ConceptInstance instance, final String indent, final Collection<ConceptInstance> breakers ) {
      breakers.add( instance );
      final StringBuilder sb = new StringBuilder();
      sb.append( indent ).append( instance.toShortText() ).append( "\n" );
      for ( String relationName : instance.getRelated().keySet().stream().sorted().collect( Collectors.toList() ) ) {
         sb.append( indent ).append( " " ).append( relationName ).append( " :\n" );
         for ( ConceptInstance related : instance.getRelated().get( relationName ) ) {
            if ( breakers.contains( related ) ) {
               sb.append( indent ).append( "  " ).append( related.toShortText() ).append( "\n" );
            } else {
               sb.append( appendRelated( related, indent+"  ", breakers ) );
            }
         }
      }

      for ( String relationName : instance.getReverseRelated().keySet().stream().sorted().collect( Collectors.toList() ) ) {
         sb.append( indent ).append( "-" ).append( relationName ).append( " :\n" );
         for ( ConceptInstance related : instance.getReverseRelated().get( relationName ) ) {
            if ( breakers.contains( related ) ) {
               sb.append( indent ).append( "--" ).append( related.toShortText() ).append( "\n" );
            } else {
               sb.append( appendRelated( related, indent+"--", breakers ) );
            }
         }
      }

      return sb.toString();
   }

}
