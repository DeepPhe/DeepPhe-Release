package org.apache.ctakes.cancer.owl;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/6/2017
 */
final public class OwlRelationUtil {

   static private final Logger LOGGER = Logger.getLogger( "OwlRelationUtil" );

   private OwlRelationUtil() {
   }


   static public Collection<String> getRelatableUris( final Collection<String> availableUris,
                                                      final Collection<String> relatedUris ) {
      final Collection<String> relatableUris = new ArrayList<>();
      for ( String uri : availableUris ) {
         final Collection<String> pathToRoot = UriAnnotationCache.getInstance().getPathToRoot( uri );
         if ( pathToRoot.isEmpty() ) {
            continue;
         }
         if ( pathToRoot.stream().anyMatch( relatedUris::contains ) ) {
            relatableUris.add( uri );
         }
      }
      return relatableUris;
   }

   static public Map<String, Set<String>> getAllUriRelations( final String uri ) {
      final HashSetMap<String, String> allRelations = new HashSetMap<>();
      allRelations.putAll( getUriRelations( uri ) );
      allRelations.putAll( getUriRelations( uri + "Phenotype" ) );
      return allRelations;
   }

   static private Map<String, Set<String>> getUriRelations( final String uri ) {
      final IClass iClass = OwlOntologyConceptUtil.getIClass( uri );
      return getClassRelations( iClass );
   }

   static private Map<String, Set<String>> getClassRelations( final IClass iClass ) {
      if ( iClass == null ) {
         return Collections.emptyMap();
      }
      final Map<String, Set<String>> relationMap = new HashMap<>();
      final ILogicExpression necessary = iClass.getNecessaryRestrictions();
      relationMap.putAll( getRelations( necessary ) );
      final ILogicExpression equivalent = iClass.getEquivalentRestrictions();
      relationMap.putAll( getRelations( equivalent ) );
      return relationMap;
   }


   static private Map<String, Set<String>> getRelations( final ILogicExpression expression ) {
      if ( expression == null ) {
         return Collections.emptyMap();
      }
      final HashSetMap<String, String> classRelations = new HashSetMap<>();
      for ( Object member : expression ) {
         if ( member instanceof IRestriction && isSummarizable( (IRestriction) member ) ) {
            final IRestriction restriction = (IRestriction) member;
            classRelations.addAllValues( restriction.getProperty().getName(), getParameterUris( restriction ) );
         }
      }
      return classRelations;
   }


   static private boolean isSummarizable( final IRestriction restriction ) {
      if ( restriction.getProperty().isObjectProperty() ) {
         final ILogicExpression expression = restriction.getParameter();
         return isSummarizable( expression );
      }
      return false;
   }

   static private boolean isSummarizable( final ILogicExpression expression ) {
      if ( expression == null ) {
         return false;
      }
      for ( Object object : expression ) {
         if ( object instanceof IClass ) {
            final String uri = OwlParserUtil.getUriString( (IClass) object );
            final boolean summarizable = OwlOntologyConceptUtil.getUriRootsStream( uri )
                  .anyMatch( u -> u.equals( OwlConstants.BODY_SITE_URI ) || u.equals( OwlConstants.EVENT_URI ) );
            if ( summarizable ) {
               return true;
            }
         }
      }
      return false;
   }

   static private Collection<String> getParameterUris( final IRestriction restriction ) {
      final ILogicExpression expression = restriction.getParameter();
      if ( expression == null ) {
         return Collections.emptySet();
      }
      final Collection<String> uris = new HashSet<>();
      for ( Object member : expression ) {
         if ( member instanceof IClass ) {
            uris.add( OwlParserUtil.getUriString( (IClass) member ) );
         }
      }
      return uris;
   }

}
