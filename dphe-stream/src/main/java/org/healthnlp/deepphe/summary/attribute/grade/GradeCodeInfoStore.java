package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Map;


final public class GradeCodeInfoStore implements CodeInfoStore {

   public String _bestCode;


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      _bestCode = getBestGradeCode( uriInfoStore._bestUri );
   }

   public String getBestCode() {
      return _bestCode;
   }

   static private String getBestGradeCode( final String bestUri ) {
      final int gradeNumber = getUriGradeNumber( bestUri );
      if ( gradeNumber < 0 ) {
         return "9";
      }
      return "" + gradeNumber;
   }


   static public int getGradeNumber( final ConceptAggregate grade ) {
      return grade.getAllUris()
                  .stream()
                  .mapToInt( GradeCodeInfoStore::getUriGradeNumber )
                  .max()
                  .orElse( -1 );
   }

   static public int getUriGradeNumber( final String uri ) {
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         if ( uri.endsWith( "6" ) ) {
			 // well differentiated
            return 1;
         } else if ( uri.endsWith( "7" ) ) {
			 // moderately differentiated
            return 2;
         } else if ( uri.endsWith( "8" )
                     || uri.endsWith( "9" )
                     || uri.endsWith( "10" ) ) {
						 // poorly differentiated
            return 3;
         } else {
            return -1;
         }
      } else if ( uri.equals( "Grade_1" )
                  || uri.equals( "Low_Grade" )
                  || uri.equals( "Low_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Well_Differentiated" ) ) {
         return 1;
      } else if ( uri.equals( "Grade_2" )
                  || uri.equals( "Intermediate_Grade" )
                  || uri.equals( "Intermediate_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Moderately_Differentiated" ) ) {
         return 2;
      } else if ( uri.equals( "Grade_3" )
                  || uri.equals( "High_Grade" )
                  || uri.equals( "High_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Poorly_Differentiated" ) ) {
         return 3;
      } else if ( uri.equals( "Grade_4" )
                  || uri.equals( "Undifferentiated" )
                  || uri.equals( "Anaplastic" )) {
         return 4;
//      } else if ( uri.equals( "Grade_5" ) ) {
//         return 5;
      }
      return -1;
   }

}
