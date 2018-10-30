//package org.apache.ctakes.cancer.phenotype.receptor;
//
//import org.apache.ctakes.cancer.owl.OwlConstants;
//import org.apache.ctakes.cancer.phenotype.property.Test;
//import org.apache.ctakes.cancer.uri.UriConstants;
//
//import java.util.regex.Matcher;
//
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 2/24/2016
// */
// TODO SPF  Look up the new test uris and Refactor to UriConstants
//enum StatusTest implements Test {
//   IHC( "Immunohistochemistry", OwlConstants.IMMUNO_TEST_URI ),
//   //   IHC_PR("Unknown Diagnostic Test", OwlOntologyConceptUtil.SCHEMA_OWL + "#" + "DiagnosticProcedure" ),
////   IHC_ER("Unknown Diagnostic Test", OwlOntologyConceptUtil.SCHEMA_OWL + "#" + "DiagnosticProcedure" ),
////   IHC_HER2("Unknown Diagnostic Test", OwlOntologyConceptUtil.SCHEMA_OWL + "#" + "DiagnosticProcedure" ),
//   FISH( "Fluorescence in situ Hybridization",
//         OwlConstants.FISH_TEST_URI ),  //her2
//   CISH( "Chromogenic in situ Hybridization",
//         OwlConstants.CISH_TEST_URI ),
//   DISH( "Dual int situ Hybridization", OwlConstants.DISH_TEST_URI ),  //her2
//   //   SERUM_HER2( "Unknown Diagnostic Test", OwlOntologyConceptUtil.SCHEMA_OWL + "#" + "DiagnosticProcedure" ),
//   UNSPECIFIED( "Unknown Diagnostic Test", OwlConstants.UNSPECIFIED_TEST_URI );
//
//
//   static private final String PARENT_URI = "#DiagnosticProcedure";
//
//   final private String _title;
//   final private String _uri;
//
//   StatusTest( final String title, final String uri ) {
//      _title = title;
//      _uri = uri;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public String getTitle() {
//      return _title;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public String getUri() {
//      return _uri;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Matcher getMatcher( final CharSequence lookupWindow ) {
//      return null;
//   }
//
//
//}
