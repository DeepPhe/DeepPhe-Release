package org.apache.ctakes.core.util;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Like an AtomicLong using a BigInteger to go beyond the signed 64bit long max.
 */
public final class IdCounter {
   private final AtomicReference<BigInteger> _reference = new AtomicReference<>();

   public BigInteger incrementAndGet() {
      for ( ; ; ) {
         final BigInteger current = _reference.get();
         final BigInteger next = current.add( BigInteger.ONE );
         if ( _reference.compareAndSet( current, next ) ) {
            return next;
         }
      }
   }

   public void reset() {
      _reference.set( BigInteger.ZERO );
   }
}
