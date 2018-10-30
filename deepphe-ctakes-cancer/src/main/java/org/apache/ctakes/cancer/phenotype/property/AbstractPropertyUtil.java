package org.apache.ctakes.cancer.phenotype.property;


import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;
import java.util.regex.Matcher;

/**
 * Abstract class with Utilities to interact with neoplasm property annotations, mostly by uri.
 *
 * Specific Property-type implementation singletons should be used to:
 * <ul>
 * test that an annotation is of the desired property {@link #isCorrectProperty(IdentifiedAnnotation)}
 * get the property type uri from text {@link #getTypeUri(String)}
 * get the property value uri from text {@link #getValueUri(String)}
 *</ul>
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
abstract public class AbstractPropertyUtil<T extends Type, V extends Value> {

   static private final Logger LOGGER = Logger.getLogger( "PropertyUtil" );

   private final String _propertyName;

   protected AbstractPropertyUtil( final String propertyName ) {
      _propertyName = propertyName;
   }

   /**
    * @param annotation candidate property annotation
    * @return true if the annotation is of the correct property according to URI match
    */
   abstract public boolean isCorrectProperty( final IdentifiedAnnotation annotation );

   /**
    * @param annotation candidate property annotation
    * @param types      -
    * @return true if the annotation is of the correct property according to URI match
    */
   protected boolean isCorrectProperty( final IdentifiedAnnotation annotation, final T[] types ) {
      final Collection<String> uris = Neo4jOntologyConceptUtil.getUris( annotation );
      for ( String uri : uris ) {
         for ( T type : types ) {
            if ( uri.equals( type.getUri() ) ) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * @param jcas       -
    * @param annotation -
    * @return the Value for the given annotation or an appropriate unknown value
    */
   public V getValue( final JCas jcas, final IdentifiedAnnotation annotation ) {
      if ( !isCorrectProperty( annotation ) ) {
         LOGGER.warn( annotation.getCoveredText() + " is not a " + _propertyName + " annotation" );
         return null;
      }
      final Collection<DegreeOfTextRelation> degrees = JCasUtil.select( jcas, DegreeOfTextRelation.class );
      if ( degrees == null || degrees.isEmpty() ) {
         return getUnknownValue();
      }
      for ( DegreeOfTextRelation degree : degrees ) {
         final Annotation argument1 = degree.getArg1().getArgument();
         if ( !argument1.equals( annotation ) ) {
            continue;
         }
         final Annotation argument2 = degree.getArg2().getArgument();
         final Collection<String> uris = Neo4jOntologyConceptUtil.getUris( (IdentifiedAnnotation)argument2 );
         final V value = getUriValue( uris );
         if ( value != null ) {
            return value;
         }
      }
      return getUnknownValue();
   }

   abstract public String getTypeUri( final String typeText );

   abstract public String getValueUri( final String valueText );

   protected String getTypeUri( final String typeText, final T[] types ) {
      for ( T type : types ) {
         if ( type.getMatcher( typeText ).matches() ) {
            return type.getUri();
         }
      }
      return "";
   }

   protected String getValueUri( final String valueText, final V[] values ) {
      int bestBegin = Integer.MAX_VALUE;
      int bestEnd = Integer.MIN_VALUE;
      V bestValue = null;
      for ( V value : values ) {
         final Matcher valueMatcher = value.getMatcher( valueText );
         if ( valueMatcher.find() ) {
            if ( bestValue == null
                 || (valueMatcher.end() - valueMatcher.start() > bestEnd - bestBegin)
                 || (valueMatcher.start() < bestBegin) ) {
               bestValue = value;
               bestBegin = valueMatcher.start();
               bestEnd = valueMatcher.end();
            }
         }
      }
      if ( bestValue == null ) {
         return getUnknownValue().getUri();
      }
      return bestValue.getUri();
   }



   /**
    * @param uris uris associated with the possible property values
    * @return Value with the given uri(s)
    */
   protected V getUriValue( final Collection<String> uris ) {
      for ( String uri : uris ) {
         final V value = getUriValue( uri );
         if ( !value.equals( getUnknownValue() ) ) {
            return value;
         }
      }
      return null;
   }

   /**
    * @param uri uri associated with the possible property values
    * @return Value with the given uri
    */
   abstract protected V getUriValue( final String uri );

   /**
    * @return an appropriate value to represent unknown
    */
   abstract protected V getUnknownValue();

}
