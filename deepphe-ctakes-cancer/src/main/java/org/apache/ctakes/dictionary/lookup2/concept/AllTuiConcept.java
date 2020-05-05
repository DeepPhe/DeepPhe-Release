package org.apache.ctakes.dictionary.lookup2.concept;


import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.constants.CONST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.DPHE_CODING_SCHEME;

/**
 * Traditional ctakes semantic groups ignore a large number of semantic types.
 * This class adds extra semantic groups to ctakes concepts.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2018
 */
final public class AllTuiConcept implements Concept {

   static private final Collection<Integer> NE_TYPE_ID_UNKNOWN_LIST
         = Collections.singletonList( CONST.NE_TYPE_ID_UNKNOWN );

   private final Concept _delegate;

   final private Collection<Integer> _allSemantics;

   public AllTuiConcept( final Concept delegate ) {
      _delegate = delegate;
      Collection<Integer>
         ctakesSemantics = getCodes( TUI ).stream()
                                          .map( SemanticTui::getTuiFromCode )
                                          .map( SemanticTui::getGroupCode )
                                          .filter( id -> id != CONST.NE_TYPE_ID_UNKNOWN )
                                          .collect( Collectors.toSet() );
      if ( !ctakesSemantics.isEmpty() ) {
         _allSemantics = ctakesSemantics;
      } else {
         _allSemantics = NE_TYPE_ID_UNKNOWN_LIST;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui() {
      return _delegate.getCui();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPreferredText() {
      return _delegate.getPreferredText();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodeNames() {
      return _delegate.getCodeNames();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodes( final String codeType ) {
      // TODO Work into ontology
      final Collection<String> codes = _delegate.getCodes( codeType );
      if ( !codeType.equals( DPHE_CODING_SCHEME ) || codes.stream().noneMatch( c -> c.startsWith( "Childhood_" )
                                                                                 || c.equals( "Sentinel_Lymph_Node" )
                                                                                    || c.equals( "Lung_Tissue" )
      ) ) {
         return codes;
      }
      final Collection<String> newCodes = new ArrayList<>( codes.size() );
      for ( String code : codes ) {
         if ( code.startsWith( "Childhood_" ) ) {
            newCodes.add( code.substring( 10 ) );
         } else if ( code.equals( "Sentinel_Lymph_Node" ) ) {
            newCodes.add( "Lymph_Node" );
         } else if ( code.equals( "Lung_Tissue" ) ) {
            newCodes.add( "Lung" );
         } else {
            newCodes.add( code );
         }
      }
      return newCodes;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Integer> getCtakesSemantics() {
      return _allSemantics;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _delegate.isEmpty();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return _delegate.equals( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _delegate.hashCode();
   }

}
