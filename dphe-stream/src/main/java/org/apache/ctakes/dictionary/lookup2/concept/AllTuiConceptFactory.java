package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traditional ctakes semantic groups ignore a large number of semantic types.
 * This class adds extra semantic groups to ctakes concepts.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2018
 */
public class AllTuiConceptFactory implements ConceptFactory {

   static private final Logger LOGGER = Logger.getLogger( "AllTuiConceptFactory" );


   final private ConceptFactory _delegateConceptFactory;

   public AllTuiConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      _delegateConceptFactory = new JdbcConceptFactory( name, uimaContext, properties );
      // Seed the CuiCodeUtil with a CL nci cui.  After this nci cuis in the dictionary should be assigned CL.
      CuiCodeUtil.getInstance().getCuiCode( "CL000001" );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateConceptFactory.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      final Concept ctakesConcept = _delegateConceptFactory.createConcept( cuiCode );
      return new AllTuiConcept( ctakesConcept );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      return cuiCodes.stream()
            .collect( Collectors.toMap( Function.identity(), this::createConcept ) );
   }


}
