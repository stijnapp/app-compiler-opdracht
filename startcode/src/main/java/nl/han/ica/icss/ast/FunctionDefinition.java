package nl.han.ica.icss.ast;

import java.util.ArrayList;
import java.util.Objects;

public class FunctionDefinition extends ASTNode {
    public String name;
    public ArrayList<VariableReference> parameters = new ArrayList<>();
    public ArrayList<ASTNode> body = new ArrayList<>();
    public ReturnStatement returnValue;

    public FunctionDefinition(String name) {
        this.name = name;
    }

    @Override
    public ASTNode addChild(ASTNode child) {
        if (child instanceof VariableReference) {
            parameters.add((VariableReference) child);
        } else if (child instanceof ReturnStatement) {
            returnValue = (ReturnStatement) child;
        } else {
            body.add(child);
        }
        return this;
    }

    @Override
    public ArrayList<ASTNode> getChildren() {
        ArrayList<ASTNode> children = new ArrayList<>();
        children.addAll(parameters);
        children.addAll(body);
        if (returnValue != null)
            children.add(returnValue);
        return children;
    }

    @Override
    public String getNodeLabel() {
        return "FunctionDefinition (" + name + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnValue);
    }
}
