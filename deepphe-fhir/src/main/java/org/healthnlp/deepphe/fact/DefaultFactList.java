package org.healthnlp.deepphe.fact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides convenient varargs constructor.  Originally created because FHIRObjectMocker is creating SingletonLists
 * while FactList extended ArrayList, which caused a classtype exception.
 * <p>
 * However, FHIRObjectMocker still needs to be fixed as the return lists do not contain Fact objects ...
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2016
 */
public class DefaultFactList extends ArrayList<Fact> implements FactList {

   static private final Logger LOGGER = Logger.getLogger( "DefaultFactList" );


   private List<String> types;
   private String category;

   /**
    * Facility constructor
    *
    * @param facts one or more facts
    */
   public DefaultFactList( final Fact... facts ) {
      super( Arrays.asList( facts ) );
   }

   /**
    * Facility constructor
    *
    * @param category the category for this FactList.  Should this be immutable??
    * @param facts    one or more facts
    */
   public DefaultFactList( final String category, final Fact... facts ) {
      super( Arrays.asList( facts ) );
      this.category = category;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<String> getTypes() {
      if ( types == null ) {
         types = new ArrayList<>();
      }
      return types;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setTypes( final List<String> type ) {
      this.types = type;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addType( final String type ) {
      getTypes().add( type );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCategory() {
      return category;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setCategory( final String category ) {
      this.category = category;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean add( final Fact fact ) {
      return canAdd( fact ) && super.add( fact );
   }

   public boolean canAdd( final Fact fact ) {
      if ( fact == null ) {
         return false;
      }
      final String info = fact.getInfo();
      return this.stream()
                 .map( Fact::getInfo )
                 .noneMatch( info::equals );
   }

   public static FactList emptyList() {
      return new DefaultFactList();
   }

   public boolean containsUri( final String uri ) {
      return stream().map( Fact::getUri )
                     .anyMatch( uri::equals );
   }

   public boolean containsAnyUri( final List<Fact> facts ) {
      return facts.stream()
                  .map( Fact::getUri )
                  .anyMatch( this::containsUri );
   }

   public boolean missingUri( final String uri ) {
      return stream().map( Fact::getUri )
                     .noneMatch( uri::equals );
   }

   public boolean missingAnyUri( final List<Fact> facts ) {
      return facts.stream()
                  .map( Fact::getUri )
                  .anyMatch( this::missingUri );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean intersects( final List<Fact> facts ) {
      for ( Fact thisFact : this ) {
         for ( Fact thatFact : facts ) {
            if ( thisFact.isSameClass( thatFact )
                 || thisFact.isParentClassOf( thatFact )
                 || thisFact.isChildClassOf( thatFact ) ) {
               return true;
            }
         }
      }
      return false;
   }

}
