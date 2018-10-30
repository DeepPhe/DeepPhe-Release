package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.phenotype.AbstractPhenotypeFactory;
import org.apache.ctakes.cancer.phenotype.property.SpannedProperty;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_TYPE_ID_PHENOMENA;

/**
 * Singleton that should be used to create full neoplasm size property instances.
 * An instance is defined as the collection of all property types and values associated with a single neoplasm.
 * <p>
 * <p>
 * Use of any {@code createPhenotype()} method will create:
 * <ul>
 * size type annotations
 * neoplasm relations between the size type annotations and the nearest provided neoplasm in the text
 * size value annotations
 * degree-of relations between the size type annotations and the appropriate value annotations
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
@Deprecated
final public class SizePhenotypeFactory
      extends AbstractPhenotypeFactory<QuantityUnit, QuantityValue, MeasurementAnnotation> {

   static private final Logger LOGGER = Logger.getLogger( "SizePhenotypeFactory" );

   static private class SingletonHolder {
      static private SizePhenotypeFactory INSTANCE = new SizePhenotypeFactory();
   }

   static public SizePhenotypeFactory getInstance() {
      return SingletonHolder.INSTANCE;
   }

   private SizePhenotypeFactory() {
      super( "Size" );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public MeasurementAnnotation createPhenotype( final JCas jcas,
                                                 final int windowStartOffset,
                                                 final SpannedProperty<QuantityUnit, QuantityValue> size,
                                                 final Iterable<IdentifiedAnnotation> neoplasms,
                                                 final Iterable<IdentifiedAnnotation> diagnosticTests ) {
      final MeasurementAnnotation fullMeasurement
            = createPropertyEventMention( jcas, UriConstants.SIZE, windowStartOffset, size );
      return fullMeasurement;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected MeasurementAnnotation createSpanEventMention( final JCas jcas, final int startOffset,
                                                           final int endOffset ) {
      final MeasurementAnnotation measurement = new MeasurementAnnotation( jcas, startOffset, endOffset );
      measurement.setTypeID( NE_TYPE_ID_PHENOMENA );
      return measurement;
   }

}
