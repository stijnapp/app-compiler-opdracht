grammar ICSS;

// --- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';
PAREN_OPEN: '(';
PAREN_CLOSE: ')';

// function support
FUNCTION: 'fun';
RETURN: 'return';

// Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: ([1-9][0-9]* | '0') 'px';
PERCENTAGE: ([1-9][0-9]* | '0') '%';
SCALAR: [1-9][0-9]* | '0';
// Color value takes precedence over id idents
COLOR: '#' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F];

// Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;
SEPARATOR: ',';

// General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

// All whitespace is skipped
WS: [ \t\r\n]+ -> skip;
// Comments are skipped
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;

// Symbols
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';
GREATER: '>';
LESSER: '<';
GREATER_EQUAL: '>=';
LESSER_EQUAL: '<=';
EQUAL: '==';
NOT_EQUAL: '!=';


//--- PARSER: ---
stylesheet  : (assignment | stylerule | function)* EOF;

// variable assignment
assignment  : variable ASSIGNMENT_OPERATOR expression SEMICOLON;

// style rule
stylerule   : selector (SEPARATOR selector)* OPEN_BRACE body CLOSE_BRACE;
selector    : ID_IDENT | CLASS_IDENT | LOWER_IDENT;
declaration : prop=LOWER_IDENT COLON expression SEMICOLON;

// expressions - left-recursive, so in order of precedence
expression  : PAREN_OPEN expression PAREN_CLOSE #ParenthesizedExpression
            | expression op=MUL expression #OperationExpression
            | expression op=(PLUS | MIN) expression #OperationExpression
            | expression comp=(GREATER | LESSER | GREATER_EQUAL | LESSER_EQUAL | EQUAL | NOT_EQUAL) expression #ComparisonExpression
            | literal #LiteralExpression
            | name=CAPITAL_IDENT BOX_BRACKET_OPEN (expression (SEPARATOR expression)*)? BOX_BRACKET_CLOSE #FunctionReferenceExpression
            | variable #VariableExpression
            ;

// if-else
ifclause    : IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE OPEN_BRACE body CLOSE_BRACE elseclause?;
elseclause  : ELSE OPEN_BRACE body CLOSE_BRACE;

body        : (declaration | ifclause | assignment)*;

// functions
function    : FUNCTION name=CAPITAL_IDENT BOX_BRACKET_OPEN (variable (SEPARATOR variable)*)? BOX_BRACKET_CLOSE OPEN_BRACE (assignment | ifclause)* returnstmt CLOSE_BRACE;
returnstmt  : RETURN expression SEMICOLON;

// basics
variable    : CAPITAL_IDENT;
literal     : bool
            | PIXELSIZE
            | PERCENTAGE
            | SCALAR
            | COLOR
            ;
bool        : TRUE | FALSE;
