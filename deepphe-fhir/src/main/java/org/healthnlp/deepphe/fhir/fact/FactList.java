package org.healthnlp.deepphe.fhir.fact;

import java.util.List;

/**
 * represents a list of facts with several niceties for convinience
 *
 * @author tseytlin
 */
public interface FactList extends List<Fact> {

   List<String> getTypes();

   void setTypes( List<String> type );

   void addType( String type );

   String getCategory();

   void setCategory( String category );

}
