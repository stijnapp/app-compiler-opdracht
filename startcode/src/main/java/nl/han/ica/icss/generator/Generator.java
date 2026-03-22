package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;

public class Generator {

    // GE01: Generate CSS2-compliant code from the transformed AST
    public String generate(AST ast) {
        StringBuilder sb = new StringBuilder();
        for (ASTNode node : ast.root.getChildren()) {
            // ignore anything but Stylerule
            if (!(node instanceof Stylerule)) continue;

            // generate the selector
            Stylerule stylerule = (Stylerule) node;
            for (Selector selector : stylerule.selectors) {
                sb.append(selector.toString()).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length()).append(" {\n");

            // generate the body
            for (ASTNode bodyNode : stylerule.body) {
                // ignore anything but declaration
                if (!(bodyNode instanceof Declaration)) continue;

                Declaration declaration = (Declaration) bodyNode;

                // GE02: indent with 2 spaces
                sb.append("  ")
                        .append(declaration.property.name)
                        .append(": ")
                        .append(formatLiteral((Literal) declaration.expression))
                        .append(";\n");
            }

            // close the stylerule
            sb.append("}\n\n");
        }
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    private String formatLiteral(Literal literal) {
        if (literal instanceof ColorLiteral) {
            return ((ColorLiteral) literal).value;
        } else if (literal instanceof PixelLiteral) {
            return ((PixelLiteral) literal).value + "px";
        } else if (literal instanceof PercentageLiteral) {
            return ((PercentageLiteral) literal).value + "%";
        } else {
            return "undefined";
        }
    }
}
