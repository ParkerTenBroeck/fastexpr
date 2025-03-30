package fastexpr.ast;

public sealed interface Expr permits BinOp, Func, Ident, UnOp, Val {
}
