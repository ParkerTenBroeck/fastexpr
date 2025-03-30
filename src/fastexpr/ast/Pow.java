package fastexpr.ast;

public record Pow(Expr lhs, Expr rhs) implements BinOp {
}
