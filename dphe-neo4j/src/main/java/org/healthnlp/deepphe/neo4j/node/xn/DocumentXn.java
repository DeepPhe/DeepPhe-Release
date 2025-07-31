package org.healthnlp.deepphe.neo4j.node.xn;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.neo4j.node.Section;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {10/20/2023}
 */
public class DocumentXn {
    private String id;
    private String name;
    private String type;
    private String date;
    private String episode;
    private String text;
    private List<Section> sections;
    private List<Mention> mentions;
    private List<MentionRelation> mentionRelations;

    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType( final String type ) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate( final String date ) {
        this.date = date;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode( final String episode ) {
        this.episode = episode;
    }

    public String getText() {
        return text;
    }

    public void setText( final String text ) {
        this.text = text;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections( final List<Section> sections ) {
        this.sections = sections;
    }

    public List<Mention> getMentions() {
        return mentions;
    }

    public void setMentions( final List<Mention> mentions ) {
        this.mentions = mentions;
    }

    public List<MentionRelation> getMentionRelations() {
        return mentionRelations;
    }

    public void setMentionRelations(final List<MentionRelation> mentionRelations) {
        this.mentionRelations = mentionRelations;
    }


}
