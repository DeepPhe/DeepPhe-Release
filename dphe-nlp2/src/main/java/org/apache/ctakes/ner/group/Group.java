package org.apache.ctakes.ner.group;


/**
 * @author SPF , chip-nlp
 * @since {11/9/2023}
 */
public interface Group<G extends Group<G>>
      extends NameOwner {

   int compareGroup( final G otherGroup );

   int getOrdinal();

}
