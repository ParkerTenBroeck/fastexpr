package fastexpr.ast;

public sealed interface BinOp extends Expr permits Add, Div, Mul, Pow, Sub {
    Expr lhs();

    Expr rhs();
}
