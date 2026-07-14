package org.apache.ctakes.ner.group;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public interface AnnotationGroupAccessor<G extends Group<G>> extends GroupAccessor<G> {

   Collection<G> getAnnotationGroups( final IdentifiedAnnotation annotation );

}
