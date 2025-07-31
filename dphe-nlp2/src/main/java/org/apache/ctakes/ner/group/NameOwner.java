package org.apache.ctakes.ner.group;

/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public interface NameOwner {

   String getName();

   int compareName( NameOwner otherOwner );
}
