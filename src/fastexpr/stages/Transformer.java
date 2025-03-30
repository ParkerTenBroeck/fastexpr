package fastexpr.stages;

import fastexpr.ast.*;

import java.util.List;

public class Transformer {
    public static AST derive(AST f, String v){
        return new AST(
                f.name() + "'",
                List.copyOf(f.args()),
                derive(f.expr(), v)
        );
    }

    public static Expr derive(Expr expr, String v){
        return switch(expr){
            case Sub(var lhs, var rhs) -> new Sub(derive(lhs, v), derive(rhs, v));
            case Add(var lhs, var rhs) -> new Add(derive(lhs, v), derive(rhs, v));

            case Mul(Val(var val), var rhs) -> //peephole
                   new Mul(new Val(val), derive(rhs, v));
            case Mul(var lhs, Val(var val)) -> //peephole
                    new Mul(derive(lhs, v), new Val(val));
            case Mul(var lhs, var rhs) -> //general
                new Add(new Mul(derive(lhs, v), rhs), new Mul(lhs, derive(rhs, v)));

            case Div(var lhs, Val(var val)) -> // peephole
                    new Div(derive(lhs, v), new Val(val));
            case Div(Val(var val), var rhs) -> // peephole
                    new Div(new Mul(new Val(-val), derive(rhs, v)), new Pow(rhs, new Val(2)));
            case Div(var lhs, var rhs) -> new Div( //general
                    new Sub(new Mul(derive(lhs, v), rhs), new Mul(lhs, derive(rhs, v))),
                    new Pow(rhs, new Val(2))
            );

            case Pow(Expr lhs, Val(var val)) -> // peephole
                    new Mul(
                            new Mul(new Val(val), derive(lhs, v)),
                            new Pow(lhs, new Val(val-1))
                    );
            case Pow(Val(var val), Expr rhs) -> // peephole
                    new Mul(
                            new Mul(new Func("ln", List.of(new Val(val))), derive(rhs, v)),
                            new Pow(new Val(val), rhs)
                    );
            case Pow(Expr lhs, Expr rhs) -> //general
                new Mul(
                        new Pow(new Ident("e"), new Mul(rhs, new Func("ln", List.of(lhs)))),
                        derive(new Mul(rhs, new Func("ln", List.of(lhs))), v)
                );

            case Func(var name, var args) -> chainRule(name, args, v);

            case Ident(var ident) when ident.equals(v) -> new Val(1.0);
            case Neg(var rhs) -> new Neg(derive(rhs, v));
            case Ident _, Val _ -> new Val(0.0);
        };
    }

    private static Expr chainRule(String func, List<Expr> args, String v){
        Expr res = switch (func){
            case "sin" ->  new Func("cos", args);
            case "cos" ->  new Neg(new Func("cos", args));
            case "ln" ->  new Div(new Val(1), args.getFirst());
            default -> throw new RuntimeException("Unknown function " + func);
        };
        var der = derive(args.getFirst(), v);
        return new Mul(res, der);
    }

    public static String toString(AST ast){
        return ast.name() + "(" + String.join(", ", ast.args()) + ")=" + toString(ast.expr());
    }

    public static String toString(Expr expr){
        return switch (expr){
            case Add(var lhs, var rhs) -> toString(lhs) + "+" + toString(rhs);
            case Sub(var lhs, var rhs) -> toString(lhs) + "-" + toString(rhs);
            case Mul(var lhs, var rhs) -> conditionalParens(lhs, add_sub) + "*" + conditionalParens(rhs, add_sub);
            case Div(var lhs, var rhs) -> conditionalParens(lhs, add_sub) + "/" + conditionalParens(rhs, add_sub);
            case Pow(var lhs, var rhs) ->conditionalParens(lhs, add_sub_mul_div_pow) + "^" + conditionalParens(rhs, add_sub_mul_div_pow);
            case Func(var name, var args) -> name + "(" + String.join(", ", args.stream().map(Transformer::toString).toList()) + ")";
            case Neg(var lhs) -> "-" + conditionalParensInv(lhs, Val.class, Lexer.Ident.class, Func.class, Pow.class);
            case Ident(var ident) -> ident;
            case Val(var val) -> formatDouble(val);
        };
    }

    public static String formatDouble(double value) {
        return switch(value){
            case 0.0d -> "0";
            case int v -> v+"";
            case Math.PI -> "pi";
            case Math.E -> "e";
            case double v when Math.abs(v) >= 1e6 || Math.abs(v) < 1e-6 ->
                    String.format("%.6e", value).replace("e+0", "e").replace("e-0", "e-");
            default -> String.format("%f", value).replaceAll("\\.?0+$", "");
        };
    }

    private static final Class<?>[] add_sub = new Class[]{Add.class, Sub.class};
    private static final Class<?>[] add_sub_mul_div_pow = new Class[]{Add.class, Sub.class, Mul.class, Sub.class, Pow.class};
    private static String conditionalParens(Expr expr, Class<?>... requires){
        for(var clazz : requires)
            if(clazz.isInstance(expr))
                return "(" + toString(expr) + ")";
        return toString(expr);
    }
    private static String conditionalParensInv(Expr expr, Class<?>... requires){
        for(var clazz : requires)
            if(clazz.isInstance(expr))
                return toString(expr);
        return "(" + toString(expr) + ")";
    }
}
