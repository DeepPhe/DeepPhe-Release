package org.healthnlp.deepphe.nlp.cr.naaccr;


import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
final public class NaaccrSection implements NaaccrItem {

   private String _id;
   private StringBuilder _textBuilder;

   public NaaccrSection( final String id ) {
      _textBuilder = new StringBuilder();
      setId( id );
   }

   public void setId( final String id ) {
      _id = id;
   }

   public String getId() {
      return _id;
   }

   public NaaccrSectionType getType() {
      return NaaccrSectionType.getSectionType( _id );
   }

   public void appendText( final String text ) {
      _textBuilder.append( text );
   }

   public String getText() {
      return _textBuilder.toString();
   }

   public JCasBuilder addToBuilder( final JCasBuilder builder ) {
      // Kludgy.  At some point consider updating outputs to use DocId and Encounter Id as default filenames.
      // TODO add a method like getDocumentIdForFile to DocumentIDAnnotationUtil that uses encounter and doc id.
      // TODO getEncounterDocIdForFile or maybe just getIdForFile() ?
      // TODO make SPF (Simple Programming Facade) classes for newbies.  They can delegate or have their own methods.
      // TODO put in spf package in core?
      String docId = "";
      try {
         final JCas tempCas = builder.build();
         docId = DocIdUtil.getDocumentID( tempCas );
      } catch ( UIMAException umiaE ) {
         docId = "UnknownDoc";
      }
      builder.setDocId( docId + "_" + getType().name() );
      builder.setDocType( getType().getNoteType() );
      builder.setDocText( getText() );
      return builder;
   }

   public void populateJCas( final JCas jCas ) {
      final Segment section = new Segment( jCas, 0, _textBuilder.length() );
      section.setId( getType().name() );
      section.setPreferredText( getType().getSectionType().getName() );
      section.setTagText( _id );
      section.addToIndexes();
   }

   static public NaaccrSection NULL_SECTION = new NaaccrSection( "NULL_SECTION" );

}
