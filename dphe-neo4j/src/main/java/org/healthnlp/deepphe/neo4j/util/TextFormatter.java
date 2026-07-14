package org.healthnlp.deepphe.neo4j.util;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

final public class TextFormatter {

   private TextFormatter() {
   }

   public static String getPatientEncounterAge( final String birthDate, final String encounterDate ) {
      final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy/MM/dd" );
      int age = 0;
      if ( ( birthDate != null ) && ( encounterDate != null ) ) {
         age = Period.between( LocalDate.parse( birthDate, formatter ),
                               LocalDate.parse( encounterDate, formatter ) )
                     .getYears();
      }
      // Convert the int to String value to avoid the {"low": n, "high": 0} issue probably due to
      // the javascript neo4j driver doesn't handle intergers in neo4j type system correctly - Joe
      return String.valueOf( age );
   }


   public static String toPrettyStage( final String name ) {
      if ( name.isEmpty() || name.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "Stage Not Found";
      }
      if ( name.length() == 7 ) {
         switch ( name ) {
            case "Stage_0":
               return "Stage 0";
            case "Stage_1":
               return "Stage I";
            case "Stage_2":
               return "Stage II";
            case "Stage_3":
               return "Stage III";
            case "Stage_4":
               return "Stage IV";
            case "Stage_5":
               return "Stage V";
         }
      }
      final String uri = name.substring( 0, 8 );
      switch ( uri ) {
         case "Stage_Un":
            return "Stage Not Found";
         case "Stage_0_":
            return "Stage 0";
         case "Stage_0i":
            return "Stage 0";
         case "Stage_0a":
            return "Stage 0";
         case "Stage_Is":
            return "Stage 0";
         case "Stage_1_":
            return "Stage I";
         case "Stage_1m":
            return "Stage I";
         case "Stage_1A":
            return "Stage IA";
         case "Stage_1B":
            return "Stage IB";
         case "Stage_1C":
            return "Stage IC";
         case "Stage_2_":
            return "Stage II";
         case "Stage_2A":
            return "Stage IIA";
         case "Stage_2B":
            return "Stage IIB";
         case "Stage_2C":
            return "Stage IIC";
         case "Stage_3_":
            return "Stage III";
         case "Stage_3A":
            return "Stage IIIA";
         case "Stage_3B":
            return "Stage IIIB";
         case "Stage_3C":
            return "Stage IIIC";
         case "Stage_4_":
            return "Stage IV";
         case "Stage_4A":
            return "Stage IVA";
         case "Stage_4B":
            return "Stage IVB";
         case "Stage_4C":
            return "Stage IVC";
         case "Stage_5_":
            return "Stage V";
      }
      return name;
   }


}
