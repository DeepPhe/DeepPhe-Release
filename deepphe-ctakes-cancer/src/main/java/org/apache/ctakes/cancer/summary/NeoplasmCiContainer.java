package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.UriConstants;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2019
 */
final public class NeoplasmCiContainer extends AbstractCiContainer {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmCiSummary" );


   static public final String TYPE_BENIGN = "Benign";
   static public final String TYPE_CANCER = "Cancer";
   static public final String TYPE_NON_CANCER = "Non_Cancer";
   static public final String TYPE_PRIMARY = "Primary";
   static public final String TYPE_METASTASIS = "Metastasis";
   static public final String TYPE_GENERIC = "Generic";

   static private AtomicLong _ID_NUM = new AtomicLong( 0 );

   final private ConceptInstance _conceptInstance;


   public NeoplasmCiContainer( final String type, final ConceptInstance neoplasm ) {
      super( type, neoplasm.getUri(), neoplasm.getId() );
      _conceptInstance = neoplasm;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   final long createUniqueIdNum() {
      return _ID_NUM.incrementAndGet();
   }


   /**
    * @return core conceptInstance
    */
   public ConceptInstance getConceptInstance() {
      return _conceptInstance;
   }


   /**
    * @return Relations in the summary as Concept Instances.
    */
   public Map<String, Collection<ConceptInstance>> getRelations() {
      return _conceptInstance.getRelated();
   }

   public Map<String,String> getProperties() {

      final Map<String,String> properties = new HashMap<>( 3 );
      final boolean inHistory = _conceptInstance.inPatientHistory();
      final String historic = inHistory ? "Historic" : "Current";
      properties.put( RelationConstants.HAS_HISTORICITY, historic );
      properties.put( RelationConstants.HAS_HISTOLOGY, CiSummaryUtil.getHistology( _conceptInstance.getUri() ) );
      properties.put( RelationConstants.HAS_CANCER_TYPE, CiSummaryUtil.getCancerType( _conceptInstance.getUri() ) );
      return properties;
   }

   public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "CiContainer Type: " ).append( getType() )
        .append( "   URI: " ).append( getUri() )
        .append( "   ID: " ).append( getId() ).append( "\n" );
      for ( Map.Entry<String,String> property : getProperties().entrySet() ) {
         sb.append( property.getKey() ).append( " : " ).append( property.getValue() ).append( "\n" );
      }
      final Collection<ConceptInstance>  breakers = new ArrayList<>();
      sb.append( appendRelated( _conceptInstance, "  ", breakers ) );
      return sb.toString();
   }

   static private String appendRelated( final ConceptInstance instance, final String indent, final Collection<ConceptInstance> breakers ) {
      breakers.add( instance );
      final StringBuilder sb = new StringBuilder();
      sb.append( indent ).append( instance.toShortText() ).append( "\n" );
      for ( String relationName : instance.getRelated().keySet().stream().sorted().collect( Collectors.toList() ) ) {
         sb.append( indent ).append( " " ).append( relationName ).append( " :\n" );
         for ( ConceptInstance related : instance.getRelated().get( relationName ) ) {
               sb.append( indent ).append( "  " ).append( related.toShortText() ).append( "   " ).append( related.getId() ).append( "\n" );
         }
      }

      return sb.toString();
   }


}
