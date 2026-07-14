package org.healthnlp.deepphe.neo4j.util;


import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.MultipleFoundException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/31/2020
 */
final public class Neo4jAttributeUtil {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jAttributeUtil" );


   /**
    * @param uri -
    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
    */
   static public Collection<String> getBestAttribute( final GraphDatabaseService graphDb,
                                                      final String uri, final String attributeName ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            tx.success();
            return Collections.emptyList();
         }
         final Collection<String> attributes = new HashSet<>();
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getOrderedRootsTraverser( graphDb );
         for ( Node node : traverser.traverse( rootBase )
                                    .nodes() ) {
            final Object property = node.getProperty( attributeName, null );
            if ( property == null ) {
               continue;
            }
            if ( property instanceof String ) {
               attributes.add( (String)property );
            } else if ( property instanceof Collection ) {
               for ( Object object : (Collection)property ) {
                  if ( object instanceof String ) {
                     attributes.add( (String)object );
                  } else {
                     attributes.add( object.toString() );
                  }
               }
            }
            break;
         }
         tx.success();
         return attributes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }


}
