package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.phenotype.property.AbstractPropertyUtil;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

/**
 * Singleton class with Utilities to interact with neoplasm TNM property annotations, mostly by uri.
 * <p>
 * should be used to:
 * <ul>
 * test that an annotation is of the desired property {@link #isCorrectProperty(IdentifiedAnnotation)}
 * get the property type uri from text {@link #getTypeUri(String)}
 * get the property value uri from text {@link #getValueUri(String)}
 * </ul>
 * <p>
 * In addition there are static methods to:
 * <ul>
 * get the parent uri of the tnm property types {@link #getParentUri()}
 * </ul>
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
final public class SizePropertyUtil extends AbstractPropertyUtil<QuantityUnit, QuantityValue> {

   static private final Logger LOGGER = Logger.getLogger( "SizePropertyUtil" );

   static private class SingletonHolder {
      static private SizePropertyUtil INSTANCE = new SizePropertyUtil();
   }

   static public SizePropertyUtil getInstance() {
      return SingletonHolder.INSTANCE;
   }

   private SizePropertyUtil() {
      super( "Size" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCorrectProperty( final IdentifiedAnnotation annotation ) {
      return isCorrectProperty( annotation, QuantityUnit.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QuantityValue getUriValue( final String uri ) {
      return QuantityValue.UNKNOWN;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QuantityValue getUnknownValue() {
      return QuantityValue.UNKNOWN;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTypeUri( final String typeText ) {
      return getTypeUri( typeText, QuantityUnit.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getValueUri( final String valueText ) {
      return "Property_or_Attribute";
   }

   /**
    * @return the uri acting as parent to all individual tnm property uris
    */
   static public String getParentUri() {
      return UriConstants.SIZE;
   }


}
