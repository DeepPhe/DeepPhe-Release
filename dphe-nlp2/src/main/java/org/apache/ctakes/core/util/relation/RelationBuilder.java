package org.apache.ctakes.core.util.relation;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.function.Function;

/**
 * @author SPF , chip-nlp
 * @since {1/11/2022}
 */
final public class RelationBuilder<T extends BinaryTextRelation> {

   static private final Logger LOGGER = Logger.getLogger( "RelationBuilder" );

   private Function<JCas, T> _creator;
   private String _name;
   private boolean _notHas;
   private Annotation _annotation;
   private Annotation _relatedTo;
   private int _discoveredBy = 0;
   private double _confidence = Double.MIN_VALUE;

   public RelationBuilder<T> creator( final Function<JCas,T> creator ) {
      _creator = creator;
      return this;
   }

   private RelationBuilder<BinaryTextRelation> createDefaultBuilder() {
      return new RelationBuilder<>().creator( BinaryTextRelation::new )
                                    .name( _name )
                                    .annotation( _annotation )
                                    .hasRelated( _relatedTo )
                                    .discoveredBy( _discoveredBy )
                                    .confidence( _confidence );
   }

   public RelationBuilder<T> name( final String name ) {
      _name = name;
      return this;
   }

   public RelationBuilder<T> notHas() {
      _notHas = true;
      return this;
   }

   public RelationBuilder<T> annotation( final Annotation annotation ) {
      _annotation = annotation;
      return this;
   }

   public RelationBuilder<T> hasRelated( final Annotation relatedTo ) {
      _relatedTo = relatedTo;
      return this;
   }

   // TODO  Add other techniques to CONST by:   public static final int REL_DISCOVERY_TECH_GOLD_ANNOTATION = 1;
   public RelationBuilder<T> discoveredBy( final int discoveryTechnique ) {
      _discoveredBy = discoveryTechnique;
      return this;
   }

   public RelationBuilder<T> confidence( final double confidence ) {
      _confidence = confidence;
      return this;
   }


   private String getCategory( final BinaryTextRelation relation ) {
      if ( _name != null && !_name.isEmpty() ) {
         return _name;
      }
      final String name = relation.getClass()
                                  .getSimpleName()
                                  .replace( "TextRelation", "" );
      if ( _notHas || name.toLowerCase().startsWith( "has" ) ) {
         return _name;
      }
      return "has" + name;
   }

   static private RelationArgument createArgument( final JCas jCas, final Annotation annotation ) {
      final RelationArgument argument = new RelationArgument( jCas );
      argument.setArgument( annotation );
      return argument;
   }

   public T build( final JCas jCas ) throws ClassCastException, NullPointerException, IllegalArgumentException {
      if ( _creator == null ) {
         throw new ClassCastException( "No creator function set for relation named " + _name );
//         LOGGER.warn( "No creator function set for relation named " + _name + ".  Creating BinaryTextRelation." );
//         return (T) createDefaultBuilder().build( jCas );
      }
      if ( _annotation == null ) {
         throw new NullPointerException( "No subject (hasRelated) annotation set for relation named " + _name );
      }
      if ( _relatedTo == null ) {
         throw new NullPointerException( "No (hasRelated) object annotation set for relation named " + _name );
      }
      if ( _annotation.equals( _relatedTo ) ) {
         throw new IllegalArgumentException( "Cannot relate annotation [" + _annotation.getCoveredText()
                                             + "] to itself for relation " + _name );
      }

      final T relation = _creator.apply( jCas );
      relation.setCategory( getCategory( relation ) );
      relation.setArg1( createArgument( jCas, _annotation ) );
      relation.setArg2( createArgument( jCas, _relatedTo ) );
      relation.setDiscoveryTechnique( _discoveredBy );
      relation.setConfidence( _confidence );
      relation.addToIndexes();
      return relation;
   }


}
