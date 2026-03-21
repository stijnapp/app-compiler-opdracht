package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
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

    // Accumulator attributes:
    private AST ast;

    // Use this to keep track of the parent nodes when recursively traversing the ast
    private IHANStack<ASTNode> currentContainer;

    public ASTListener() {
        ast = new AST();
        currentContainer = new HANStack<>();
    }

    public AST getAST() {
        return ast;
    }

    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        Stylesheet stylesheet = new Stylesheet();
        ast.setRoot(stylesheet);
        currentContainer.push(stylesheet);
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterAssignment(ICSSParser.AssignmentContext ctx) {
        VariableAssignment assignment = new VariableAssignment();
        currentContainer.peek().addChild(assignment);
        currentContainer.push(assignment);
    }

    @Override
    public void exitAssignment(ICSSParser.AssignmentContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterVariable(ICSSParser.VariableContext ctx) {
        VariableReference variableReference = new VariableReference(ctx.getText());
        currentContainer.peek().addChild(variableReference);
    }

    @Override
    // OperationExpression is a subrule of expression, defined in the grammar with `#OperationExpression`
    public void enterOperationExpression(ICSSParser.OperationExpressionContext ctx) {
        Operation operation;
        // `.op` is possible due to defining it in the grammar with `op=...`
        switch (ctx.op.getType()) {
            case ICSSParser.PLUS:
                operation = new AddOperation();
                break;
            case ICSSParser.MIN:
                operation = new SubtractOperation();
                break;
            case ICSSParser.MUL:
                operation = new MultiplyOperation();
                break;
            default:
                // TODO: read something about error objects. forgot though, so exception for now
                throw new RuntimeException("Unknown operator: " + ctx.op.getText() + " at line " + ctx.op.getLine() + ", position " + ctx.op.getCharPositionInLine());
        }
        currentContainer.peek().addChild(operation);
        currentContainer.push(operation);
    }

    @Override
    public void exitOperationExpression(ICSSParser.OperationExpressionContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterLiteral(ICSSParser.LiteralContext ctx) {
        Literal literal;
        if (ctx.bool() != null) {
            literal = new BoolLiteral(ctx.bool().getText().equals("true"));
        } else if (ctx.PIXELSIZE() != null) {
            literal = new PixelLiteral(Integer.parseInt(ctx.PIXELSIZE().getText().replace("px", "")));
        } else if (ctx.PERCENTAGE() != null) {
            literal = new PercentageLiteral(Integer.parseInt(ctx.PERCENTAGE().getText().replace("%", "")));
        } else if (ctx.SCALAR() != null) {
            literal = new ScalarLiteral(Integer.parseInt(ctx.SCALAR().getText()));
        } else if (ctx.COLOR() != null) {
            literal = new ColorLiteral(ctx.COLOR().getText());
        } else {
            // TODO: same as in enterOperationExpression. replace with error object?
            throw new RuntimeException("Unknown literal: " + ctx.getText() + " at line " + ctx.getStart().getLine() + ", position " + ctx.getStart().getCharPositionInLine());
        }
        currentContainer.peek().addChild(literal);
    }

    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        Stylerule stylerule = new Stylerule();
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
            // TODO: check thing about erorr objects
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
