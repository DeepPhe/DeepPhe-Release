package org.healthnlp.deepphe.graph;

import org.healthnlp.deepphe.graph.summary.MedicalRecord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static javax.swing.UIManager.get;

/**
 * Created by Girish Chavan on 4/1/2016.
 */
public class GraphObjectFactory {

   public static final String POJO_PACKAGE = "org.healthnlp.deepphe.graph";

   public MedicalRecord copy( org.healthnlp.deepphe.fhir.summary.MedicalRecord mr ) {
      MedicalRecord medR = new MedicalRecord();

      copy( mr, medR );

      return medR;
   }

   HashMap<Object, Object> cacheMap = new HashMap<Object, Object>();

   /**
    * Calls all getters on source and saves the results using setters on destination
    *
    * @param source
    * @param dest
    */
   public void copy( Object source, Object dest ) {


      Method[] methods = (dest.getClass().getMethods());

      for ( Method destMethod : methods ) {

         //Identify setter methods except the setId
         if ( destMethod.getName().startsWith( "set" ) && !destMethod.getName().equals( "setId" ) ) {
            if ( destMethod.getName().equals( "setCompositionSummaries" ) ) {
               System.out.println( "debugger trap" );
            }
            try {
               //Try to get source data object
               Method getter = source.getClass().getMethod( "get" + destMethod.getName().substring( 3 ) );
               Object srcObject = getter.invoke( source );

               //Try to get matching graph object.
               Object neo4JObject = getNeo4JObjectCopy( destMethod, srcObject );

               if ( neo4JObject != null ) {
                  destMethod.invoke( dest, neo4JObject );
               }
               //else we just save the source data object as is. This supports not having to have a duplicate class for
               //each src object type. Simple objects need not be duplicated this way, if OGM does not choke on it.
               else {
                  destMethod.invoke( dest, srcObject );
               }

            } catch ( NoSuchMethodException e ) {
               e.printStackTrace();
            } catch ( InvocationTargetException e ) {
               e.printStackTrace();
            } catch ( IllegalAccessException e ) {
               e.printStackTrace();
            } catch ( InstantiationException e ) {
               e.printStackTrace();
            } catch ( IllegalArgumentException e ) {
               e.printStackTrace();
            }

         }
      }

   }

   private Object getNeo4JObjectCopy( Method destMethod, Object srcObject ) throws IllegalAccessException, InstantiationException {

      //First attempt to get it from cache
      Object neo4JObject = cacheMap.get( srcObject );

      if ( neo4JObject == null ) {
         neo4JObject = instantiateNeo4JObjectIfAvailable( destMethod );

         //if we have matching graph object, then copy into it.
         if ( neo4JObject != null ) {

            cacheMap.put( srcObject, neo4JObject );

            //If the neo4jobj is a collection, then we need to individually copy contents
            if ( neo4JObject instanceof Collection ) {

               //the instantiateNeo4JObjectIfAvailable method returns a list with one empty
               // object of the type that is required. We extract that and clear the list.
               Class dstCls = ((List) neo4JObject).get( 0 ).getClass();
               ((List) neo4JObject).clear();

               for ( Object o : (List) srcObject ) {
                  Object dstObj = cacheMap.get( srcObject );
                  if ( dstObj == null ) {
                     dstObj = dstCls.newInstance();
                     cacheMap.put( srcObject, dstObj );
                     copy( o, dstObj );
                  }

                  ((List) neo4JObject).add( dstObj );
               }
            } else { //else just copy the object directly
               copy( srcObject, neo4JObject );
            }
         }
      }
      return neo4JObject;
   }

   /**
    * See if the method parameter is a type of Neo4J object.
    * If yes, return an empty object of that type.
    * If no, return null;
    *
    * @param method
    * @return
    */
   public static Object instantiateNeo4JObjectIfAvailable( Method method ) throws IllegalAccessException, InstantiationException {
      for ( Type type : method.getGenericParameterTypes() ) {

         try {
            if ( type instanceof ParameterizedType ) {
               ParameterizedType ptype = (ParameterizedType) type;
               if ( Collection.class.isAssignableFrom( Class.forName( ptype.getRawType().getTypeName() ) ) ) {
                  Type[] typeArguments = ptype.getActualTypeArguments();
                  for ( Type typeArgument : typeArguments ) {
                     String neo4jName = GraphObjectFactory.deriveGraphObjectClassName( typeArgument.getTypeName() );
                     Class cls = Class.forName( typeArgument.getTypeName() );
                     if ( cls.getPackage().getName().startsWith( POJO_PACKAGE ) ) {
                        List list = new ArrayList();
                        list.add( cls.newInstance() );
                        return list;
                     }
                  }
               }
            }
            String neo4jName = GraphObjectFactory.deriveGraphObjectClassName( type.getTypeName() );

            Class cls = Class.forName( neo4jName );
            if ( cls.getPackage().getName().startsWith( POJO_PACKAGE ) ) {
               return cls.newInstance();
            }
         } catch ( ClassNotFoundException e ) {
//                e.printStackTrace();
         }

      }
      return null;
   }

   private static String deriveGraphObjectClassName( String fhirClassName ) {
      return fhirClassName.replaceAll( "\\.fhir\\.", ".graph." );
   }

}
