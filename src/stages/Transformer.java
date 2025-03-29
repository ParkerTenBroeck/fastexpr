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

            case Parser.Mul(var lhs, var rhs) ->
                new Parser.Add(new Parser.Mul(derive(lhs, v), rhs), new Parser.Mul(lhs, derive(rhs, v)));

            case Parser.Div(var lhs, var rhs) -> new Parser.Div(
                    new Parser.Sub(new Parser.Mul(derive(lhs, v), rhs), new Parser.Mul(lhs, derive(rhs, v))),
                    new Parser.Pow(rhs, new Parser.Val(2))
            );
            
            case Parser.Pow(Parser.Expr lhs, Parser.Expr rhs) ->
                new Parser.Mul(
                        new Parser.Pow(new Parser.Ident("e"), new Parser.Mul(rhs, new Parser.Func("ln", List.of(lhs)))),
                        derive(new Parser.Mul(rhs, new Parser.Func("ln", List.of(lhs))), v)
                );

            case Parser.Func(var name, var args) when name.equals("sin") ->
                new Parser.Mul(derive(args.getFirst(), v), new Parser.Func("cos", List.copyOf(args)));
            case Parser.Func(var name, var args) when name.equals("cos") ->
                    new Parser.Neg(new Parser.Mul(derive(args.getFirst(), v), new Parser.Func("sin", List.copyOf(args))));
            case Parser.Func(var name, var args) when name.equals("ln") ->
                    new Parser.Div(derive(args.getFirst(), v), args.getFirst());

            case Parser.Func(var name, var _) -> throw new RuntimeException("Unknown function " + name);

            case Parser.Ident(var ident) when ident.equals(v) -> new Parser.Val(1.0);
            case Parser.Ident(var ident) -> new Parser.Ident(ident);
            case Parser.Neg(var rhs) -> new Parser.Neg(derive(rhs, v));
            case Parser.Val _ -> new Parser.Val(0.0);
        };
    }

    public static String toString(Parser.AST ast){
        return ast.name() + "(" + String.join(", ", ast.args()) + ")=" + toString(ast.expr());
    }

    public static String toString(Parser.Expr expr){
        return switch (expr){
            case Parser.Add(var lhs, var rhs) -> "(" + toString(lhs) + ")+(" + toString(rhs) + ")";
            case Parser.Sub(var lhs, var rhs) -> "(" + toString(lhs) + ")-(" + toString(rhs) + ")";
            case Parser.Mul(var lhs, var rhs) -> "(" + toString(lhs) + ")*(" + toString(rhs) + ")";
            case Parser.Div(var lhs, var rhs) -> "(" + toString(lhs) + ")/(" + toString(rhs) + ")";
            case Parser.Pow(var lhs, var rhs) -> "(" + toString(lhs) + ")^(" + toString(rhs) + ")";
            case Parser.Func(var name, var args) -> name + "(" + String.join(", ", args.stream().map(Transformer::toString).toList()) + ")";
            case Parser.Ident(var ident) -> ident;
            case Parser.Neg(var lhs) -> "-(" + toString(lhs) + ")";
            case Parser.Val(var val) -> val+"";
        };
    }
}
