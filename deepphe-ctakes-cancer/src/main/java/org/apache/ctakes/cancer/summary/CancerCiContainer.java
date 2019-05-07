package org.apache.ctakes.cancer.summary;


import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/4/2019
 */
final public class CancerCiContainer extends AbstractCiContainer {

   static private AtomicLong _ID_NUM = new AtomicLong( 0 );

   private final NeoplasmCiContainer _cancer;
   private final Collection<NeoplasmCiContainer> _tumors;

   public CancerCiContainer( final NeoplasmCiContainer cancer, final Collection<NeoplasmCiContainer> tumors ) {
      super( cancer.getType(), cancer.getUri(), cancer.getWorldId() );
      _cancer = cancer;
      _tumors = tumors;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final long createUniqueIdNum() {
      return _ID_NUM.incrementAndGet();
   }

   public NeoplasmCiContainer getCancer() {
      return _cancer;
   }

   public Collection<NeoplasmCiContainer> getTumors() {
      return _tumors;
   }

   public String toString() {
      return "  =====  Cancer  =====\n" + _cancer.toString() + "    ===  Tumor  ===\n"
             + _tumors.stream()
                      .map( CiContainer::toString )
                      .collect( Collectors.joining( "    ===  Tumor  ===\n" ) );
   }

}
