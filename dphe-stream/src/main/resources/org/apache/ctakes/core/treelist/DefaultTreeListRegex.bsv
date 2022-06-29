// The bsv format, with optional named groups is:
// TreeList Name/Type || Full Regex; Heading, Refinement, List || List Entry Regex; Index, Name, Value, Details

// !!!!    ^(?<Header>[A-Z ]{4,})(?<Refinement>[ \t]+\([^\r\n:]*\))?[ ]*\r?\n(?<List>(?:(?:[A-Z][^\r\n:]{2,}):[ \t]+[^\r\n]+\r?\n(?:(?:[ \t]+[^\r\n:]{2,}:)?[ \t]+[^\r\n]+\r?\n)*)+)
////  Will Match  HEADER (Refinement) \n All kinds of lists as long as at least one list line has a colon.

Caps Header Mixed List||^(?<Header>[A-Z ]{4,})(?<Refinement>[ \t]+\([^\r\n:]*\))?[ ]*\r?\n(?<List>(?:[A-Z][^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:(?:[ \t]+[^\r\n:]{2,}:)?[ \t]+[^\r\n]+\r?\n)*)+)||^(?:(?<Name>[^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n


////  Will Match {Header text} {(stuff)}:
////    ^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:
////  Will Match Above plus value plus {multi word text}
////    ^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<List>[ \t]{2,}[^\r\n:]{2,}:?[ \t]+[^\r\n]+\r?\n)
////  Will Match Above but requires 2 colons {Header text] {(stuff)}:  {name text}: {value text}
////    ^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<List>[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n)
////  Will Match Above plus multiple lines of indented  {name text}:{value text}
////    ^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<List>[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n)*)
////  Will Match Above plus multiple lines of indented with or without :   {name text}:?{value text}   !!! As Long as at least 1 line has a {name}:{answer}  !!!
////    ^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<List>[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:[ \t]{2,}[^\r\n:]{2,}:?[ \t]+[^\r\n]+\r?\n)*)
// !!!!   So, start with the full expression above.
// !!!!   Will match Name, Value of above
// !!!!    [ \t]{2,}(?:(?<Name>[^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n

Header Colon Name Colon Value||^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<List>[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:[ \t]{2,}[^\r\n:]{2,}:?[ \t]+[^\r\n]+\r?\n)*)||^[ \t]{2,}(?:(?<Name>[^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n

//// Will Match {Header text}: {Header Value} then an indented list of name : value  -- Not necessary?  Caught by general name:value list
//Header Colon Value Name Colon Value||^(?<Heading>[A-Z][^\r\n\(\):]{2,})(?<Refinement>\([^\r\n:]*\))?:(?<HeadingValue>[^\r\n]+)?\r?\n(?<List>[ \t]{2,}[^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:[ \t]{2,}[^\r\n:]{2,}:?[ \t]+[^\r\n]+\r?\n)*)||^[ \t]{2,}(?:(?<Name>[^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n


////  Will Match {Header text}:\n and then following indented name: value lines

Header Colon Indented Mixed List||^(?<Header>[A-Z][^\r\n:]{4,}):[ ]*\r?\n(?<List>(?:[ \t]+[A-Z][^\r\n:]{2,}:[ \t]+[^\r\n]+\r?\n(?:(?:[ \t]+[^\r\n:]{2,}:)?[ \t]+[^\r\n]+\r?\n)*)+)||^(?:[ \t]+(?<Name>[^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n



// Mixed Colon Name Colon Value||^(?<Heading>[^\r\n:\(\)]{2,})(?:\((?<Refinement>[^\r\n:]*)\))?:(?<List>(?:[ \t]{2,}(?:[^\r\n:0-9][^\r\n]{2,})\r?\n)(?:^[ \t]{2,}(?:[^\r\n:0-9][^\r\n]{2,})\r?\n)*)||[ \t]{2,}(?:(?<Name>[^\r\n:0-9][^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n

// CAPS Name Colon Value||^[ \t]*(?<Heading>(?:[A-Z0-9]+[ ]*)+)(?<Refinement>\([^\r\n]*\))?[ \t]*\r?\n(?<List>(?:^[^\r\n:0-9][^\r\n:]{2,}:[^\r\n]+\r?\n(?:^(?:[ A-Za-z0-9]{0,4}[ a-z0-9]+)\r?\n)*)+)||(?<Name>^[^\r\n:0-9][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^(?:[ A-Za-z0-9]{0,4}[ a-z0-9]+)\r?\n)*)

// ^(?<Heading>[A-Z](?:[^\r\n\(\):]{2,})+)(?<Refinement>\([^\r\n:]*\))?:(?<List>(?:[ \t]{2,}(?:[^\r\n:0-9][^\r\n:]{2,}):?[ \t]+(?:[^\r\n]+)\r?\n){2,})

// Example:    A/1.  Right arm stuff and text.
Slash Index Description||(?<List>(?:^[ \t]*[A-Z]\/[0-9][\).:][ \t]+[^\r\n]+\r?\n(?:^[^ \r\n][^\/][^\r\n]+\r?\n)*){2,})||^[ \t]*(?<Index>[A-Z]\/[0-9])[\).:][ \t]+(?<Details>[^\r\n]+\r?\n(?:^[^ \r\n][^\/][^\r\n]+\r?\n)*)


// An Name : Value on each row, separated by a colon character or some large amount of whitespace.  Can start with '*'
//Name Colon Value||(?<List>(?:^[ \t]*[A-Z][^\r\n:]{2,}:[^\r\n:]+\r?\n(?:^[^:\r\n]+\r?\n)*)+)||^[ \t]*(?<Name>[A-Z][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^[^:\r\n]+\r?\n)*)
Name Colon Value||(?<List>(?:^[ \t]*\*?[A-Z][^\r\n:]{2,}:[^\r\n:]+\r?\n(?:^[^:\r\n]+\r?\n)*)+)||^[ \t]*(?<Name>\*?[A-Z][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^[^:\r\n]+\r?\n)*)
Name Space Value||(?<List>(?:^[ \t]*[A-Z][^:\r\n]{3,}(?:[ ]{2,12}|[\t]{1,3})+[^:\r\n]+\r?\n)+)||^[ \t]*(?<Name>[A-Z][^:\r\n]{3,})(?:[ ]{2,12}|[\t]{1,3})+(?<Value>[^:\r\n]+)


// Mixed Colon Name Colon Value||^(?<Heading>[^\r\n:\(\)]{2,})(?:\((?<Refinement>[^\r\n:]*)\))?:(?<List>(?:[ \t]{2,}(?:[^\r\n:0-9][^\r\n]{2,})\r?\n)(?:^[ \t]{2,}(?:[^\r\n:0-9][^\r\n]{2,})\r?\n)*)||[ \t]{2,}(?:(?<Name>[^\r\n:0-9][^\r\n:]{2,}):)?[ \t]+(?<Value>[^\r\n]+)\r?\n

// CAPS Name Colon Value||^[ \t]*(?<Heading>(?:[A-Z0-9]+[ ]*)+)(?<Refinement>\([^\r\n]*\))?[ \t]*\r?\n(?<List>(?:^[^\r\n:0-9][^\r\n:]{2,}:[^\r\n]+\r?\n(?:^(?:[ A-Za-z0-9]{0,4}[ a-z0-9]+)\r?\n)*)+)||(?<Name>^[^\r\n:0-9][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^(?:[ A-Za-z0-9]{0,4}[ a-z0-9]+)\r?\n)*)

// ^(?<Heading>[A-Z](?:[^\r\n\(\):]{2,})+)(?<Refinement>\([^\r\n:]*\))?:(?<List>(?:[ \t]{2,}(?:[^\r\n:0-9][^\r\n:]{2,}):?[ \t]+(?:[^\r\n]+)\r?\n){2,})


// Here are plain old List regex.  They should work with the TreeListFinder but each list will have no Heading.

// An Alpha or Number index on each row, each row having some simple informational Details.
Alpha Description||(?<List>(?:^[ \t]*[A-Z][\).:][ \t]+[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n){2,})+)||^[ \t]*(?<Index>[A-Z])[\).:][ \t]+(?<Details>[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)
Number Description||(?<List>(?:^[ \t]*[0-9][\).:][ \t]+[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*){2,})||^[ \t]*(?<Index>[0-9])[\).:][ \t]+(?<Details>[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)

// An Name : Value on each row, separated by a colon character or some large amount of whitespace.
//Name Colon Value||(?<List>(?:^[^\r\n:0-9][^\r\n:]{2,}:[^\r\n:]+\r?\n(?:^[^:\r\n]+\r?\n)*){2,})||^(?<Name>[^\r\n:0-9][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^[^:\r\n]+\r?\n)*)
//Name Space Value||(?<List>(?:^[ \t]*[^0-9\r\n][^:\r\n]{3,}(?:[ ]{2,12}|[\t]{1,3})+[^:\r\n]+\r?\n){2,})||^(?<Name>[ \t]*[^0-9\r\n][^:\r\n]{3,})(?:[ ]{2,12}|[\t]{1,3})+(?<Value>[^:\r\n]+)

Space Name Colon Value||(?<List>(?:^[ \t]*[^\r\n:0-9][^\r\n:]{2,}:[^\r\n:]+\r?\n(?:^[^:\r\n]+\r?\n)*)+)||^[ \t]*(?<Name>[^\r\n:0-9][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^[^:\r\n]+\r?\n)*)
Space Name Space Value||(?<List>(?:^[ \t]*[^ \r\n0-9][^\r\n:]{2,}(?:[ ]{2,}|[\t]+)[^:\r\n]+\r?\n)+)||^[ \t]*(?<Name>[^\r\n:0-9][^\r\n:]{2,})(?:[ ]{2,}|[\t]+)(?<Value>[^:\r\n]+)



// Checkboxes.  These rely upon each checkbox having brackets or underscores and Y,N or X values.
Name Checkbox||(?<List>(?:^[^\r\n:]{2,}\[[XYN _]*\]\r?\n){3,})||^(?<Name>[^\r\n:]{2,})\[(?<Value>[XYN _]*)\]
Name Newline Checkbox||(?<List>(?:^[^\r\n]{2,}\r?\n\[[XYN _]*\]\r?\n){3,})||^(?<Name>[^\r\n]{2,})\r?\n\[(?:<Value>[XYN _]*)\]
Name Checkbox Details||(?<List>(?:^[^\r\n:]{2,}\[[XYN _]*\][^\r\n]{2,}\r?\n){3,})||^(?<Name>[^\r\n:]{2,})\[(?<Value>[XYN _]*)\](?<Details>[^\r\n]{2,})
Name Colon Checkbox Details||(?<List>(?:^[^\r\n:]{2,}:[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n){3,})||^(?<Name>[^\r\n:]+):[\t ]*\[(?<Value>[XYN _]*)\](?<Details>[^\r\n]{2,})
Checkbox Name||(?<List>(?:^[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n){3,})||^[\t ]*\[(?<Value>[XYN _]*)\](?<Name>[^\r\n]{2,})
Checkbox Colon Name||(?<List>(?:^[\t ]*\[[XYN _]*\][\t :]*:[^\r\n]{2,}\r?\n){3,})||^[\t ]*\[(?<Value>[XYN _]*)\][\t ]*:(?<Name>[^\r\n]{2,})

