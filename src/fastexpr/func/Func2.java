package fastexpr.func;

@FunctionalInterface
public non-sealed interface Func2 extends ASTFunc {
    double eval(double x, double y);
}
