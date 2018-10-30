package org.apache.ctakes.cancer.cc;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil.RelationDirection;

/**
 * Instead of attempting to use hard coded columns, make dynamic output using the available relation types.
 * When uris and / or relation names are changed there is no propagation / refactoring / problem.
 * Choice of which columns to use is up to the eval tool.
 * This also allows a run once, analyze as many times / ways as wanted process.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/4/2018
 */
public class CiBsvFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "CiBsvFileWriter" );

   /**
    *
    * @param outputUri -
    * @param conceptInstances all concept instances
    * @param file -
    * @throws IOException -
    */
   public void writeFile( final String outputUri,
                          final Collection<ConceptInstance> conceptInstances,
                          final File file ) throws IOException {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Collection<String> branchUris = SearchUtil.getBranchUris( graphDb, outputUri );
      final Collection<ConceptInstance> ciBranch = conceptInstances.stream()
                                                                   .filter( ci -> branchUris.contains( ci.getUri() ) )
                                                                   .collect( Collectors.toList() );

      writeForwardBranch( ciBranch, file );
   }

   public void writeForwardBranch( final Collection<ConceptInstance> ciBranch,
                                   final File file ) throws IOException {
      LOGGER.info( "Writing Concept Instances to " + file.getAbsolutePath() + " ..." );
      final List<String> relationNames = getRelationNames( ciBranch, RelationDirection.FORWARD );

      final String header = createBsvHeader( relationNames );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( header + "\n" );
         for ( ConceptInstance conceptInstance : ciBranch ) {
            writer.write( createBsvRow( conceptInstance, relationNames, RelationDirection.FORWARD ) + "\n" );
         }
      }
   }

   static private void writeFullBranch( final Collection<ConceptInstance> ciBranch,
                                           final File file ) throws IOException {
      String allName = file.getName();
      String ext = "";
      final int lastDot = allName.indexOf( '.' );
      if ( lastDot > 0 ) {
         ext = allName.substring( lastDot );
         allName = allName.substring( 0, lastDot );
      }
      final File fullFile = new File( file.getParentFile(), allName + "_all" + ext );
      LOGGER.info( "Writing Concept Instances to " + fullFile.getAbsolutePath() + " ..." );
      final List<String> relationNames = getRelationNames( ciBranch, RelationDirection.FORWARD );
      final List<String> fullNames = getRelationNames( ciBranch, RelationDirection.ALL );
      fullNames.removeAll( relationNames );
      relationNames.addAll( fullNames );

      final String header = createBsvHeader( relationNames );
      try ( Writer writer = new BufferedWriter( new FileWriter( fullFile ) ) ) {
         writer.write( header + "\n" );
         for ( ConceptInstance conceptInstance : ciBranch ) {
            writer.write( createBsvRow( conceptInstance, relationNames, RelationDirection.ALL ) + "\n" );
         }
      }
   }


   static private List<String> getRelationNames( final Collection<ConceptInstance> conceptInstances,
                                                 final RelationDirection direction ) {
      final Map<Integer,Collection<String>> relationDepths = new HashMap<>();
      final Collection<ConceptInstance> soFar = new HashSet<>();
      for ( ConceptInstance conceptInstance : conceptInstances ) {
         soFar.add( conceptInstance );
         final Map<Integer,Collection<String>> childDepths = getRelationDepths( conceptInstance, soFar, 1 , direction );
         for ( Map.Entry<Integer,Collection<String>> childEntry : childDepths.entrySet() ) {
            relationDepths.computeIfAbsent( childEntry.getKey(), c -> new HashSet<>() ).addAll( childEntry.getValue() );
         }
      }
      final Collection<String> descentNames = new HashSet<>();
      for ( int i=relationDepths.size(); i>0; i-- ) {
         final Collection<String> depthNames = relationDepths.get( i );
         depthNames.removeAll( descentNames );
         descentNames.addAll( depthNames );
      }
      final List<String> relationNames = new ArrayList<>();
      for ( int i=1; i<= relationDepths.size(); i++ ) {
         final List<String> names = new ArrayList<>( relationDepths.get( i ) );
         names.sort( String.CASE_INSENSITIVE_ORDER );
         relationNames.addAll( names );
      }
      return relationNames;
   }

   /**
    *
    * @param conceptInstance parent
    * @param soFar concept instances already processed.  Prevents infinite loops.
    * @return relation names and concept instances
    */
   static private Map<String,Collection<ConceptInstance>> getDescendents( final ConceptInstance conceptInstance,
                                                                          final Collection<ConceptInstance> soFar,
                                                                          final RelationDirection direction ) {
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
      if ( direction != RelationDirection.REVERSE ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : conceptInstance.getRelated().entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
         getDescendents( allRelated, soFar, direction );
      }
      if ( direction != RelationDirection.FORWARD ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : conceptInstance.getReverseRelated().entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
         getDescendents( allRelated, soFar, direction );
      }
      return allRelated;
   }


   static private void getDescendents( final Map<String,Collection<ConceptInstance>> allRelated,
                                                                          final Collection<ConceptInstance> soFar,
                                                                          final RelationDirection direction ) {
      final Collection<ConceptInstance> children = allRelated.values().stream()
                                                             .flatMap( Collection::stream )
                                                             .distinct()
                                                             .collect( Collectors.toList() );
      children.removeAll( soFar );
      soFar.addAll( children );
      for ( ConceptInstance child : children ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : getDescendents( child, soFar, direction ).entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
      }
   }

   static private void getLayerDescendents( final Map<Integer,Map<String,Collection<ConceptInstance>>> layerMap,
                                            final int depth,
                                            final Map<String,Collection<ConceptInstance>> allRelated,
                                       final Collection<ConceptInstance> soFar,
                                       final RelationDirection direction ) {
      final Collection<ConceptInstance> children = allRelated.values().stream()
                                                             .flatMap( Collection::stream )
                                                             .distinct()
                                                             .collect( Collectors.toList() );
      children.removeAll( soFar );
      soFar.addAll( children );
      for ( ConceptInstance child : children ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : getDescendents( child, soFar, direction ).entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
      }
   }

   /**
       *
       * @param conceptInstance parent
       * @param soFar concept instances already processed.  Prevents infinite loops.
       * @return relation names and concept instances
       */
   static private Map<Integer,Collection<String>> getRelationDepths( final ConceptInstance conceptInstance,
                                                                    final Collection<ConceptInstance> soFar,
                                                                    final int depth,
                                                                     final RelationDirection direction ) {
      if ( conceptInstance == null ) {
         LOGGER.error( "Null Concept Instance" );
         return Collections.emptyMap();
      }
      final Map<Integer,Collection<String>> relationDepths = new HashMap<>();
      final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
      if ( direction != RelationDirection.REVERSE ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : conceptInstance.getRelated().entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
         getRelationDepths( allRelated, soFar, direction, relationDepths, depth );
      }
      if ( direction != RelationDirection.FORWARD ) {
         for ( Map.Entry<String,Collection<ConceptInstance>> entry : conceptInstance.getReverseRelated().entrySet() ) {
            allRelated.putIfAbsent( entry.getKey(), entry.getValue() );
         }
         getRelationDepths( allRelated, soFar, direction, relationDepths, depth );
      }
      relationDepths.computeIfAbsent( depth, c -> new HashSet<>() ).addAll( allRelated.keySet() );
      return relationDepths;
   }

   static private void getRelationDepths( final Map<String,Collection<ConceptInstance>> allRelated,
                                       final Collection<ConceptInstance> soFar,
                                       final RelationDirection direction,
                                          final Map<Integer,Collection<String>> relationDepths,
                                          final int depth ) {
      final Collection<ConceptInstance> children = allRelated.values().stream()
                                                             .flatMap( Collection::stream )
                                                             .distinct()
                                                             .collect( Collectors.toList() );
      children.removeAll( soFar );
      soFar.addAll( children );
      for ( ConceptInstance child : children ) {
         final Map<Integer,Collection<String>> childDepths = getRelationDepths( child, soFar, depth+1, direction );
         for ( Map.Entry<Integer,Collection<String>> childEntry : childDepths.entrySet() ) {
            relationDepths.computeIfAbsent( childEntry.getKey(), c -> new HashSet<>() ).addAll( childEntry.getValue() );
         }
      }
   }


   /**
    *
    * @param wantedRelations all relation types, sorted by name
    * @return a bsv row with column names
    */
   static private String createBsvHeader( final List<String> wantedRelations ) {
      final StringBuilder sb = new StringBuilder( "PatientId" );
      sb.append( '|' ).append( "DocumentId" )
        .append( '|' ).append( "URI" )
        .append( '|' ).append( "Negated" )
        .append( '|' ).append( "Uncertain" )
        .append( '|' ).append( "Conditional" )
        .append( '|' ).append( "Generic" )
        .append( '|' ).append( "DocTimeRel" );
      List<String> sortedRelations = sortStrings(wantedRelations);
      final String relations = sortedRelations.stream().collect( Collectors.joining( "|" ) );
      if ( !relations.isEmpty() ) {
         sb.append( '|' ).append( relations );
      }
      sb.append( '|' ).append( "CoveredText" );
      return sb.toString();

   }

   static private List<String> sortStrings(List<String> list) {
      List<String> strings = new ArrayList<>(list);
      Collections.sort(strings);
      return strings;

   }

   /**
    *
    * @param conceptInstance -
    * @param wantedRelations all relation types, sorted by name
    * @return a bsv row with information about the concept instance
    */
   static private String createBsvRow( final ConceptInstance conceptInstance,
                                       final List<String> wantedRelations,
                                       final RelationDirection direction ) {
      final StringBuilder sb = new StringBuilder( conceptInstance.getPatientId() );
      sb.append( '|' ).append( conceptInstance.getJoinedDocumentId() )
        .append( '|' ).append( UriUtil.getExtension( conceptInstance.getUri() ) )
        .append( '|' ).append( Boolean.toString( conceptInstance.isNegated() ) )
        .append( '|' ).append( Boolean.toString( conceptInstance.isUncertain() ) )
        .append( '|' ).append( Boolean.toString( conceptInstance.isConditional() ) )
        .append( '|' ).append( Boolean.toString( conceptInstance.isGeneric() ) )
        .append( '|' ).append( conceptInstance.getRelated().size() )
        .append( '|' ).append( conceptInstance.getReverseRelated().size() )
        .append( '|' ).append( conceptInstance.getDocTimeRel() );

      final Map<String, Collection<ConceptInstance>> related = ConceptInstanceUtil
            .getRelations( conceptInstance, direction );
      List<String> sortedRelations = sortStrings(wantedRelations);
      for ( String relationName : sortedRelations ) {
         sb.append( '|' );
         final Collection<ConceptInstance> instances = related.get( relationName );
         if ( instances != null ) {
            // If there are multiple CIs sharing the same type of relation, separate with semicolon
            final String uris = instances.stream()
                                         .filter( Objects::nonNull )
                                         .map( ConceptInstance::getUri )
                                         .distinct()
                                         .map( UriUtil::getExtension )
                                         .sorted()
                                         .collect( Collectors.joining( ";" ) );
            sb.append( uris );
         }
      }
      sb.append( '|' ).append( conceptInstance.getCoveredText() );
      return sb.toString();
   }


}
