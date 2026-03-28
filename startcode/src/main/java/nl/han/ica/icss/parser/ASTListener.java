package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.comparisons.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

    // The AST itself, that's being built while traversing the parse tree
    private final AST ast;

    // Keeps track of the parent nodes when recursively traversing the AST
    // The current parent node is always on top
    private final IHANStack<ASTNode> currentContainer;

    public ASTListener() {
        // initialize the AST and the stack
        ast = new AST();
        currentContainer = new HANStack<>();
    }

    public AST getAST() {
        return ast;
    }

    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        // stylesheet is the root of the AST, so:
        // create new AST node
        Stylesheet stylesheet = new Stylesheet();
        // set the root of the AST to this node
        ast.setRoot(stylesheet);
        // push it to the stack to become the current (and only) parent node
        currentContainer.push(stylesheet);
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterAssignment(ICSSParser.AssignmentContext ctx) {
        // for *most* declarations, create a new AST node, add it to the current parent node, and push it to the stack (to become the new parent node)
        // so for the assignment:
        // create new AST node
        VariableAssignment assignment = new VariableAssignment();
        // add it to the current parent node (which is on top of the stack)
        currentContainer.peek().addChild(assignment);
        // push this node to the stack to become the new parent node
        currentContainer.push(assignment);

        // and basically repeat that for all other rules, except for variables, literals and selectors
        // those are the bottom nodes, so they only need to be added to the current parent and move on
    }

    @Override
    public void exitAssignment(ICSSParser.AssignmentContext ctx) {
        // after visiting the children of this node, pop it off the stack to make this node's parent the current parent again
        currentContainer.pop();
    }

    @Override
    public void enterVariable(ICSSParser.VariableContext ctx) {
        VariableReference variableReference = new VariableReference(ctx.getText());
        // because variables are the bottom nodes, we don't push them to the stack, just add them to the current parent node
        // same for literals and selectors
        currentContainer.peek().addChild(variableReference);
    }

    @Override
    // OperationExpression is a subrule of expression, defined in the grammar with `#OperationExpression`
    // still, it has multiple operators (+, -, *), so we need to manually check which one it is.
    public void enterOperationExpression(ICSSParser.OperationExpressionContext ctx) {
        Operation operation = switch (ctx.op.getType()) {
            case ICSSParser.PLUS -> new AddOperation();
            case ICSSParser.MIN -> new SubtractOperation();
            case ICSSParser.MUL -> new MultiplyOperation();
            default ->
                // This shouldn't be reached, because the grammar should only allow +, - and *
                // but just to be sure, throw an exception (with line and position, because it's cool that that's possible with ANTLR)
                    throw new RuntimeException("Unknown operator: " + ctx.op.getText() + " at line " + ctx.op.getLine() + ", position " + ctx.op.getCharPositionInLine());
        };
        // `.op` is possible due to defining it in the grammar with `op=...`
        currentContainer.peek().addChild(operation);
        currentContainer.push(operation);
    }

    @Override
    public void exitOperationExpression(ICSSParser.OperationExpressionContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterComparisonExpression(ICSSParser.ComparisonExpressionContext ctx) {
        Comparison comparison = switch (ctx.comp.getType()) {
            case ICSSParser.GREATER -> new GreaterComparison();
            case ICSSParser.LESSER -> new LesserComparison();
            case ICSSParser.GREATER_EQUAL -> new GreaterEqualComparison();
            case ICSSParser.LESSER_EQUAL -> new LesserEqualComparison();
            case ICSSParser.EQUAL -> new EqualComparison();
            case ICSSParser.NOT_EQUAL -> new NotEqualComparison();
            default ->
                    throw new RuntimeException("Unknown comparison operator: " + ctx.comp.getText() + " at line " + ctx.comp.getLine() + ", position " + ctx.comp.getCharPositionInLine());
        };
        // same deal as with the operation expression, but now for the comparison operators
        currentContainer.peek().addChild(comparison);
        currentContainer.push(comparison);
    }

    @Override
    public void exitComparisonExpression(ICSSParser.ComparisonExpressionContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterLiteral(ICSSParser.LiteralContext ctx) {
        Literal literal;
        // Kind of the same deal as with the operation expression
        // there are multiple types of literals, so we need to check which one it is
        if (ctx.bool() != null) {
            literal = new BoolLiteral(ctx.bool().getText().equals("TRUE"));
        } else if (ctx.PIXELSIZE() != null) {
            literal = new PixelLiteral(Integer.parseInt(ctx.PIXELSIZE().getText().replace("px", "")));
        } else if (ctx.PERCENTAGE() != null) {
            literal = new PercentageLiteral(Integer.parseInt(ctx.PERCENTAGE().getText().replace("%", "")));
        } else if (ctx.SCALAR() != null) {
            literal = new ScalarLiteral(Integer.parseInt(ctx.SCALAR().getText()));
        } else if (ctx.COLOR() != null) {
            literal = new ColorLiteral(ctx.COLOR().getText());
        } else {
            throw new RuntimeException("Unknown literal: " + ctx.getText() + " at line " + ctx.getStart().getLine() + ", position " + ctx.getStart().getCharPositionInLine());
        }
        currentContainer.peek().addChild(literal);
    }

    @Override
    public void enterFunctionReferenceExpression(ICSSParser.FunctionReferenceExpressionContext ctx) {
        FunctionReference functionReference = new FunctionReference(ctx.name.getText());
        currentContainer.peek().addChild(functionReference);
        currentContainer.push(functionReference);
    }

    @Override
    public void exitFunctionReferenceExpression(ICSSParser.FunctionReferenceExpressionContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterFunction(ICSSParser.FunctionContext ctx) {
        FunctionDefinition functionDefinition = new FunctionDefinition(ctx.name.getText());
        currentContainer.peek().addChild(functionDefinition);
        currentContainer.push(functionDefinition);
    }

    @Override
    public void exitFunction(ICSSParser.FunctionContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterReturnstmt(ICSSParser.ReturnstmtContext ctx) {
        ReturnStatement returnStatement = new ReturnStatement();
        currentContainer.peek().addChild(returnStatement);
        currentContainer.push(returnStatement);
    }

    @Override
    public void exitReturnstmt(ICSSParser.ReturnstmtContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        StyleRule stylerule = new StyleRule();
        currentContainer.peek().addChild(stylerule);
        currentContainer.push(stylerule);
    }

    @Override
    public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterSelector(ICSSParser.SelectorContext ctx) {
        Selector selector;
        if (ctx.ID_IDENT() != null) {
            selector = new IdSelector(ctx.ID_IDENT().getText());
        } else if (ctx.CLASS_IDENT() != null) {
            selector = new ClassSelector(ctx.CLASS_IDENT().getText());
        } else if (ctx.LOWER_IDENT() != null) {
            selector = new TagSelector(ctx.LOWER_IDENT().getText());
        } else {
            throw new RuntimeException("Unknown selector: " + ctx.getText() + " at line " + ctx.getStart().getLine() + ", position " + ctx.getStart().getCharPositionInLine());
        }
        currentContainer.peek().addChild(selector);
    }

    @Override
    public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration declaration = new Declaration(ctx.prop.getText());
        currentContainer.peek().addChild(declaration);
        currentContainer.push(declaration);
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterIfclause(ICSSParser.IfclauseContext ctx) {
        IfClause ifClause = new IfClause();
        currentContainer.peek().addChild(ifClause);
        currentContainer.push(ifClause);
    }

    @Override
    public void exitIfclause(ICSSParser.IfclauseContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterElseclause(ICSSParser.ElseclauseContext ctx) {
        ElseClause elseClause = new ElseClause();
        currentContainer.peek().addChild(elseClause);
        currentContainer.push(elseClause);
    }

    @Override
    public void exitElseclause(ICSSParser.ElseclauseContext ctx) {
        currentContainer.pop();
    }
}
