package stages;

import java.util.List;

public class Transformer {
    public static Parser.AST derive(Parser.AST f, String v){
        return new Parser.AST(
                f.name() + "'",
                List.copyOf(f.args()),
                derive(f.expr(), v)
        );
    }

    public static Parser.Expr derive(Parser.Expr expr, String v){
        return switch(expr){
            case Parser.Sub(var lhs, var rhs) -> new Parser.Sub(derive(lhs, v), derive(rhs, v));
            case Parser.Add(var lhs, var rhs) -> new Parser.Add(derive(lhs, v), derive(rhs, v));

            case Parser.Mul(Parser.Val(double val), var rhs) -> //peephole
                   new Parser.Mul(new Parser.Val(val), derive(rhs, v));
            case Parser.Mul(var lhs, Parser.Val(double val)) -> //peephole
                    new Parser.Mul(derive(lhs, v), new Parser.Val(val));
            case Parser.Mul(var lhs, var rhs) -> //general
                new Parser.Add(new Parser.Mul(derive(lhs, v), rhs), new Parser.Mul(lhs, derive(rhs, v)));

            case Parser.Div(var lhs, Parser.Val(double val)) -> // peephole
                    new Parser.Div(derive(lhs, v), new Parser.Val(val));
            case Parser.Div(Parser.Val(double val), var rhs) -> // peephole
                    new Parser.Div(new Parser.Mul(new Parser.Val(-val), derive(rhs, v)), new Parser.Pow(rhs, new Parser.Val(2)));
            case Parser.Div(var lhs, var rhs) -> new Parser.Div( //general
                    new Parser.Sub(new Parser.Mul(derive(lhs, v), rhs), new Parser.Mul(lhs, derive(rhs, v))),
                    new Parser.Pow(rhs, new Parser.Val(2))
            );

            case Parser.Pow(Parser.Expr lhs, Parser.Val(double val)) -> // peephole
                    new Parser.Mul(
                            new Parser.Mul(new Parser.Val(val), derive(lhs, v)),
                            new Parser.Pow(lhs, new Parser.Val(val-1))
                    );
            case Parser.Pow(Parser.Val(double val), Parser.Expr rhs) -> // peephole
                    new Parser.Mul(
                            new Parser.Mul(new Parser.Func("ln", List.of(new Parser.Val(val))), derive(rhs, v)),
                            new Parser.Pow(new Parser.Val(val), rhs)
                    );
            case Parser.Pow(Parser.Expr lhs, Parser.Expr rhs) -> //general
                new Parser.Mul(
                        new Parser.Pow(new Parser.Ident("e"), new Parser.Mul(rhs, new Parser.Func("ln", List.of(lhs)))),
                        derive(new Parser.Mul(rhs, new Parser.Func("ln", List.of(lhs))), v)
                );

            case Parser.Func(var name, var args) -> chainRule(name, args, v);

            case Parser.Ident(var ident) when ident.equals(v) -> new Parser.Val(1.0);
            case Parser.Neg(var rhs) -> new Parser.Neg(derive(rhs, v));
            case Parser.Ident _, Parser.Val _ -> new Parser.Val(0.0);
        };
    }

    private static Parser.Expr chainRule(String func, List<Parser.Expr> args, String v){
        Parser.Expr res = switch (func){
            case "sin" ->  new Parser.Func("cos", args);
            case "cos" ->  new Parser.Neg(new Parser.Func("cos", args));
            case "ln" ->  new Parser.Div(new Parser.Val(1), args.getFirst());
            default -> throw new RuntimeException("Unknown function " + func);
        };
        var der = derive(args.getFirst(), v);
        return new Parser.Mul(res, der);
    }

    public static String toString(Parser.AST ast){
        return ast.name() + "(" + String.join(", ", ast.args()) + ")=" + toString(ast.expr());
    }

    public static String toString(Parser.Expr expr){
        return switch (expr){
            case Parser.Add(var lhs, var rhs) -> toString(lhs) + "+" + toString(rhs);
            case Parser.Sub(var lhs, var rhs) -> toString(lhs) + "-" + toString(rhs);
            case Parser.Mul(var lhs, var rhs) -> conditionalParens(lhs, add_sub) + "*" + conditionalParens(rhs, add_sub);
            case Parser.Div(var lhs, var rhs) -> conditionalParens(lhs, add_sub) + "/" + conditionalParens(rhs, add_sub);
            case Parser.Pow(var lhs, var rhs) ->conditionalParens(lhs, add_sub_mul_div_pow) + "^" + conditionalParens(rhs, add_sub_mul_div_pow);
            case Parser.Func(var name, var args) -> name + "(" + String.join(", ", args.stream().map(Transformer::toString).toList()) + ")";
            case Parser.Neg(var lhs) -> "-" + conditionalParensInv(lhs, Parser.Val.class, Lexer.Ident.class, Parser.Func.class, Parser.Pow.class);
            case Parser.Ident(var ident) -> ident;
            case Parser.Val(var val) -> formatDouble(val);
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

    private static final Class<?>[] add_sub = new Class[]{Parser.Add.class, Parser.Sub.class};
    private static final Class<?>[] add_sub_mul_div_pow = new Class[]{Parser.Add.class, Parser.Sub.class, Parser.Mul.class, Parser.Sub.class, Parser.Pow.class};
    private static String conditionalParens(Parser.Expr expr, Class<?>... requires){
        for(var clazz : requires)
            if(clazz.isInstance(expr))
                return "(" + toString(expr) + ")";
        return toString(expr);
    }
    private static String conditionalParensInv(Parser.Expr expr, Class<?>... requires){
        for(var clazz : requires)
            if(clazz.isInstance(expr))
                return toString(expr);
        return "(" + toString(expr) + ")";
    }
}
