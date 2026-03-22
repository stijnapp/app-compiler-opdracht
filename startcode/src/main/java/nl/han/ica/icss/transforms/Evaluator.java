package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import java.util.ArrayList;
import java.util.HashMap;

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
        if (node instanceof Stylesheet || node instanceof StyleRule || node instanceof IfClause || node instanceof ElseClause) {
            // new scope, so push new hashmap to list
            variableValues.addFirst(new HashMap<>());

            ArrayList<ASTNode> currentBody;
            if (node instanceof Stylesheet) {
                currentBody = ((Stylesheet) node).body;
            } else if (node instanceof StyleRule) {
                currentBody = ((StyleRule) node).body;
            } else if (node instanceof IfClause) {
                currentBody = ((IfClause) node).body;
            } else {
                currentBody = ((ElseClause) node).body;
            }

            ArrayList<ASTNode> newBody = new ArrayList<>();

            // for each child, check if it should end up in the new body
            // also calculate variable assignments
            for (ASTNode child : currentBody) {
                if (child instanceof VariableAssignment) {
                    // save the variable's name and value in the symbol table
                    VariableAssignment variableAssignment = (VariableAssignment) child;
                    String name = variableAssignment.name.name;
                    Literal value = calculateExpressionValue(variableAssignment.expression);
                    variableValues.getFirst().put(name, value);
                }

                // TR02: replace all if-else's with actual body (or nothing) based on condition
                else if (child instanceof IfClause) {
                    IfClause ifClause = (IfClause) child;

                    applyNode(ifClause);

                    BoolLiteral conditionLiteral = (BoolLiteral) calculateExpressionValue(ifClause.conditionalExpression);

                    if (conditionLiteral.value) {
                        // condition is true, add if body to new body
                        newBody.addAll(ifClause.body);
                    } else if (ifClause.elseClause != null) {
                        // condition is false and there is an else, add else body to new body
                        applyNode(ifClause.elseClause);
                        newBody.addAll(ifClause.elseClause.body);
                    }
                    // if condition is false and there is no else, just ignore the if-clause (which won't add it to the new body)
                } else {
                    // for any other child, just check and add it to the nwe body
                    applyNode(child);
                    newBody.add(child);
                }
            }

            // replace the current body with the new body with solved if-else's
            if (node instanceof Stylesheet) {
                ((Stylesheet) node).body = newBody;
            } else if (node instanceof StyleRule) {
                ((StyleRule) node).body = newBody;
            } else if (node instanceof IfClause) {
                ((IfClause) node).body = newBody;
            } else {
                ((ElseClause) node).body = newBody;
            }
        }

        // TR01: replace all variable references with their actual value
        // (also handles all operations and literals)
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            declaration.expression = calculateExpressionValue(declaration.expression);
        }

        // end of scope
        if (node instanceof StyleRule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
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
