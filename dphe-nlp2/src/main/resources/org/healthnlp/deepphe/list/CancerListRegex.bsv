// The new format is:
// List Name || Full List Regex || Single List Line Regex
// Where Single Line Regex may contain the named groups <Index> <Name> <Value> <Details>



//Numbered List||(?:^[ ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+){2,}||(?:^[ ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+)
//Alpha Sentence List||(?:^[ ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n){2,}||(?:^[ ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n)
//// Name Value List||(?:^[ ]*[^\s]+:[\t ]+(?:[^\t\r\n:]+\r?\n)+){3,}||(?:^[ ]*[^\s]+:[\t ]+(?:[^\t\r\n:]+\r?\n)+)
//Name Value List||(?:^[^\t\r\n]{2,}:[\t ]+(?:[^\t\r\n:]+\r?\n)+){3,}||(?:^[^\t\r\n]{2,}:[\t ]+(?:[^\t\r\n:]+\r?\n)+)
//Multi Column List||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||\r?\n
//Mixed Column||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n[\t ]*(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||[\r\n][\t ]*(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n
// ER PR Score||(?:^(?:Result|ER:|PR:|ESTROGEN|PROGESTERONE|HER\-2\/NEU  )[^\r\n]+\r?\n){3,}||\r?\n

//Header||(?:^[^\t\r\n\.]+\.{3,}[^\t\r\n\.]+\r?\n){3,}||\r?\n
//Numbered||(?:^[\t ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+){2,}||^[\t ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[\t ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+
//Alpha Sentence||(?:^[\t ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n){2,}||^[\t ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n
//Name Colon Value||(?:^[^\r\n:0-9]{2,}:[\t ]+[^\r\n]+\r?\n)+||^[^\r\n:0-9]{2,}:[\t ]+[^\r\n]+\r?\n
//Multi Column||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||\r?\n
//Dash||(?:^[\t ]*-{1,3}[\t ]+(?:(?:[^\t\r\n-]+-?)+\r?\n){1,3}){2,}||^[\t ]*-{1,3}[\t ]+(?:(?:[^\t\r\n-]+-?)+\r?\n)+
//Dash Line||(?:^[\t ]*-{1,3}[\t ]+[^\t\r\n\:\-]+\r?\n){2,}||^[\t ]*-{1,3}[\t ]+[^\t\r\n\:\-]+\r?\n
//Document Header||(?:^[^\t\r\n\.]+\.{3,}[^\t\r\n\.]+\r?\n){3,}||\r?\n
// Checkbox Line||(?:^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*\[[XYN _]*\][^\r\n]+\r?\n)+)+||^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*\[[XYN _]*\][^\r\n]+\r?\n)+
// Checkline||(?:^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*_+[XYN]*_+[^\r\n]+\r?\n)+)+||^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*_+[XYN]*_+[^\r\n]+\r?\n)+

//Dash Separator||^[\t ]*[-_=]{3,}[\t ]*\r?\n||^[\t ]*[-_=]{3,}[\t ]*\r?\n

Alpha Description||(?:^[ \t]*[A-Z][\).:][ \t]+[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)+||^[ \t]*(?<Index>[A-Z])[\).:][ \t]+(?<Details>[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)
Number Description||(?:^[ \t]*[0-9][\).:][ \t]+[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)+||^[ \t]*(?<Index>[0-9])[\).:][ \t]+(?<Details>[^\r\n]+\r?\n(?:^[^ \r\n][^:\).][^:\r\n]+\r?\n)*)

Name Colon Value||(?:^[^\r\n:0-9][^\r\n:]{2,}:[^\r\n:]+\r?\n(?:^[^:\r\n]+\r?\n)*)+||^(?<Name>[^\r\n:0-9][^\r\n:]{2,}):(?<Value>[^\r\n]+\r?\n(?:^[^:\r\n]+\r?\n)*)
Name Space Value||(?:^[ \t]*[^0-9\r\n][^:\r\n]{3,}(?:[ ]{2,12}|[\t]{1,3})+[^:\r\n]+\r?\n)+||^(?<Name>[ \t]*[^0-9\r\n][^:\r\n]{3,})(?:[ ]{2,12}|[\t]{1,3})+(?<Value>[^:\r\n]+)

// Name Checkbox||(?:^[^\r\n:]{2,}\[[XYN _]*\]\r?\n)+^[^\r\n:]{2,}\[[XYN _]*\]\r?\n?||^[^\r\n:]{2,}\[[XYN _]*\]
// Name Newline Checkbox||(?:^[^\r\n]{2,}\r?\n\[[XYN _]*\]\r?\n)+^[^\r\n]{2,}\r?\n\[[XYN _]*\]\r?\n?||^[^\r\n]{2,}\r?\n\[[XYN _]*\]
// Name Checkbox Value||(?:^[^\r\n:]{2,}\[[XYN _]*\][^\r\n]{2,}\r?\n)+^[^\r\n:]{2,}\[[XYN _]*\][^\r\n]{2,}\r?\n?||^[^\r\n:]{2,}\[[XYN _]*\][^\r\n]{2,}
// Name Colon Checkbox Value||(?:^[^\r\n:]{2,}:[\t ]*\[[XYN _]*\][[^\r\n]{2,}\r?\n)+^[^\r\n:]{2,}:[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n?||^[^\r\n:]+:[\t ]*\[[XYN _]*\][^\r\n]{2,}
// Checkbox Value||(?:^[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n)+^[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n?||^[\t ]*\[[XYN _]*\][^\r\n]{2,}
// Checkbox Colon Value||(?:^[\t ]*\[[XYN _]*\][\t :]*:[^\r\n]{2,}\r?\n)+^[\t ]*\[[XYN _]*\][\t ]*:[^\r\n]{2,}\r?\n?||^[\t ]*\[[XYN _]*\][\t ]*:[^\r\n]{2,}

Name Checkbox||(?:^[^\r\n:]{2,}\[[XYN _]*\]\r?\n)+||^(?<Name>[^\r\n:]{2,})\[(?<Value>[XYN _]*)\]
Name Newline Checkbox||(?:^[^\r\n]{2,}\r?\n\[[XYN _]*\]\r?\n)+||^(?<Name>[^\r\n]{2,})\r?\n\[(?:<Value>[XYN _]*)\]
Name Checkbox Details||(?:^[^\r\n:]{2,}\[[XYN _]*\][^\r\n]{2,}\r?\n)+||^(?<Name>[^\r\n:]{2,})\[(?<Value>[XYN _]*)\](?<Details>[^\r\n]{2,})
Name Colon Checkbox Details||(?:^[^\r\n:]{2,}:[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n)+||^(?<Name>[^\r\n:]+):[\t ]*\[(?<Value>[XYN _]*)\](?<Details>[^\r\n]{2,})
Checkbox Name||(?:^[\t ]*\[[XYN _]*\][^\r\n]{2,}\r?\n)+||^[\t ]*\[(?<Value>[XYN _]*)\](?<Name>[^\r\n]{2,})
Checkbox Colon Name||(?:^[\t ]*\[[XYN _]*\][\t :]*:[^\r\n]{2,}\r?\n)+||^[\t ]*\[(?<Value>[XYN _]*)\][\t ]*:(?<Name>[^\r\n]{2,})

ER PR Score||(?:^(?:Result|ER:|PR:|ESTROGEN|PROGESTERONE|HER\-2\/NEU  )[^\r\n]+\r?\n){3,}||^(?<Name>Result|ER:|PR:|ESTROGEN|PROGESTERONE|HER\-2\/NEU  )(?<Value>[^\r\n]+)\r?\n
