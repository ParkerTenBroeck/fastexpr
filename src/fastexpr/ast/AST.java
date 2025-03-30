package fastexpr.ast;

import fastexpr.stages.Transformer;

import java.util.List;

public record AST(
        String name,
        List<String> args,
        Expr expr
) {
    @Override
    public String toString() {
        return Transformer.toString(this);
    }
}
