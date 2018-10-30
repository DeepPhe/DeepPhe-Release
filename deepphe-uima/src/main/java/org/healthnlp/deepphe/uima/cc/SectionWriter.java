package org.healthnlp.deepphe.uima.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * @author JJM , chip-nlp
 * @version %I%
 * @since 7/20/2018
 */
public class SectionWriter extends AbstractJCasFileWriter {

   private static final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();

   static private final Logger LOGGER = Logger.getLogger( CLASS_NAME );


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      String sectionIDs = "";
      String sectionIDsAndTagText = "";
      String tagText = "";

      final Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );

      for (Segment section: sections) {
         String thisTagText;
         if (sections.size()==1 && section.getId().contains("SIMPLE_SEGMENT")) {
            sectionIDs = "(Only SIMPLE_SEGMENT found)";
            tagText = sectionIDs;
         }

         thisTagText = "";
         if (!section.getId().contains("SIMPLE_SEGMENT")) {
            sectionIDs = sectionIDs + section.getId().trim() + " " + section.getBegin() + " " + section.getEnd() + "\n";

            if (section.getTagText() != null) {
               thisTagText = section.getTagText().trim() + "";
            }
            tagText = tagText + thisTagText + " " + section.getBegin() + " " + section.getEnd() +  "\n";


         }

         sectionIDsAndTagText = sectionIDsAndTagText + section.getId().trim() + " " + section.getBegin() + " " + section.getEnd() +  "\n" +
                 thisTagText + " " + section.getBegin() + " " + section.getEnd() +  "\n" +
                                  "- - - - - - - - - - - - - - - - - - - - - - - - - - - - " + "\n";

      }

      writeTextFile(outputDir, documentId+".sections.txt", sectionIDs);
      writeTextFile(outputDir, documentId+".sections.and.tagtext.txt", sectionIDsAndTagText);
      writeTextFile(outputDir, documentId+".tagtext.txt", tagText);

   }

    private void writeTextFile(String outputDir, String filename, String text) throws IOException {

      final File f = new File(outputDir, filename);
      try ( final BufferedWriter writer = new BufferedWriter(new FileWriter(f)) ) {
         writer.write(text);
      }

    }

}
