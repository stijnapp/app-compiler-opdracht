package nl.han.ica.icss.ast;

import java.util.ArrayList;

public class Inversion extends Expression {
    public Expression expression;

    @Override
    public ArrayList<ASTNode> getChildren() {
        ArrayList<ASTNode> children = new ArrayList<>();
        if (expression != null)
            children.add(expression);
        return children;
    }

    @Override
    public ASTNode addChild(ASTNode child) {
        if (child instanceof Expression) {
            expression = (Expression) child;
        }
        return this;
    }

    @Override
    public String getNodeLabel() {
        return "Inversion";
    }
}
