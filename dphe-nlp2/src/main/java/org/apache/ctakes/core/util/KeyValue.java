package org.apache.ctakes.core.util;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
final public class KeyValue<K,V> {

   private final K _key;
   private final V _value;

   public KeyValue(final K key, final V value ) {
      _key = key;
      _value = value;
   }

   public K getKey() {
      return _key;
   }

   public V getValue() {
      return _value;
   }


}
