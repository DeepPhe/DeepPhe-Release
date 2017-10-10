package org.healthnlp.deepphe.fhir.fact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import org.neo4j.ogm.annotation.GraphId;

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

   @GraphId
   Long objectId;

   public Long getObjectId() {
      return objectId;
   }

   public void setObjectId( Long id ) {
      this.objectId = id;
   }

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
   public void setTypes( List<String> type ) {
      this.types = type;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addType( String type ) {
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
   public void setCategory( String category ) {
      this.category = category;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean add( Fact e ) {
      if ( canAdd( e ) )
         return e != null && super.add( e );
      else
         return false;
   }

   public boolean canAdd( Fact e ) {
      if ( e == null )
         return false;

      String eInfo = e.getInfo();
      for ( Fact f : this ) {
         if ( eInfo.equals( f.getInfo() ) )
            return false;
      }
      return true;
   }

   public static FactList emptyList() {
      return new DefaultFactList();
   }

}
