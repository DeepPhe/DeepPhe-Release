package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {10/15/2021}
 */
@PipeBitInfo(
      name = "Regex Topic Finder",
      description = "Annotates Document Topic topics by detecting Topic Headers using Regular Expressions provided in a Bar-Separated-Value (BSV) File.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class BsvRegexTopicFinder  extends RegexTopicFinder {

   static private final Logger LOGGER = Logger.getLogger( "BsvRegexTopicFinder" );


   static public final String TOPIC_TYPES_PATH = "TopicsBsv";
   static public final String TOPIC_TYPES_DESC
         = "path to a BSV file containing a list of regular expressions and corresponding topic types.";

   @ConfigurationParameter(
         name = TOPIC_TYPES_PATH,
         description = TOPIC_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/topic/DefaultTopicRegex.bsv"
   )
   private String _topicTypesPath;

   /**
    * {@inheritDoc}
    */
   @Override
   synchronized protected void loadTopics() throws ResourceInitializationException {
      try {
         final List<RegexUtil.RegexItemInfo> itemInfos
               = RegexUtil.parseBsvFile( _topicTypesPath,
                                         1,
                                         "Topic Name/Type "
                                         + "|| Full Regex; Index, Subject" );
         for ( RegexUtil.RegexItemInfo itemInfo : itemInfos ) {
            final List<Pattern> patternList = itemInfo.getPatternList();
            final TopicType topicType = new TopicType( itemInfo.getName(), patternList.get( 0 ) );
            addTopicType( topicType );
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "Finished Parsing" );
   }

   static public AnalysisEngineDescription createEngineDescription( final String topicTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( BsvRegexTopicFinder.class,
                                                            TOPIC_TYPES_PATH, topicTypesPath );
   }


}
