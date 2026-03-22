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
COLOR: '#' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F];

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
stylesheet  : (assignment | stylerule)* EOF;

// variable assignment
assignment  : variable ASSIGNMENT_OPERATOR expression SEMICOLON;

// style rule
stylerule   : selector OPEN_BRACE body CLOSE_BRACE;
selector    : ID_IDENT | CLASS_IDENT | LOWER_IDENT;
declaration : prop=LOWER_IDENT COLON expression SEMICOLON;

// expressions - left-recursive, so MUL has higher precedence than PLUS and MIN
expression  : expression op=MUL expression #OperationExpression
            | expression op=(PLUS | MIN) expression #OperationExpression
            | literal #LiteralExpression
            | variable #VariableExpression
            ;

// if-else
ifclause    : IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE OPEN_BRACE body CLOSE_BRACE elseclause?;
elseclause  : ELSE OPEN_BRACE body CLOSE_BRACE;

body       : (declaration | ifclause | assignment)*;


// basics
variable    : CAPITAL_IDENT;
literal     : bool
            | PIXELSIZE
            | PERCENTAGE
            | SCALAR
            | COLOR
            ;
bool        : TRUE | FALSE;
