package org.apache.ctakes.ner.group;


/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public interface NameOwnerAccessor<N extends NameOwner> {

   N getByName( final String name );

}
