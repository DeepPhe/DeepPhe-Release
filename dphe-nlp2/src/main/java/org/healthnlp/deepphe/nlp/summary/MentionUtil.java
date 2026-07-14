package org.healthnlp.deepphe.nlp.summary;

import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {12/28/2023}
 */
final public class MentionUtil {

    private MentionUtil() {}

    static public List<String> getSortedMentionIds(final Collection<Mention> mentions ) {
        return mentions.stream()
                .sorted( Comparator.comparingInt( Mention::getBegin )
                        .thenComparingInt( Mention::getEnd ) )
                .map( Mention::getId )
                .collect( Collectors.toList() );
    }

}
