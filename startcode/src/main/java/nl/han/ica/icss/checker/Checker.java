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

import java.util.HashMap;
import java.util.List;


public class Checker {

    // symbol table: a list (which is mostly used as an iterable stack) of hashmaps
    // each hashmap represents a scope with its variable's names and types
    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;
    private HashMap<String, FunctionDefinition> availableFunctions;

    public void check(AST ast) {
        // reset symbol table when check is called (which is the root of the checker)
        variableTypes = new HANLinkedList<>();
        availableFunctions = new HashMap<>();

        // pre-population of available functions and duplicate function check
        for (ASTNode node : ast.root.getChildren()) {
            if (!(node instanceof FunctionDefinition functionDefinition)) continue;

            String functionName = functionDefinition.name;
            if (availableFunctions.containsKey(functionName)) {
                functionDefinition.setError("Function '" + functionName + "' is already defined");
            } else {
                availableFunctions.put(functionName, functionDefinition);
            }
        }

        checkNode(ast.root);
    }

    // checkNode is called recursively, so it's possible for the node to be any type of node in the AST
    private void checkNode(ASTNode node) {
        // boolean for if this node creates a new scope
        boolean createsNewScope = node instanceof StyleRule || node instanceof Stylesheet || node instanceof IfClause || node instanceof ElseClause;

        if (createsNewScope) {
            // new scope, so push new hashmap to list
            variableTypes.addFirst(new HashMap<>());
        }

        switch (node) {
            case VariableAssignment variableAssignment -> validateVariableAssignment(variableAssignment);
            case VariableReference variableReference -> validateVariableReference(variableReference);
            case AddOperation ignored -> validateCalculationOperation(node);
            case SubtractOperation ignored2 -> validateCalculationOperation(node);
            case MultiplyOperation ignored3 -> validateCalculationOperation(node);
            case Declaration declaration -> validateDeclaration(declaration);
            case IfClause ifClause -> validateIfClause(ifClause);
            case FunctionDefinition ignored -> {
                // explicitly skip function definitions, because they are checked when called.
                // skipping prevents the checkNode from being called on its children,
                //   which otherwise will cause errors of undefined variables (it doesn't recognize params as params)
                return;
            }
            default -> {}
        }

        // check all children of this node for errors
        for (ASTNode child : node.getChildren()) {
            checkNode(child);
        }

        // nodes with ending scope
        if (createsNewScope) {
            // end of scope, so "pop" the hashmap from the list
            variableTypes.removeFirst();
        }
    }

    private void validateVariableAssignment(VariableAssignment variableAssignment) {
        String name = variableAssignment.name.name;
        ExpressionType varType = getExpressionType(variableAssignment.expression);

        // check if variable is already defined in current scope, and if it's the same type
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(name)) {
                ExpressionType existingType = scope.get(name);
                if (existingType != varType) {
                    // set error if types don't match
                    variableAssignment.setError("Variable '" + name + "' can't be redefined with a different type. Existing type: " + existingType + ", new type: " + varType);
                }
                // don't need to update the value, since this is the checker, not the evaluator. just break loop
                return;
            }
        }

        // if not found, add it to the current scope (which is the first hashmap in the list)
        variableTypes.getFirst().put(name, varType);
    }

    // CH01 + CH06: variables should be defined, and only used within their scope
    private void validateVariableReference(VariableReference variableReference) {
        String name = variableReference.name;

        // search through the variable name in the symbol table, starting from current scope and going up
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(name)) {
                return;
            }
        }

        // if not found, set error on the variable reference node
        variableReference.setError("CH01/CH06: Variable '" + name + "' is undefined");
    }

    private void validateCalculationOperation(ASTNode node) {
        ExpressionType typeofLhs = getExpressionType(((Operation) node).lhs);
        ExpressionType typeofRhs = getExpressionType(((Operation) node).rhs);

        boolean isAddOrSubtract = node instanceof AddOperation || node instanceof SubtractOperation;
        boolean isMultiply = node instanceof MultiplyOperation;

        // skip if one of the sides already has an error
        if (typeofLhs == null || typeofRhs == null) return;

        // CH02: check operands of PLUS/MIN for either the exact same types, or scalar+other
        if (isAddOrSubtract && typeofLhs != typeofRhs && typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR) {
            node.setError("CH02: Incompatible add/subtract types: " + typeofLhs + " and " + typeofRhs);
        }

        // CH02: check operands of MUL for at least one scaler
        if (isMultiply && typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR) {
            node.setError("CH02: Incompatible multiplication types: " + typeofLhs + " and " + typeofRhs + ". At least one operand of a multiplication must be a scalar.");
        }
    }

    // CH04: check if a property's value is of the correct type
    private void validateDeclaration(Declaration declaration) {
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
    private void validateIfClause(IfClause ifClause) {
        ExpressionType conditionType = getExpressionType(ifClause.conditionalExpression);

        // skip if existing error
        if (conditionType == null) return;

        if (conditionType != ExpressionType.BOOL) {
            ifClause.setError("CH05: If-condition must be of type BOOL. Type is: " + conditionType);
        }
    }

    private ExpressionType getExpressionType(Expression expression) {
        if (expression instanceof Literal) {
            // simple checks for literal types
            return switch (expression.getClass().getSimpleName()) {
                case "BoolLiteral" -> ExpressionType.BOOL;
                case "ColorLiteral" -> ExpressionType.COLOR;
                case "PercentageLiteral" -> ExpressionType.PERCENTAGE;
                case "PixelLiteral" -> ExpressionType.PIXEL;
                case "ScalarLiteral" -> ExpressionType.SCALAR;
                default -> null; // should never be reached
            };
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
        } else if (expression instanceof Comparison comparison) {
            ExpressionType typeofLhs = getExpressionType(comparison.lhs);
            ExpressionType typeofRhs = getExpressionType(comparison.rhs);

            if (typeofLhs == null || typeofRhs == null) return null;

            boolean isSizeComparison = !(comparison instanceof EqualComparison || comparison instanceof NotEqualComparison);
            boolean typesAreDifferent = typeofLhs != typeofRhs;
            boolean neitherIsScalar = typeofLhs != ExpressionType.SCALAR && typeofRhs != ExpressionType.SCALAR;

            // Throw an error if >, <, >=, <= AND types are different AND none of them is scalar
            if (isSizeComparison && typesAreDifferent && neitherIsScalar) {
                expression.setError("CH07: Incompatible comparison types: '" + typeofLhs + "' and '" + typeofRhs + "' with operator: " + comparison.getClass().getSimpleName());
                return null;
            }

            // return type is always bool, so if no errors, return bool
            return ExpressionType.BOOL;
        } else if (expression instanceof Inversion inversion) {
            ExpressionType expressionType = getExpressionType(inversion.expression);

            if (expressionType == null) return null;

            // Inversions can only be applied to booleans, or expressions that resolve to a boolean
            if (expressionType != ExpressionType.BOOL) {
                inversion.setError("Inversion can only be applied to BOOL types. Type is: " + expressionType);
                return null;
            }
            return ExpressionType.BOOL;
        } else if (expression instanceof FunctionReference reference) {

            // check if function exists
            if (!availableFunctions.containsKey(reference.name)) {
                reference.setError("Function '" + reference.name + "' is not defined");
                return null;
            }

            FunctionDefinition function = availableFunctions.get(reference.name);

            // check if return statement exists
            if (function.returnValue == null || function.returnValue.expression == null) {
                reference.setError("Function '" + reference.name + "' does not have a return statement");
                return null;
            }

            // check if parameter amount is correct
            if (function.parameters.size() != reference.arguments.size()) {
                reference.setError("Function '" + reference.name + "' expects " + function.parameters.size() + " arguments, got " + reference.arguments.size());
                return null;
            }

            // create new scope for function, with only it's parameters
            HashMap<String, ExpressionType> functionScope = new HashMap<>();
            for (int i = 0; i < function.parameters.size(); i++) {
                // get the name that the inside of the function expects
                String paramName = function.parameters.get(i).name;
                // get the type of the argument that is passed to the function
                ExpressionType argType = getExpressionType(reference.arguments.get(i));

                if (argType != null) {
                    functionScope.put(paramName, argType);
                }
            }

            // Move symbol table to a temporary variable, so the function can have its own scope without affecting the main one
            IHANLinkedList<HashMap<String, ExpressionType>> globalScope = variableTypes;
            // clear the main symbol table
            variableTypes = new HANLinkedList<>();
            variableTypes.addFirst(functionScope);

            // check all body nodes normally
            for (ASTNode node : function.body) {
                checkNode(node);
            }

            // get the type of the return value
            ExpressionType returnType = getExpressionType(function.returnValue.expression);

            // restore the original symbol table
            variableTypes = globalScope;

            // last check if return type is null, if so, there is an error within the function
            if (returnType == null) {
                reference.setError("Could not determine return type of function '" + reference.name + "'");
                return null;
            }

            return returnType;
        }

        // this should never be reached. Maybe when a new expression is added and not handled yet?
        expression.setError("Unknown expression type: " + expression.getClass().getSimpleName());
        return null;
    }
}
