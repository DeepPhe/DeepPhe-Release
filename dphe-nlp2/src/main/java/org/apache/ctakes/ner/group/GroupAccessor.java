package org.apache.ctakes.ner.group;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public interface GroupAccessor<G extends Group<G>>
      extends NameOwnerAccessor<G> {

   List<G> getGroups();

   @Override
   default G getByName( String name ) {
      return getGroups().stream()
                        .filter( g -> g.getName().equals( name ) )
                        .findFirst()
                        .orElse( getNullGroup() );
   }

   /**
    *
    * @return group to be used when a return value is null.  The default is null.
    */
   default G getNullGroup() {
      return null;
   }

}
