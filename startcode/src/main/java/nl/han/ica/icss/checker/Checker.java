package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.comparisons.EqualComparison;
import nl.han.ica.icss.ast.comparisons.NotEqualComparison;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Checker {

    // symbol table: a list (which is mostly used as an iterable stack) of hashmaps
    // each hashmap represents a scope with its variable's names and types
    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        // reset symbol table when check is called (which is the root of the checker)
        variableTypes = new HANLinkedList<>();
        checkNode(ast.root);
    }

    // checkNode is called recursively, so it's possible for the node to be any type of node in the AST
    private void checkNode(ASTNode node) {
        // nodes that create a new scope: StyleRule, Stylesheet, IfClause, ElseClause
        if (node instanceof StyleRule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
            // new scope, so push new hashmap to list
            variableTypes.addFirst(new HashMap<>());
        }

        // variable assignment
        if (node instanceof VariableAssignment) {
            VariableAssignment variableAssignment = (VariableAssignment) node;
            String name = variableAssignment.name.name;
            ExpressionType varType = getExpressionType(variableAssignment.expression);
            variableTypes.getFirst().put(name, varType);
        }
        // CH01 + CH06: variables should be defined, and only used within their scope
        else if (node instanceof VariableReference) {
            VariableReference variableReference = (VariableReference) node;
            String name = variableReference.name;
            // search through the variable name in the symbol table, starting from current scope and going up
            boolean found = false;
            for (HashMap<String, ExpressionType> scope : variableTypes) {
                if (scope.containsKey(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // if not found, set error on the variable reference node
                variableReference.setError("CH01/CH06: Variable '" + name + "' is undefined");
            }
        }

        // CH02: check operands of PLUS/MIN for either the exact same types, or scalar+other
        else if (node instanceof AddOperation || node instanceof SubtractOperation) {
            ExpressionType typeofLhs = getExpressionType(((Operation) node).lhs);
            ExpressionType typeofRhs = getExpressionType(((Operation) node).rhs);

            // skip if one of the sides already has an error
            if (typeofLhs == null || typeofRhs == null) return;

            if (typeofLhs != typeofRhs && typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR) {
                node.setError("CH02: Incompatible add/subtract types: " + typeofLhs + " and " + typeofRhs);
            }
        }
        // CH02: check operands of MUL for at least one scaler
        else if (node instanceof MultiplyOperation) {
            ExpressionType typeofLhs = getExpressionType(((Operation) node).lhs);
            ExpressionType typeofRhs = getExpressionType(((Operation) node).rhs);

            if (typeofLhs == null || typeofRhs == null) return;

            if (typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR) {
                node.setError("CH02: Incompatible multiplication types: " + typeofLhs + " and " + typeofRhs + ". At least one operand of a multiplication must be a scalar.");
            }
        }

        // CH04: check if a property's value is of the correct type
        else if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            String propertyName = declaration.property.name;

            // propertyName must be one of color, background-color, width, height
            if (!List.of("color", "background-color", "width", "height").contains(propertyName)) {
                declaration.setError("Unknown property '" + propertyName + "'. Allowed properties are: color, background-color, width, height.");
                return;
            }

            ExpressionType valueType = getExpressionType(declaration.expression);

            // if valueType is null, skip because there is another error
            if (valueType == null) return;

            // if valueType is SCALAR, it's never correct because it needs to be a specific unit
            if (valueType == ExpressionType.SCALAR) {
                declaration.setError("CH04: Property '" + propertyName + "' cannot be of type SCALAR");
                return;
            }

            // color/background-color = COLOR (or variable with that type)
            if ((propertyName.equals("color") || propertyName.equals("background-color"))
                    && valueType != ExpressionType.COLOR) {
                declaration.setError("CH04: Property '" + propertyName + "' must be of type COLOR. Type is: " + valueType);
            }
            // width/height = PIXEL/PERCENTAGE (or variable with one of those types)
            else if ((propertyName.equals("width") || propertyName.equals("height"))
                    && valueType != ExpressionType.PIXEL && valueType != ExpressionType.PERCENTAGE) {
                declaration.setError("CH04: Property '" + propertyName + "' must be of type PIXEL or PERCENTAGE. Type is: " + valueType);
            }
        }

        // CH05: check if if-clause is boolean
        else if (node instanceof IfClause) {
            IfClause ifClause = (IfClause) node;
            ExpressionType conditionType = getExpressionType(ifClause.conditionalExpression);

            // skip if existing error
            if (conditionType == null) return;

            if (conditionType != ExpressionType.BOOL) {
                ifClause.setError("CH05: If-condition must be of type BOOL. Type is: " + conditionType);
            }
        }

        // check all children of this node for errors
        for (ASTNode child : node.getChildren()) {
            checkNode(child);
        }

        // nodes with ending scope
        if (node instanceof StyleRule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause) {
            // end of scope, so "pop" the hashmap from the list
            variableTypes.removeFirst();
        }
    }

    private ExpressionType getExpressionType(Expression expression) {
        if (expression instanceof Literal) {
            // simple checks for literal types
            switch (expression.getClass().getSimpleName()) {
                case "BoolLiteral":
                    return ExpressionType.BOOL;
                case "ColorLiteral":
                    return ExpressionType.COLOR;
                case "PercentageLiteral":
                    return ExpressionType.PERCENTAGE;
                case "PixelLiteral":
                    return ExpressionType.PIXEL;
                case "ScalarLiteral":
                    return ExpressionType.SCALAR;
            }
        } else if (expression instanceof VariableReference) {
            // search for variable in symbol table, starting from current scope and going up
            for (HashMap<String, ExpressionType> scope : variableTypes) {
                if (scope.containsKey(((VariableReference) expression).name)) {
                    return scope.get(((VariableReference) expression).name);
                }
            }
            // if not found, set error on the expression and return null
            expression.setError("CH01/CH06: Variable '" + ((VariableReference) expression).name + "' is undefined");
            return null;
        } else if (expression instanceof Operation) {
            ExpressionType typeofLhs = getExpressionType(((Operation) expression).lhs);
            ExpressionType typeofRhs = getExpressionType(((Operation) expression).rhs);

            // first, if one of the sides already has an error, just skip it for cleaner errors
            if (typeofLhs == null || typeofRhs == null) return null;

            // CH03: colors are not allowed in operations, so immediately set error
            if (typeofLhs == ExpressionType.COLOR || typeofRhs == ExpressionType.COLOR) {
                expression.setError("CH03: Colors are not allowed in operations");
                return null;
            }

            // if sides are the same type, return that type
            if (typeofLhs == typeofRhs) return typeofLhs;

            // CH02: if none are scalar (and they cant be the same, because of previous if), the types are incompatible
            if (typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR) {
                expression.setError("CH02: Incompatible operator types: " + typeofLhs + " and " + typeofRhs);
                return null;
            }

            // one side has to be scalar, so return the other type
            return typeofLhs == ExpressionType.SCALAR ? typeofRhs : typeofLhs;
        } else if (expression instanceof Comparison) {
            Comparison comparison = (Comparison) expression;
            ExpressionType typeofLhs = getExpressionType(((Comparison) expression).lhs);
            ExpressionType typeofRhs = getExpressionType(((Comparison) expression).rhs);

            if (typeofLhs == null || typeofRhs == null) return null;

            // TODO: extra: check for valid comparisons. eg, only allowed:
            //   comparing same types with == or !=
            //   comparing scalar, pixel, or percentage with >, <, >=, <= (given the types are the same, or one is scalar)

            if (typeofLhs == typeofRhs && (comparison instanceof EqualComparison || comparison instanceof NotEqualComparison)) {
                return ExpressionType.BOOL;
            }

            boolean LhsIsComparable = (typeofLhs == ExpressionType.SCALAR || typeofLhs == ExpressionType.PIXEL || typeofLhs == ExpressionType.PERCENTAGE);
            boolean RhsIsComparable = (typeofRhs == ExpressionType.SCALAR || typeofRhs == ExpressionType.PIXEL || typeofRhs == ExpressionType.PERCENTAGE);
            boolean eitherIsScalar = (typeofLhs == ExpressionType.SCALAR || typeofRhs == ExpressionType.SCALAR);
            boolean sizeBasedComparison = !(comparison instanceof EqualComparison || comparison instanceof NotEqualComparison);

            if (LhsIsComparable && RhsIsComparable && (typeofLhs == typeofRhs || eitherIsScalar) && sizeBasedComparison) {
                return ExpressionType.BOOL;
            }

            expression.setError("Incompatible comparison types: '" + typeofLhs + "' and '" + typeofRhs + "' with operator: " + comparison.getClass().getSimpleName());
            return null;
        }

        // this should never be reached. Maybe when a new expression is added and not handled yet?
        expression.setError("Unknown expression type: " + expression.getClass().getSimpleName());
        return null;
    }
}
