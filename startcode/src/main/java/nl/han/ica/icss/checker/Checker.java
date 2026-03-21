package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;


public class Checker {

    // symbol table: a stack of hashmaps. each hashmap represents a scope with its variable's names and types
    private IHANStack<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        // reset symbol table when check is called (which is the root of the checker)
        variableTypes = new HANStack<>();
        checkNode(ast.root);
    }

    private void checkNode(ASTNode node) {
        // checkNode is called recursively, so it's possible for the node to be any type of node in the AST

        // nodes that create a new scope: Stylerule, Stylesheet, IfClause, ElseClause
        if (node instanceof Stylerule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
            // new scope, so push new hashmap to stack
            variableTypes.push(new HashMap<>());
            // then check all children of this node for their variable usage and add errors if needed
            for (ASTNode child : node.getChildren()) {
                checkNode(child);
            }
            // exit scope, so pop the hashmap from the stack
            variableTypes.pop();
        }
        // variable assignment
        else if (node instanceof VariableAssignment) {
            VariableAssignment variableAssignment = (VariableAssignment) node;
            String name = variableAssignment.name.name;
            ExpressionType varType = getExpressionType(variableAssignment.expression);
            variableTypes.peek().put(name, varType);
        }
    }

    private ExpressionType getExpressionType(Expression expression) {
        if (expression instanceof Literal) {
            if (expression instanceof BoolLiteral) {
                return ExpressionType.BOOL;
            } else if (expression instanceof ColorLiteral) {
                return ExpressionType.COLOR;
            } else if (expression instanceof PercentageLiteral) {
                return ExpressionType.PERCENTAGE;
            } else if (expression instanceof PixelLiteral) {
                return ExpressionType.PIXEL;
            } else if (expression instanceof ScalarLiteral) {
                return ExpressionType.SCALAR;
            }
        } else if (expression instanceof VariableReference) {
            // TODO: search for the variable in the symbol table. either this scope or above (maybe change stack back to linked list for easier searching?)
            // If it doesnt exist, return an error somehow
            // otherwise return the type of the variable
            // TODO: tempo return for now:
            return ExpressionType.SCALAR;
        } else if (expression instanceof AddOperation || expression instanceof SubtractOperation || expression instanceof MultiplyOperation) {
            // TODO: scalar for now, but could be other, eg. 2*10px should be pixel...
            return ExpressionType.SCALAR;
        }
        // TODO: null for now. should this throw an exception?
        return null;
    }
}
