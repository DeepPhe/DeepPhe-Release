package org.apache.ctakes.core.util.owner;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface CodeOwner {

   String UNKNOWN_CUI = "C0439673";
   String UNKNOWN_URI = "Unknown";

   String getCui();

   String getUri();

}

