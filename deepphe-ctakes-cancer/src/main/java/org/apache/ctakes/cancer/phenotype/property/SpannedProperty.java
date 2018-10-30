package org.apache.ctakes.cancer.phenotype.property;

import org.apache.ctakes.cancer.util.SpannedEntity;

/**
 * Holds a spanned Type and spanned Value
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public interface SpannedProperty<T extends Type, V extends Value> extends SpannedEntity {

   /**
    * @return spanned entity for the type
    */
   SpannedType<T> getSpannedType();

   /**
    * @return spanned entity for the value
    */
   SpannedValue<V> getSpannedValue();

}
