package fastexpr.func;

@FunctionalInterface
public non-sealed interface Func3 extends ASTFunc {
    double eval(double x, double y, double z);
}
