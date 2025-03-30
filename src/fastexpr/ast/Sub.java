package fastexpr.ast;

public record Sub(Expr lhs, Expr rhs) implements BinOp {
}
