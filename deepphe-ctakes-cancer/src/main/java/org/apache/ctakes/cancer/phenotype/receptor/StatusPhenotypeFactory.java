package org.apache.ctakes.cancer.phenotype.receptor;


import org.apache.ctakes.cancer.phenotype.AbstractPhenotypeFactory;
import org.apache.ctakes.cancer.phenotype.property.SpannedProperty;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_TYPE_ID_FINDING;

/**
 * Singleton that should be used to create full neoplasm receptor status property instances.
 * An instance is defined as the collection of all property types and values associated with a single neoplasm.
 *
 *
 * Use of any {@code createPhenotype()} method will create:
 * <ul>
 * Receptor Status type annotations
 * neoplasm relations between the Receptor Status type annotations and the nearest provided neoplasm in the text
 * Receptor Status value annotations
 * degree-of relations between the Receptor Status type annotations and the appropriate value annotations
 * test-for relations between Receptor Status type annotations and the nearest provided test in the text
 * </ul>
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/6/2015
 */
@Deprecated
final public class StatusPhenotypeFactory
      extends AbstractPhenotypeFactory<StatusType, StatusValue, SignSymptomMention> {

   static private final Logger LOGGER = Logger.getLogger( "StatusPhenotypeFactory" );

   static private class SingletonHolder {
      static private StatusPhenotypeFactory INSTANCE = new StatusPhenotypeFactory();
   }

   static public StatusPhenotypeFactory getInstance() {
      return SingletonHolder.INSTANCE;
   }

   private StatusPhenotypeFactory() {
      super( "Receptor Status" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public SignSymptomMention createPhenotype( final JCas jcas,
                                              final int windowStartOffset,
                                              final SpannedProperty<StatusType, StatusValue> status,
                                              final Iterable<IdentifiedAnnotation> neoplasms,
                                              final Iterable<IdentifiedAnnotation> diagnosticTests ) {
      final SignSymptomMention eventMention = createTypeEventMention( jcas, windowStartOffset, status );
      final Modifier valueModifier = createValueModifier( jcas, windowStartOffset, status );
      createEventMentionDegree( jcas, eventMention, valueModifier );
      return eventMention;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected SignSymptomMention createSpanEventMention( final JCas jcas, final int startOffset, final int endOffset ) {
      final SignSymptomMention disorder = new SignSymptomMention( jcas, startOffset, endOffset );
      disorder.setTypeID( NE_TYPE_ID_FINDING );
      return disorder;
   }


}
