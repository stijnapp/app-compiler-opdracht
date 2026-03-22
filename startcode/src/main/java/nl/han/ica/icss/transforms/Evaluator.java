package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;

public class Evaluator implements Transform {

    // TODO: allowed to remove list?
    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();

        applyNode(ast.root);

    }

    private void applyNode(ASTNode node) {
        // same logic as in Checker, but store actual values instead of types
        if (node instanceof Stylerule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
            variableValues.addFirst(new HashMap<>());
        }

        // variable assignment
        else if (node instanceof VariableAssignment) {
            VariableAssignment variableAssignment = (VariableAssignment) node;
            String name = variableAssignment.name.name;
            Literal value = calculateExpressionValue(variableAssignment.expression);
            variableValues.getFirst().put(name, value);
        }

        // TODO: TR01: replace all variable references with their actual value
        // TODO: TR02: replace all if-else's with actual body (or nothing) based on condition

        // recursively apply to children
        for (ASTNode child : node.getChildren()) {
            applyNode(child);
        }

        // end of scope
        if (node instanceof Stylerule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
            variableValues.removeFirst();
        }
    }

    private Literal calculateExpressionValue(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        } else if (expression instanceof VariableReference) {
            VariableReference variableReference = (VariableReference) expression;
            String name = variableReference.name;
            for (HashMap<String, Literal> scope : variableValues) {
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
            }
            // variable not found. Should not be possible
            throw new RuntimeException("Variable not found: " + name);
        } else if (expression instanceof Operation) {
            Operation operation = (Operation) expression;
            Literal leftLiteral = calculateExpressionValue(operation.lhs);
            Literal rightLiteral = calculateExpressionValue(operation.rhs);

            // TODO: very crude way to get the value (without changing the Literal classes), but works for now...
            // get the int value of the left and right literals
            // replace with `getOperandValue` method
            int leftValue;
            if (leftLiteral instanceof PercentageLiteral) {
                leftValue = ((PercentageLiteral) leftLiteral).value;
            } else if (leftLiteral instanceof PixelLiteral) {
                leftValue = ((PixelLiteral) leftLiteral).value;
            } else if (leftLiteral instanceof ScalarLiteral) {
                leftValue = ((ScalarLiteral) leftLiteral).value;
            } else {
                // should not be possible. It's to suppress "Variable 'leftValue' might not have been initialized"
                throw new RuntimeException("Unsupported literal type: " + leftLiteral.getClass().getSimpleName());
            }
            int rightValue;
            if (rightLiteral instanceof PercentageLiteral) {
                rightValue = ((PercentageLiteral) rightLiteral).value;
            } else if (rightLiteral instanceof PixelLiteral) {
                rightValue = ((PixelLiteral) rightLiteral).value;
            } else if (rightLiteral instanceof ScalarLiteral) {
                rightValue = ((ScalarLiteral) rightLiteral).value;
            } else {
                throw new RuntimeException("Unsupported literal type: " + rightLiteral.getClass().getSimpleName());
            }

            // calculate integer value
            int resultValue;
            if (operation instanceof AddOperation) {
                resultValue = leftValue + rightValue;
            } else if (operation instanceof SubtractOperation) {
                resultValue = leftValue - rightValue;
            } else if (operation instanceof MultiplyOperation) {
                resultValue = leftValue * rightValue;
            } else {
                throw new RuntimeException("Unsupported operation type: " + operation.getClass().getSimpleName());
            }

            // figure out the return type based on the types of left and right. scalar is lowest priority
            if (leftLiteral instanceof PercentageLiteral || rightLiteral instanceof PercentageLiteral) {
                return new PercentageLiteral(resultValue);
            } else if (leftLiteral instanceof PixelLiteral || rightLiteral instanceof PixelLiteral) {
                return new PixelLiteral(resultValue);
            } else {
                return new ScalarLiteral(resultValue);
            }
        }
        // Shouldn't be reached, except if the code has changes in the future without updating the transformer
        throw new RuntimeException("Unsupported expression type: " + expression.getClass().getSimpleName());
    }
}
