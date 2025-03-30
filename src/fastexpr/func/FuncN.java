package fastexpr.func;

@FunctionalInterface
public non-sealed interface FuncN extends ASTFunc {
    double eval(double... args);
}
