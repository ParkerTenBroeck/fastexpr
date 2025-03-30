package fastexpr.ast;

public record Add(Expr lhs, Expr rhs) implements BinOp {
}
