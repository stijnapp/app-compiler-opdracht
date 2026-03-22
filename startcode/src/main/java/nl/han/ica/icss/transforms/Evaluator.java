package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Evaluator implements Transform {

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
        if (node instanceof Stylesheet || node instanceof Stylerule || node instanceof IfClause || node instanceof ElseClause) {
            variableValues.addFirst(new HashMap<>());

            // create a new list to prevent ConcurrentModificationException
            ArrayList<ASTNode> children = new ArrayList<>(node.getChildren());
            for (ASTNode child : children) {
                if (child instanceof VariableAssignment) {
                    // save the variable's name and value in the symbol table
                    VariableAssignment variableAssignment = (VariableAssignment) child;
                    String name = variableAssignment.name.name;
                    Literal value = calculateExpressionValue(variableAssignment.expression);
                    variableValues.getFirst().put(name, value);

                    // remove the variable assignment node from the AST itself, since it's not needed anymore
                    node.removeChild(variableAssignment);
                }
            }
        }

        // TR01: replace all variable references with their actual value
        // (also handles all operations and literals)
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            declaration.expression = calculateExpressionValue(declaration.expression);
        }

        // TODO: TR02: replace all if-else's with actual body (or nothing) based on condition
        // else if (node instanceof IfClause) {
        //     IfClause ifClause = (IfClause) node;
        //     // condition should already be reduced to a boolean literal, so just get the value
        //     boolean conditionValue = ((BoolLiteral) ifClause.conditionalExpression).value;
        //
        //     if (conditionValue) {
        //         // TODO: replace the if-clause with all of its body nodes
        //         // body can be multiple nodes... ?
        //     } else if (ifClause.elseClause != null) {
        //         // TODO: replace the if-clause with the else clause's body nodes
        //     } else {
        //         // TODO: condition is false and no else clause, so just remove the whole if-clause
        //         replaceNode(ifClause, null);
        //     }
        // }

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

            // get operand values as ints
            int leftValue = getOperandValue(leftLiteral);
            int rightValue = getOperandValue(rightLiteral);

            // calculate int result
            int resultValue = calculateResultValue(operation, leftValue, rightValue);

            // figure out the return type based on the types of left and right. scalar is the lowest priority
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

    // TODO: add to Literal classes? would be cleaner, dont know if allowed
    private int calculateResultValue(Operation operation, int leftValue, int rightValue) {
        if (operation instanceof AddOperation) {
            return leftValue + rightValue;
        } else if (operation instanceof SubtractOperation) {
            return leftValue - rightValue;
        } else if (operation instanceof MultiplyOperation) {
            return leftValue * rightValue;
        } else {
            throw new RuntimeException("Unsupported operation type: " + operation.getClass().getSimpleName());
        }
    }

    // TODO: add to Literal classes? would be cleaner, dont know if allowed
    private int getOperandValue(Literal operand) {
        if (operand instanceof PercentageLiteral) {
            return ((PercentageLiteral) operand).value;
        } else if (operand instanceof PixelLiteral) {
            return ((PixelLiteral) operand).value;
        } else if (operand instanceof ScalarLiteral) {
            return ((ScalarLiteral) operand).value;
        } else {
            throw new RuntimeException("Unsupported literal type: " + operand.getClass().getSimpleName());
        }
    }
}
