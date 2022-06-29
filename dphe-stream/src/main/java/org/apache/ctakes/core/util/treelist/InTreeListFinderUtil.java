package org.apache.ctakes.core.util.treelist;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.log4j.Logger;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {12/3/2021}
 */
final public class InTreeListFinderUtil {

   // TODO - Type NormalizableIdentifiedAnnotation - add NormalizingAnnotation (get,set) that can hold an Annotation?
   // TODO - Same as above with IdentifiedAnnotation ?
   //  Would allow a = getNormalizingAnnotation() if a IdentifiedAnnotation || normalizableIdentifiedAnnotation
   //  a.getNormalization()  <-- util could throw into loop until there is only text or only an annotation.
   //  This could make things like Grade easier as they have a string but that string may come from an annotation.

   // TODO  Make reusable (util) methods for
   //  findListByHeaderAnnotation( FormattedLists, annotations )
   //  findListByHeaderCode( FormattedLists, schemeName (cui, url, etc.), codes )
   //  findListByHeaderRegex( FormattedLists, Regex/Pattern )
   //  findEntryByNameAnnotation( FormattedLists/lists/entries, annotations )  map<annotation,entry> ?
   //  findEntryByNameCode( FormattedLists/lists/entries, schemeName (cui, url, etc.), codes )  map<code,collection<entry>> ?
   //  findEntryByNameRegex( FormattedLists/lists/entries, Regex/Pattern )  map<span,collection<entry>> ?
   //  findEntryByValueAnnotation( FormattedLists/lists/entries, annotations),
   //  findEntryByValueCode( FormattedLists/lists/entries, schemeName (cui, url, etc.), codes )
   //  findEntryByValueRegex( FormattedLists/lists/entries, Regex/Pattern )
   //  findEntryByAnnotation( FormattedLists/lists/entries, annotations )
   //  findEntryByCode( FormattedLists/lists/entries, schemeName (cui, url, etc.), codes )
   //  findEntryByRegex( FormattedLists/lists/entries, Regex/Pattern )
   //  --- can use FormattedListEntry.getName() and .getValue, then use the returned IdentifiedAnnotation
   //  e.g.  findByName() { return findByNormalizable( entry.getName() ) }
   //        findByValue() { return findByNormalizable( entry.getValue() ) }
   //  normalize( NormalizableIdentifiedAnnotation, String )
   //  normalize( NormalizableIdentifiedAnnotation, Annotation )
   //  normalize( NormalizableIdentifiedAnnotation, schemeName, code )

   private InTreeListFinderUtil() {}

   static public <A extends Annotation> List<A> findInFormattedLists( final JCas jCas,
                                                               final InTreeListFinder<A> inFormattedListFinder ) {
      return findInFormattedLists( jCas, null, "", inFormattedListFinder );
   }

   static public <A extends Annotation> List<A> findInFormattedLists( final JCas jCas,
                                                                 final Logger logger,
                                                                 final String processName,
                                                                 final InTreeListFinder<A> inFormattedListFinder ) {

      final List<A> foundItems = new ArrayList<>();
      final Collection<FormattedList> FormattedLists = JCasUtil.select( jCas, FormattedList.class );
      if ( FormattedLists == null || FormattedLists.isEmpty() ) {
         return Collections.emptyList();
      }
      logger.info( processName + " in FormattedLists ..." );
      for ( FormattedList FormattedList : FormattedLists ) {
         inFormattedListFinder.addFound( jCas, FormattedList, foundItems );
      }
      return foundItems;
   }

   ///////////////////////////////////////////////////////////////////////
   //                         Fetchers
   ///////////////////////////////////////////////////////////////////////


   static public List<FormattedListEntry> getListEntries( final FormattedList formattedList ) {
      if ( formattedList == null ) {
         return Collections.emptyList();
      }
      final FSArray fsArray = formattedList.getListEntries();
      final List<FormattedListEntry> entries = new ArrayList<>( fsArray.size() );
      for ( int i=0; i< fsArray.size(); i++ ) {
         final FeatureStructure structure = fsArray.get( i );
         if ( structure instanceof FormattedListEntry ) {
            entries.add( (FormattedListEntry)structure );
         }
      }
      return entries;
   }

   static public List<IdentifiedAnnotation> getNames( final FormattedList formattedList ) {
      return getListEntries( formattedList ).stream()
                                            .map( FormattedListEntry::getName )
                                            .collect( Collectors.toList() );
   }

   static public List<IdentifiedAnnotation> getValues( final FormattedList formattedList ) {
      return getListEntries( formattedList ).stream()
                                            .map( FormattedListEntry::getValue )
                                            .collect( Collectors.toList() );
   }




   ///////////////////////////////////////////////////////////////////////
   //                         Lists
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,FormattedList> getAnnotationsInLists( final Collection<FormattedList> FormattedLists,
                                                                 final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      FormattedLists.forEach( t -> getAnnotationsInList( t, annotations )
            .forEach( a -> map.put( a, t ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInList( final FormattedList FormattedList,
                                                              final Collection<Annotation> annotations ) {
      final Collection<Annotation> present = getAnnotationsInHeading( FormattedList, annotations );
      present.addAll( getAnnotationsInEntries( getListEntries( FormattedList ), annotations ).keySet() );
      return getAnnotationsInNormalizable( FormattedList.getHeading(), annotations );
   }

   static public boolean isAnnotationInList( final FormattedList FormattedList, final Annotation annotation ) {
      return isAnnotationInHeading( FormattedList, annotation )
             || !getAnnotationsInEntries( getListEntries( FormattedList ),
                                          Collections.singletonList( annotation ) ).isEmpty();
   }


   static public Map<Annotation,FormattedList> getAnnotationsInLists( final JCas jCas,
                                                              final Collection<FormattedList> FormattedLists,
                                                              final Collection<String> codes ) {
      final Collection<Annotation> annotations
            = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
      return getAnnotationsInLists( FormattedLists, annotations );
   }


   ///////////////////////////////////////////////////////////////////////
   //                         Headings
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,FormattedList> getAnnotationsInHeadings( final Collection<FormattedList> FormattedLists,
                                                                    final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      FormattedLists.forEach( t -> getAnnotationsInHeading( t, annotations )
            .forEach( a -> map.put( a, t ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInHeading( final FormattedList FormattedList,
                                                                 final Collection<Annotation> annotations ) {
      return getAnnotationsInNormalizable( FormattedList.getHeading(), annotations );
   }

   static public boolean isAnnotationInHeading( final FormattedList FormattedList,
                                                final Annotation annotation ) {
      return isAnnotationInHeading( FormattedList.getHeading(), annotation );
   }

   static public boolean isAnnotationInHeading( final IdentifiedAnnotation heading,
                                                final Annotation annotation ) {
      return isAnnotationInNormalizable( heading, annotation );
   }

   static public Map<Annotation,FormattedList> getAnnotationsInHeadings( final JCas jCas,
                                                                    final Collection<FormattedList> FormattedLists,
                                                                    final Collection<String> codes ) {
      final Collection<Annotation> annotations
            = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
      return getAnnotationsInHeadings( FormattedLists, annotations );
   }

   ///////////////////////////////////////////////////////////////////////
   //                         Names
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,FormattedList> getAnnotationsInNames( final Collection<FormattedList> FormattedLists,
                                                                 final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      FormattedLists.forEach( t -> map.putAll( getAnnotationsInNames( t, annotations) ) );
      return map;
   }

   static public Map<Annotation,FormattedList> getAnnotationsInNames( final FormattedList FormattedList,
                                                                 final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      getAnnotationsInEntryNames( getListEntries( FormattedList ), annotations )
            .keySet()
            .forEach( a -> map.put( a, FormattedList ) );
      return map;
   }

   static public Map<Annotation,FormattedListEntry> getAnnotationsInEntryNames(
         final Collection<FormattedListEntry> entries,
         final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedListEntry> map = new HashMap<>();
      entries.forEach(  e -> getAnnotationsInName( e, annotations )
            .forEach( a -> map.put( a, e ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInName( final FormattedListEntry entry,
                                                              final Collection<Annotation> annotations ) {
      return getAnnotationsInNormalizable( entry.getName(), annotations );
   }

   static public boolean isAnnotationInName( final FormattedListEntry entry,
                                             final Annotation annotation ) {
      return isAnnotationInName( entry.getName(), annotation );
   }

   static public boolean isAnnotationInName( final IdentifiedAnnotation name,
                                             final Annotation annotation ) {
      return isAnnotationInNormalizable( name, annotation );
   }

   static public Map<Annotation,FormattedList> getAnnotationsInNames( final JCas jCas,
                                                                    final Collection<FormattedList> FormattedLists,
                                                                    final Collection<String> codes ) {
      final Collection<Annotation> annotations
            = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
      return getAnnotationsInNames( FormattedLists, annotations );
   }

   ///////////////////////////////////////////////////////////////////////
   //                         Values
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,FormattedList> getAnnotationsInValues( final Collection<FormattedList> FormattedLists,
                                                                 final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      FormattedLists.forEach( t -> map.putAll( getAnnotationsInValues( t, annotations) ) );
      return map;
   }

   static public Map<Annotation,FormattedList> getAnnotationsInValues( final FormattedList FormattedList,
                                                                 final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedList> map = new HashMap<>();
      getAnnotationsInEntryValues( getListEntries( FormattedList ), annotations )
            .keySet()
            .forEach( a -> map.put( a, FormattedList ) );
      return map;
   }

   static public Map<Annotation,FormattedListEntry> getAnnotationsInEntryValues(
         final Collection<FormattedListEntry> entries,
         final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedListEntry> map = new HashMap<>();
      entries.forEach(  e -> getAnnotationsInValue( e, annotations )
             .forEach( a -> map.put( a, e ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInValue( final FormattedListEntry entry,
                                                               final Collection<Annotation> annotations ) {
      return getAnnotationsInNormalizable( entry.getValue(), annotations );
   }

   static public boolean isAnnotationInValue( final FormattedListEntry entry,
                                              final Annotation annotation ) {
      return isAnnotationInValue( entry.getValue(), annotation );
   }

   static public boolean isAnnotationInValue( final IdentifiedAnnotation value,
                                              final Annotation annotation ) {
      return isAnnotationInNormalizable( value, annotation );
   }

   static public Map<Annotation,FormattedList> getAnnotationsInValues( final JCas jCas,
                                                                 final Collection<FormattedList> FormattedLists,
                                                                 final Collection<String> codes ) {
      final Collection<Annotation> annotations
            = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
      return getAnnotationsInValues( FormattedLists, annotations );
   }

   ///////////////////////////////////////////////////////////////////////
   //                         Entries
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,FormattedListEntry> getAnnotationsInEntries(
         final Collection<FormattedListEntry> entries,
         final Collection<Annotation> annotations ) {
      final Map<Annotation,FormattedListEntry> map = new HashMap<>();
      entries.forEach(  e -> getAnnotationsInEntry( e, annotations )
             .forEach( a -> map.put( a, e ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInEntry( final FormattedListEntry entry,
                                                               final Collection<Annotation> annotations ) {
      if ( entry == null ) {
         return Collections.emptyList();
      }
      return annotations.stream()
                        .filter( a -> isAnnotationInEntry( entry, a ) )
                        .collect( Collectors.toList() );
   }

   static public boolean isAnnotationInEntry( final FormattedListEntry entry,
                                              final Annotation annotation ) {
      return isAnnotationInName( entry.getName(), annotation )
             || isAnnotationInValue( entry.getValue(), annotation );
   }

   static public Map<Annotation,FormattedListEntry> getAnnotationsInEntries( final JCas jCas,
                                                                             final Collection<FormattedListEntry> entries,
                                                                             final Collection<String> codes ) {
      final Collection<Annotation> annotations
            = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
      return getAnnotationsInEntries( entries, annotations );
   }

   ///////////////////////////////////////////////////////////////////////
   //                         Normalizables
   ///////////////////////////////////////////////////////////////////////

   static public Map<Annotation,IdentifiedAnnotation> getAnnotationsInNormalizables(
         final Collection<IdentifiedAnnotation> normalizables,
         final Collection<Annotation> annotations ) {
      final Map<Annotation,IdentifiedAnnotation> map = new HashMap<>();
      normalizables.forEach( n -> getAnnotationsInNormalizable( n, annotations )
            .forEach( a -> map.put( a, n ) ) );
      return map;
   }

   static public Collection<Annotation> getAnnotationsInNormalizable(
         final IdentifiedAnnotation IdentifiedAnnotation,
         final Collection<Annotation> annotations ) {
      if ( IdentifiedAnnotation == null ) {
         return Collections.emptyList();
      }
      return annotations.stream()
                        .filter( a -> isAnnotationInNormalizable( IdentifiedAnnotation, a ) )
                        .collect( Collectors.toList() );
   }

   static public boolean isAnnotationInNormalizable(
         final IdentifiedAnnotation IdentifiedAnnotation,
         final Annotation annotation ) {
      return IdentifiedAnnotation != null
             && TextSpanUtil.isAnnotationCovered( IdentifiedAnnotation, annotation );
   }

   static public Collection<Annotation> getAnnotationsInNormalizable( final JCas jCas,
                                                                      final IdentifiedAnnotation IdentifiedAnnotation,
                                                                       final Collection<String> codes ) {
      return JCasUtil.selectCovered( jCas, IdentifiedAnnotation.class, IdentifiedAnnotation )
                      .stream()
                      .filter( a -> OntologyConceptUtil.getCodes( a )
                                                       .stream()
                                                       .anyMatch( codes::contains ) )
                      .collect( Collectors.toSet() );
   }

   static public  Collection<Pair<Integer>> getSpansInNormalizable( final IdentifiedAnnotation IdentifiedAnnotation,
                                                                      final String regex ) {
      return getSpansInNormalizable( IdentifiedAnnotation, Pattern.compile( regex ) );
   }

   static public Collection<Pair<Integer>> getSpansInNormalizable( final IdentifiedAnnotation IdentifiedAnnotation,
                                                                         final Pattern pattern ) {
      final int offset = IdentifiedAnnotation.getBegin();
      final RegexSpanFinder finder = new RegexSpanFinder( pattern );
      return finder.findSpans( IdentifiedAnnotation.getCoveredText() )
                   .stream()
                   .filter( RegexUtil::isValidSpan )
                   .map( s -> new Pair<>( offset + s.getValue1(), offset + s.getValue2() ) )
                   .collect( Collectors.toList() );
   }

   static public Collection<String> getTextInNormalizable( final IdentifiedAnnotation IdentifiedAnnotation,
                                                           final String regex ) {
      return getTextInNormalizable( IdentifiedAnnotation, Pattern.compile( regex ) );
   }

   static public Collection<String> getTextInNormalizable( final IdentifiedAnnotation IdentifiedAnnotation,
                                                           final Pattern pattern ) {
      final String text = IdentifiedAnnotation.getCoveredText();
      final RegexSpanFinder finder = new RegexSpanFinder( pattern );
      return finder.findSpans( text )
                   .stream()
                   .filter( RegexUtil::isValidSpan )
                   .map( s -> text.substring( s.getValue1(), s.getValue2() ) )
                   .collect( Collectors.toList() );
   }


}
