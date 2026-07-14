package org.healthnlp.deepphe.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {4/16/2024}
 */
final public class VumcDocFixer {

   //  !!!!  A bunch of notes do not use period characters or even have a space at the end of many sentences!
   //  !!!!  "She has not vomited todayDiarrhea todayPt is still requesting MRI, chemo today. Also wants " ...

   static private final Pattern JS_PATTERN_1
         = Pattern.compile(
         "[0-9]\\. javascript:top\\.vois_link_to_plan\\('\\*?\\*?ID_NUM','\\w+'\\);" );
   static private final Pattern JS_PATTERN_2
         = Pattern.compile(
         "[0-9]\\. javascript:Lnk\\(\"fastlabs.cgi[\\?:\\.\\-\\+\\*\\w]+\",\"[0-9]+\"\\);" );
   static private final Pattern NAME_PATTERN = Pattern.compile( "\\*\\*NAME\\[(.*?)\\]" );
   static private final Pattern TAG_PATTERN = Pattern.compile( "</? ?[A-Za-z]+[\\w =\"]*>" );

   static private final Map<String, String> NAME_MAP = new HashMap<>( 26 );
   static private final Map<String, String> CODE_CHARS = new HashMap<>( 7 );

   static {
      NAME_MAP.put( "AAA", "James" );
      NAME_MAP.put( "BBB", "Robert" );
      NAME_MAP.put( "CCC", "John" );
      NAME_MAP.put( "DDD", "Michael" );
      NAME_MAP.put( "EEE", "David" );
      NAME_MAP.put( "FFF", "William" );
      NAME_MAP.put( "GGG", "Richard" );
      NAME_MAP.put( "HHH", "Joseph" );
      NAME_MAP.put( "III", "Thomas" );
      NAME_MAP.put( "JJJ", "Christopher" );
      NAME_MAP.put( "KKK", "Charles" );
      NAME_MAP.put( "LLL", "Daniel" );
      NAME_MAP.put( "MMM", "Matthew" );
      NAME_MAP.put( "NNN", "Mary" );
      NAME_MAP.put( "OOO", "Patricia" );
      NAME_MAP.put( "PPP", "Jennifer" );
      NAME_MAP.put( "QQQ", "Linda" );
      NAME_MAP.put( "RRR", "Elizabeth" );
      NAME_MAP.put( "SSS", "Barbara" );
      NAME_MAP.put( "TTT", "Susan" );
      NAME_MAP.put( "UUU", "Jessica" );
      NAME_MAP.put( "VVV", "Sarah" );
      NAME_MAP.put( "WWW", "Karen" );
      NAME_MAP.put( "XXX", "Lisa" );
      NAME_MAP.put( "YYY", "Nancy" );
      NAME_MAP.put( "ZZZ", "Betty" );
      CODE_CHARS.put( "&lt;", "<" );
      CODE_CHARS.put( "&gt;", ">" );
      CODE_CHARS.put( "&sol;", "/" );
      CODE_CHARS.put( "&quot;", "\"" );
      CODE_CHARS.put( "&apos;", "'" );
      CODE_CHARS.put( "&amp;", "&" );
      CODE_CHARS.put( "&nbsp;", " " );
   }

//   // In -Critical_* notes, which don't actually contain much usable information, but also some labs that might.
//   static private final Pattern E_DOCS_PATTERN = Pattern.compile( "<E-docs code=[0-9]{4}>" );
//   static private final Collection<String> MARKUP_TAGS = new HashSet<>( Arrays.asList(
//         "<study_id>", "<critical_message>", "<radiologist_id>", "<radiologist_name>", "<event_type>", "<event_time>",
//         "<event_user>", "<event_user_real_name>", "<event_user_pager_num>", "<contact_name>", "<contact_role>",
//         "<read_back>", "<event_team>", "<event_team_pager>",
//
//         "</study_id>", "</critical_message>", "</radiologist_id>", "</radiologist_name>", "</event_type>", "</event_time>",
//         "</event_user>", "</event_user_real_name>", "</event_user_pager_num>", "</contact_name>", "</contact_role>",
//         "</read_back>", "</event_team>", "</event_team_pager>", "</E-docs>"
//         ) );


   static public String fixDoc( final File file ) {
      String text = readFile(file).trim();
      return fixText(text);
   }

   static public String fixText( String text ) {
      if ( text.isEmpty() ) {
         return text;
      }
      // Some notes use "+----+" or "------" or "::::" of varying length to denote newline / separator.
      text = text.replaceAll( "\\+?[\\-]{4,}\\+?", "\n" );
      text = text.replaceAll( ":{4,}", "\n" );
      // Get rid of javascript garbage.  Do this before removing generic tags.
      text = JS_PATTERN_1.matcher( text ).replaceAll( " " );
      text = JS_PATTERN_2.matcher( text ).replaceAll( " " );
      // Replace markup line breaks.  Do this before removing generic tags.
      text = text.replace( "<BR>", "\n" );
      text = text.replace( ".BR.", "\n" );
      for ( Map.Entry<String, String> code_char : CODE_CHARS.entrySet() ) {
         text = text.replace( code_char.getKey(), code_char.getValue() );
      }
      // Replace generic tags after replacing line break.  This could remove some rare wanted (non-tag) text.
      text = TAG_PATTERN.matcher( text ).replaceAll( " " );
      // Some notes use '|' instead of line break?  Some for spacing such as in tables.
      text = text.replace( "|", "     " );
      // Add space before and after parentheses '(' ')'.
      text = text.replace( "(", " (" );
      text = text.replace( ")", ") " );
      // Garbage.
      text = text.replace( "References Visible links", " " );
      // replace DeId.
      text = fixDeId( text );
      return text.trim();
   }


   //  **NAME[AAA,BBB]      **NAME[ZZZ]    **NAME[AAA's M]   **NAME[NNN, OOO M]  **NAME[ZZZ, YYY XXX]  **NAME[M&M]
   //  **ID-NUM             [ID***]
   //  **DATE[Dec 16 62]    **DATE[Jul 01 2016]     **DATE[Sep 30 15]:30:00 CST 2016   **DATE[Sep 30 15]:30:00 CST 2016 03:30 PM
   //  **AGE[in 50s]
   //  **PHONE              **PHONEarno8fy         ***PHONE       **EMAIL
   //  **INSTITUTION        **INSTITUTION1211
   //  **PLACE              **ZIP-CODE             **ROOM      **STREET-ADDRESS

   //  !! Sometimes the above are in parentheses or braces:  (**NAME[zzz, yyy])  {**NAME[AAA]}
   //  !! There are valid multi-* terms:  ***Change:     ***Note:
   //  !! Replace:
   //  **ID-NUM , [ID***] : " ID123456789 "
   //  **NAME[AAA,BBB] : " Josephine Claudwell "
   //  **DATE[1 2 3] : " 1 2 3 "
   //  **AGE[in 50s] : " in 50s "
   //  **PHONE , ***PHONE , **EMAIL , **INSTITUTION , **PLACE , **Zip-CODE , **ROOM : " "
   static private String fixDeId( String text ) {
      text = fixName( text );
      text = fixId( text );
      text = fixDateAge( text );
      text = fixContact( text );
      text = fixPlace( text );
      text = fixBlock( text );
      return text;
   }


   static private String fixName( final String text ) {
      final Matcher matcher = NAME_PATTERN.matcher( text );
      int nextStart = 0;
      final StringBuilder sb = new StringBuilder();
      boolean found = matcher.find();
      if ( !found ) {
         return text;
      }
      while ( found ) {
         final int start = matcher.start();
         String group = matcher.toMatchResult().group( 1 );
         for ( Map.Entry<String, String> sub : NAME_MAP.entrySet() ) {
            group = group.replace( sub.getKey(), sub.getValue() );
         }
         sb.append( text, nextStart, start ).append( " " ).append( group ).append( " " );
         nextStart = matcher.end();
         found = matcher.find();
      }
      if ( nextStart < text.length() ) {
         // Need the end of the text.
         sb.append( text.substring( nextStart ) );
      }
      return sb.toString();
   }

   static private String fixId( String text ) {
      //  **ID-NUM             [ID***]
      text = text.replace( "**ID-NUM", " Id123456789 " );
      text = text.replace( "[ID***]", " Id987654321 " );
      return text;
   }

   static private String fixDateAge( String text ) {
      text = text.replaceAll( "\\*\\*DATE\\[(.*?)\\]", " $1 " );
      return text.replaceAll( "\\*\\*AGE\\[(.*?)\\]", " $1 " );
   }

   static private String fixContact( String text ) {
      text = text.replace( "**PHONE", " 555-1212 " );
      text = text.replace( "**EMAIL", " fake.account@notmailserver.com " );
      return text;
   }

   static private String fixPlace( String text ) {
      text = text.replace( "**INSTITUTION **INSTITUTION", " Unreal Institute " );
      text = text.replace( "**INSTITUTION", " Unreal Foundation " );
      text = text.replace( "**PLACE", " Someplace " );
      text = text.replace( "**ZIP-CODE", " 12345 " );
      text = text.replace( "**ROOM", " Room 100 " );
      text = text.replace( "**STREET-ADDRESS", " 101 fake St. " );
      return text;
   }

   static private String fixBlock( String text ) {
      return text.replace( "***FOOTER-BLOCK", "" );
   }


   static private String readFile( final File file ) {
      final StringBuilder sb = new StringBuilder();
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            sb.append( line.trim() ).append( "\n" );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
      return sb.toString().trim();
   }

   public static void main( String[] args ) {
      System.out.println( fixName( "daddy**NAME[XXX YYY]had a baby **NAME[AAA M BBB] with mamma**NAME[CCC]today." ) );
      System.out.println( fixId( "daddy**ID-NUMhad a baby [ID***] with mamma[ID***]today." ) );
      System.out.println(
            fixDateAge( "funky fresh**DATE[24 25 96]for baby ***DATE[Jul 22 1976] daddy**AGE[in 40s]." ) );
      System.out.println(
            JS_PATTERN_1.matcher( "References Visible links 1. javascript:top.vois_link_to_plan('**ID_NUM','jd451');" +
                  " 3. javascript:top.vois_link_to_plan('**ID_NUM','jd101');" ).replaceAll( " LINK1 " ) );
      System.out.println( JS_PATTERN_2.matcher( "References Visible links 1." +
            " javascript:Lnk(\"fastlabs.cgi?-I:**IDNUM.vumc+-sample+**IDNUM\",\"1\");" ).replaceAll( " LINK2 " ) );
      System.out.println( TAG_PATTERN.matcher( "<TR>first text</ TR><header asdf=aasdf><some " +
            "class=\"asdf\"> second text </class></header>" ).replaceAll( " TAG " ) );
   }
}
