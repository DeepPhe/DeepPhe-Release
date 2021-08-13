package org.healthnlp.deepphe.neo4j.util;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/6/2018
 */
final public class DataUtil {

    static private final Logger LOGGER = Logger.getLogger("DataUtil");


    static public Collection<Node> getAllPatientNodes( final GraphDatabaseService graphDb ) {
        // Can now use Neo4jConstants . PATIENT_LABEL
        try ( Transaction tx = graphDb.beginTx() ) {
            // Do we want to sort in any fashion?  Name, Age, Stage(s)?
            final Collection<Node> patients = graphDb.findNodes( PATIENT_LABEL )
                                                     .stream()
                                                     .collect( Collectors.toList() );
            tx.success();
            return patients;
        } catch (MultipleFoundException mfE) {
//         throw new RuntimeException( mfE );
        }
        return Collections.emptyList();
    }


    static public Node getInstanceClass( final GraphDatabaseService graphDb, final Node instance ) {
        if ( instance == null ) {
            return null;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            final Relationship instanceOf = instance.getSingleRelationship( INSTANCE_OF_RELATION, Direction.OUTGOING );
            if ( instanceOf == null ) {
                tx.success();
                return null;
            }
            final Node classNode = instanceOf.getOtherNode( instance );
            tx.success();
            return classNode;
        } catch (MultipleFoundException mfE) {
            LOGGER.error(mfE.getMessage(), mfE);
        }
        return null;
    }

    static public String getNodeName(final GraphDatabaseService graphDb, final Node node) {
        try (Transaction tx = graphDb.beginTx()) {
            final Object property = node.getProperty(NAME_KEY);
            tx.success();
            final String name = objectToString(property);
            if (!name.isEmpty()) {
                return name;
            }
        } catch (MultipleFoundException mfE) {
//         throw new RuntimeException( mfE );
        }
        return MISSING_NODE_NAME;
    }

    static public String getPreferredText( final GraphDatabaseService graphDb, final Node node ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node typeClass = getInstanceClass( graphDb, node );
            if ( typeClass == null ) {
                tx.success();
                return "";
            }
            final String prefText = objectToString( typeClass.getProperty( PREF_TEXT_KEY ) );
            tx.success();
            return prefText;
        } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
        }
        return "";
    }

    public static String safeGetProperty(final Node node, final String propertyName, final String defaultValue) {
        try {
            if (node.hasProperty(propertyName)) {
                Object value = node.getProperty(propertyName);
                String stringValue = value.toString();
                return stringValue;
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            // log.error(e.getMessage());
            throw e;
        }


    }

    static public String getUri( final GraphDatabaseService graphDb, final Node node ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node typeClass = getInstanceClass( graphDb, node );
            if ( typeClass == null ) {
                tx.success();
                return "Unknown";
            }
            final String uri = getNodeName( graphDb, typeClass );
            tx.success();
            return uri;
        } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
        }
        return "";
    }

    static public String objectToString( final Object stringObject ) {
        if ( stringObject == null ) {
            return "";
        }
        return stringObject.toString();
    }

    static public int objectToInt( final Object intObject ) {
        if ( intObject == null ) {
            return Integer.MIN_VALUE;
        }
        if ( intObject instanceof Integer ) {
            return (Integer)intObject;
        }
        try {
            return Integer.parseInt( intObject.toString() );
        } catch ( NumberFormatException nfE ) {
            return Integer.MIN_VALUE;
        }
    }

    static public boolean objectToBoolean( final Object booleanObject ) {
        if ( booleanObject == null ) {
            return false;
        }
        if ( booleanObject instanceof Boolean ) {
            return (Boolean)booleanObject;
        }
        return Boolean.parseBoolean( booleanObject.toString() );
    }


    static public String adjustPropertyName( final String propertyName ) {
        final char[] original = propertyName.toCharArray();
        final char[] adjusted = new char[ propertyName.length() ];
        adjusted[ 0 ] = Character.toLowerCase( original[ 0 ] );
        boolean wasScore = false;
        int adjustedLength = 1;
        for ( int i = 1; i < original.length; i++ ) {
            if ( wasScore ) {
                adjusted[ adjustedLength ] = Character.toUpperCase( original[ i ] );
                wasScore = false;
            } else if ( original[ i ] == '_' || original[ i ] == ' ' ) {
            wasScore = true;
            adjustedLength--;
         } else {
            adjusted[ adjustedLength ] = original[ i ];
         }
         adjustedLength++;
      }
      return new String( Arrays.copyOf( adjusted, adjustedLength ) );
   }

    static public String adjustRelationName( final String relationName ) {
        String hasOnly = relationName.replace( "Disease_", "" );
        hasOnly = hasOnly.replace( "Regimen_", "" );
        final char[] original = hasOnly.toCharArray();
        final char[] adjusted = new char[ hasOnly.length() * 2 ];
        adjusted[ 0 ] = Character.toUpperCase( original[ 0 ] );
        int adjustedLength = 0;
        boolean wasScore = true;
        boolean wasLowerCase = false;
        for ( int i = 0; i < original.length; i++ ) {
            if ( Character.isUpperCase( original[ i ] ) && !wasScore && wasLowerCase ) {
                adjusted[ adjustedLength ] = '_';
                adjustedLength++;
            }
            wasScore = original[ i ] == '_';
            wasLowerCase = Character.isLetter( original[ i ] ) && Character.isLowerCase( original[ i ] );
            adjusted[ adjustedLength ] = Character.toUpperCase( original[ i ] );
            adjustedLength++;
        }
        return new String( Arrays.copyOf( adjusted, adjustedLength ) );
    }

    static public String getRelationPrettyName( final String relationName ) {
        if (relationName != null) {
        String hasOnly = relationName.replace( "Disease_", "" );
        hasOnly = hasOnly.replace( "Regimen_", "" );
        if ( hasOnly.toLowerCase().startsWith( "has_" ) ) {
            hasOnly = hasOnly.substring( 4 );
        } else if ( hasOnly.startsWith( "has" ) ) {
            hasOnly = hasOnly.substring( 3 );
        } else if ( hasOnly.startsWith( "May_Have_" ) ) {
            hasOnly = hasOnly.substring( 9 );
        }
        final char[] original = hasOnly.toCharArray();
        final char[] adjusted = new char[ hasOnly.length()*2 ];
        adjusted[0] = Character.toUpperCase( original[0] );
        int adjustedLength = 0;
        boolean wasScore = true;
        boolean wasLowerCase = false;
        for ( int i=0; i<original.length; i++ ) {
            if ( Character.isUpperCase( original[ i ] ) && !wasScore && wasLowerCase ) {
                adjusted[ adjustedLength ] = ' ';
                adjustedLength++;
            }
            wasScore = original[ i ] == '_';
            wasLowerCase = Character.isLetter( original[ i ] ) && Character.isLowerCase( original[ i ] );
            adjusted[ adjustedLength ] = wasScore ? ' ' : original[ i ];
            adjustedLength++;
        }
        return new String( Arrays.copyOf( adjusted, adjustedLength ) );
        }
        return null;
    }


    // Convert note date / time string in format yyyyMMddhhmm to yyyy/mm/dd
    // viz wants only the date part
    static public String getReportDate( final String compactDate ) {
        if (compactDate.startsWith(NOTE_DATE)) {
            //JDL: hacky?  if the date, very specifically, begins with "Note_Date" then lets assume it's a more
            //specific error (e.g. "Note_Date_property_not_found") and move on.
            return compactDate;
        }
        if ( compactDate.length() != 12 ) {
            return "1999/01/01";
        }
        final char[] chars = compactDate.toCharArray();
        final StringBuilder sb = new StringBuilder();
        sb.append( chars[ 0 ] ).append( chars[ 1 ] ).append( chars[ 2 ] ).append( chars[ 3 ] )
          .append( '/' )
          .append( chars[ 4 ] ).append( chars[ 5 ] )
          .append( '/' )
          .append( chars[ 6 ] ).append( chars[ 7 ] );
        
        return sb.toString();
    }

}
