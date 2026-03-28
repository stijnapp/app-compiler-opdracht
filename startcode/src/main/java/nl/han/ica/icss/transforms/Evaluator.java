package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.comparisons.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;
    private HashMap<String, FunctionDefinition> availableFunctions;

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        availableFunctions = new HashMap<>();

        // pre-population of available functions
        for (ASTNode node : ast.root.getChildren()) {
            if (node instanceof FunctionDefinition) {
                FunctionDefinition functionDefinition = (FunctionDefinition) node;
                String functionName = functionDefinition.name;
                availableFunctions.put(functionName, functionDefinition);
            }
        }

        applyNode(ast.root);

        // TODO: remove function definitions from the AST (dont do this before this point)
    }

    private void applyNode(ASTNode node) {
        // nodes with body:
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

                    boolean found = false;
                    for (HashMap<String, Literal> scope : variableValues) {
                        if (scope.containsKey(name)) {
                            scope.put(name, value);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        variableValues.getFirst().put(name, value);
                    }
                }

                // TR02: replace all if-else's with actual body (or nothing) based on condition
                else if (child instanceof IfClause) {
                    IfClause ifClause = (IfClause) child;

                    BoolLiteral conditionLiteral = (BoolLiteral) calculateExpressionValue(ifClause.conditionalExpression);

                    if (conditionLiteral.value) {
                        // condition is true, add if body to new body
                        applyNode(ifClause);
                        newBody.addAll(ifClause.body);
                    } else if (ifClause.elseClause != null) {
                        // condition is false and there is an else, add else body to new body
                        applyNode(ifClause.elseClause);
                        newBody.addAll(ifClause.elseClause.body);
                    }
                    // if condition is false and there is no else, just ignore the if-clause (which won't add it to the new body)
                } else if (child instanceof StyleRule) {
                    // delete empty StyleRules after evaluating the body
                    StyleRule styleRule = (StyleRule) child;
                    applyNode(styleRule);
                    if (!styleRule.body.isEmpty()) {
                        newBody.add(styleRule);
                    }
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
        } else if (expression instanceof Comparison) {
            Comparison comparison = (Comparison) expression;
            Literal leftLiteral = calculateExpressionValue(comparison.lhs);
            Literal rightLiteral = calculateExpressionValue(comparison.rhs);

            boolean resultValue = calculateComparisonResult(comparison, leftLiteral, rightLiteral);
            return new BoolLiteral(resultValue);
        } else if (expression instanceof FunctionReference) {
            // TODO: dont transform the actual function. this will break the next calls
            FunctionReference functionReference = (FunctionReference) expression;
            String functionName = functionReference.name;
            FunctionDefinition functionDefinition = availableFunctions.get(functionName);

            // create new scope for within the function
            HashMap<String, Literal> functionScope = new HashMap<>();


            for (int i = 0; i < functionDefinition.parameters.size(); i++) {
                String parameterName = functionDefinition.parameters.get(i).name;
                Literal argumentValue = calculateExpressionValue(functionReference.arguments.get(i));
                functionScope.put(parameterName, argumentValue);
            }

            // temporarily replace
            IHANLinkedList<HashMap<String, Literal>> outerScope = variableValues;
            variableValues = new HANLinkedList<>();
            variableValues.addFirst(functionScope);

            // execute the function body
            for (ASTNode node : functionDefinition.body) {
                applyNode(node);
            }

            // restore the outer scope
            variableValues = outerScope;

            return calculateExpressionValue(functionDefinition.returnValue.expression);
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

    private boolean calculateComparisonResult(Comparison comparison, Literal leftValue, Literal rightValue) {
        if (comparison instanceof EqualComparison) {
            return leftValue.equals(rightValue);
        } else if (comparison instanceof NotEqualComparison) {
            return !leftValue.equals(rightValue);
        } else if (comparison instanceof GreaterComparison) {
            return getOperandValue(leftValue) > getOperandValue(rightValue);
        } else if (comparison instanceof LesserComparison) {
            return getOperandValue(leftValue) < getOperandValue(rightValue);
        } else if (comparison instanceof GreaterEqualComparison) {
            return getOperandValue(leftValue) >= getOperandValue(rightValue);
        } else if (comparison instanceof LesserEqualComparison) {
            return getOperandValue(leftValue) <= getOperandValue(rightValue);
        } else {
            throw new RuntimeException("Unsupported comparison type: " + comparison.getClass().getSimpleName());
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
