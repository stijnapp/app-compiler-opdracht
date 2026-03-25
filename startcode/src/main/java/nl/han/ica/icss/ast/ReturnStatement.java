package nl.han.ica.icss.ast;

import java.util.Objects;

public class ReturnStatement extends ASTNode {
    public Expression expression;

    @Override
    public ASTNode addChild(ASTNode child) {
        if (child instanceof Expression) {
            this.expression = (Expression) child;
        }
        return this;
    }

    @Override
    public java.util.ArrayList<ASTNode> getChildren() {
        java.util.ArrayList<ASTNode> children = new java.util.ArrayList<>();
        if (expression != null) {
            children.add(expression);
        }
        return children;
    }

    @Override
    public String getNodeLabel() {
        return "ReturnStatement";
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
