package nl.han.ica.icss.ast;

import java.util.ArrayList;
import java.util.Objects;

public class FunctionReference extends Expression {
    public String name;
    public ArrayList<Expression> arguments;

    public FunctionReference(String name, ArrayList<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public FunctionReference(String name) {
        this(name, new ArrayList<>());
    }

    @Override
    public ASTNode addChild(ASTNode child) {
        if (child instanceof Expression) {
            arguments.add((Expression) child);
        }
        return this;
    }

    @Override
    public ArrayList<ASTNode> getChildren() {
        return new ArrayList<>(arguments);
    }

    @Override
    public String getNodeLabel() {
        return "FunctionReference (" + name + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
