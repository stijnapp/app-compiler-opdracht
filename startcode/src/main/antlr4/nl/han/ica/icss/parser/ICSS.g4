grammar ICSS;

// --- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';

// Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [1-9][0-9]* | '0';
// Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

// Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

// General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

// All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

// Symbols
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';



//--- PARSER: ---
stylesheet  : (stylerule | assignment)* EOF;

// variable assignment
assignment  : name=variable ASSIGNMENT_OPERATOR expression SEMICOLON;

// style rule
stylerule   : selector OPEN_BRACE (declaration | ifclause)* CLOSE_BRACE;
selector    : ID_IDENT | CLASS_IDENT | LOWER_IDENT;
declaration : property=LOWER_IDENT COLON expression SEMICOLON;

// expressions - left-recursive, so MUL has higher precedence than PLUS and MIN
expression  : expression MUL expression
            | expression (PLUS | MIN) expression
            | literal
            | variable
            ;

// if-else
ifclause    : IF BOX_BRACKET_OPEN condition BOX_BRACKET_CLOSE OPEN_BRACE (declaration | ifclause)* CLOSE_BRACE elseclause?;
elseclause  : ELSE OPEN_BRACE (declaration | ifclause)* CLOSE_BRACE;
condition   : (bool | literal | variable);

variable    : CAPITAL_IDENT;
literal     : bool
            | PIXELSIZE
            | PERCENTAGE
            | SCALAR
            | COLOR
            ;
bool        : TRUE | FALSE;
