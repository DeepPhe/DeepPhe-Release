package org.apache.ctakes.ner.group;


import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
public interface GroupHierarchy<G extends Group<G>> extends GroupAccessor<G>, Comparator<G> {

   /**
    * sort giving the most important group first.  This is opposite the ordinal declaration.
    * @param g1 -
    * @param g2 -
    * @return 1 if g1 is the higher group, -1 g1 is the lower group.  0 iff they are the same group.
    */
   @Override
   default int compare( G g1, G g2 ) {
      if ( g1.equals( getNullGroup() ) || g2.equals( getNullGroup() ) ) {
         if ( !g2.equals( getNullGroup() ) ) {
            return 1;
         }
         if ( !g1.equals( getNullGroup() ) ) {
            return -1;
         }
         return 0;
      }
      final Trie<G> trie = getTrie();
      if ( trie != null ) {
         if ( trie.getAncestorGroups( g2 ).contains( g1 ) ) {
            return 1;
         }
         if ( trie.getAncestorGroups( g1 ).contains( g2 ) ) {
            return -1;
         }
      }
      return g2.getOrdinal() - g1.getOrdinal();
   }

   default List<G> sort( final Collection<G> groups ) {
      return groups.stream().sorted( this ).collect( Collectors.toList() );
   }

   default Trie<G> getTrie() {
      return null;
   }

   default Collection<G> getSubsumed( G group ) {
      final Collection<G> subsumed = new HashSet<>();
      // may be null, but null is allowed.
      subsumed.add( getNullGroup() );
      final Trie<G> trie = getTrie();
      if ( trie != null ) {
         subsumed.addAll( trie.getBranchGroups( group ) );
      }
      return subsumed;
   }

   /**
    *
    * @param groups -
    * @return best group.
    */
   default G getBestGroup( Collection<G> groups ) {
      if ( groups.isEmpty() ) {
         return getNullGroup();
      }
      if ( groups.size() == 1 ) {
         return new ArrayList<>( groups ).get( 0 );
      }
      return sort( groups ).get( 0 );
   }


   /**
    * trie for subsumption assistance.
    * @param <G> AnnotationGrouping for this accessor.
    */
   final class Trie<G> {
      final Collection<TrieNode<G>> _roots = new HashSet<>();
      public void addRoot( final G group ) {
         _roots.add( new TrieNode<>( group ) );
      }
      public Collection<G> getBranchGroups( final G group ) {
         return getBranch( group )
               .stream()
               .map( TrieNode::getGroup )
               .collect( Collectors.toSet() );
      }
      public Collection<TrieNode<G>> getBranch( final G group ) {
         final TrieNode<G> node = find( group );
         if ( node != null ) {
            return node.getBranch();
         }
         return Collections.emptyList();
      }
      public Collection<G> getAncestorGroups( final G group ) {
         final TrieNode<G> node = find( group );
         if ( node != null ) {
            return node.getAncestorGroups();
         }
         return Collections.emptyList();
      }
      public TrieNode<G> find( final G group ) {
         for ( TrieNode<G> root : _roots ) {
            final TrieNode<G> node = root.find( group );
            if ( node != null ) {
               return node;
            }
         }
         return null;
      }
      public TrieNode<G> findOrCreate( final G group ) {
         final TrieNode<G> node = find( group );
         if ( node != null ) {
            return node;
         }
         return new TrieNode<>( group );
      }
      public void addChild( final G parent, final G child ) {
         final TrieNode<G> parentNode = findOrCreate( parent );
         final TrieNode<G> childNode = findOrCreate( child );
         parentNode.add( childNode );
      }
   }

   final class TrieNode<G> {
      final G _group;
      final Collection<G> _parents = new HashSet<>();
      final Collection<TrieNode<G>> _children = new HashSet<>();
      TrieNode( final G group ) {
         _group = group;
      }
      G getGroup() {
         return _group;
      }
      void add( final TrieNode<G> node ) {
         _children.add( node );
         node._parents.add( getGroup() );
         node._parents.addAll( _parents );
      }
      TrieNode<G> find( final G group ) {
         if ( _group.equals( group ) ) {
            return this;
         }
         for ( TrieNode<G> child : _children ) {
            TrieNode<G> wanted = child.find( group );
            if ( wanted != null ) {
               return wanted;
            }
         }
         return null;
      }
      Collection<TrieNode<G>> getBranch() {
         final Collection<TrieNode<G>> branch = new HashSet<>();
         for ( TrieNode<G> child : _children ) {
            branch.add( child );
            branch.addAll( child.getBranch() );
         }
         return branch;
      }
      Collection<G> getAncestorGroups() {
         return _parents;
      }
   }

}
