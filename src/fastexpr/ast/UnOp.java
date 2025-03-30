package fastexpr.ast;

public sealed interface UnOp extends Expr permits Neg {
    Expr expr();
}
