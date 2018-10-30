package org.apache.ctakes.cancer.phenotype.receptor;


import org.apache.ctakes.cancer.phenotype.property.AbstractPropertyUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

/**
 * Singleton class with Utilities to interact with neoplasm Receptor Status property annotations, mostly by uri.
 *
 * should be used to:
 * <ul>
 * test that an annotation is of the desired property {@link #isCorrectProperty(IdentifiedAnnotation)}
 * get the property type uri from text {@link #getTypeUri(String)}
 * get the property value uri from text {@link #getValueUri(String)}
 *</ul>
 *
 * In addition there are static methods to:
 * <ul>
 * </ul>
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/6/2015
 */
final public class StatusPropertyUtil extends AbstractPropertyUtil<StatusType, StatusValue> {

   static private final Logger LOGGER = Logger.getLogger( "StatusPropertyUtil" );

   static private class SingletonHolder {
      static private StatusPropertyUtil INSTANCE = new StatusPropertyUtil();
   }

   static public StatusPropertyUtil getInstance() {
      return SingletonHolder.INSTANCE;
   }


   private StatusPropertyUtil() {
      super( "Receptor Status" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCorrectProperty( final IdentifiedAnnotation annotation ) {
      return isCorrectProperty( annotation, StatusType.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected StatusValue getUriValue( final String uri ) {
      return StatusValue.getUriValue( uri );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTypeUri( final String typeText ) {
      return getTypeUri( typeText, StatusType.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getValueUri( final String valueText ) {
      return getValueUri( valueText, StatusValue.values() );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected StatusValue getUnknownValue() {
      return StatusValue.UNKNOWN;
   }


}
