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
            // ignore anything but StyleRule
            if (!(node instanceof StyleRule stylerule)) continue;

            // generate the selector
            for (Selector selector : stylerule.selectors) {
                sb.append(selector.toString()).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length()).append(" {\n");

            // generate the body
            for (ASTNode bodyNode : stylerule.body) {
                // ignore anything but declaration
                if (!(bodyNode instanceof Declaration declaration)) continue;

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

        if (sb.isEmpty()) return "";

        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    private String formatLiteral(Literal literal) {
        return switch (literal) {
            case ColorLiteral colorLiteral -> colorLiteral.value;
            case PixelLiteral pixelLiteral -> pixelLiteral.value + "px";
            case PercentageLiteral percentageLiteral -> percentageLiteral.value + "%";
            case null, default -> "undefined";
        };
    }
}
