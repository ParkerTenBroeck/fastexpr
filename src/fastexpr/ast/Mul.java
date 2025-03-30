package fastexpr.ast;

public record Mul(Expr lhs, Expr rhs) implements BinOp {
}
