package org.healthnlp.deepphe.fact;

import java.util.List;

/**
 * represents a list of facts with several niceties for convenience
 *
 * @author tseytlin
 */
public interface FactList extends List<Fact> {

   List<String> getTypes();

   void setTypes( List<String> type );

   void addType( String type );

   String getCategory();

   void setCategory( String category );

   boolean containsUri( String uri );

   boolean containsAnyUri( List<Fact> facts );

   boolean missingUri( String uri );

   boolean missingAnyUri( List<Fact> facts );

   /**
    * Check to see if there is an intersection between the two uri branches covered by these lists.
    * If any fact in this list is related to any fact in the given list then there is intersection.
    * Relation is judged by one uri being equal to or child to another.
    *
    * @param facts -
    * @return true if any fact in the given list has the same heritage as any fact in this list.
    */
   boolean intersects( List<Fact> facts );

}
