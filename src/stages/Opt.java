package stages;

public class Opt {
    public static Parser.AST run(Parser.AST ast) {
        for(int i = 0; i < 100; i ++){
            var opt_ast = opt(ast);
            if(opt_ast.equals(ast)) return opt_ast;
            ast = opt_ast;
        }
        return ast;
    }

    private static Parser.AST opt(Parser.AST ast){
        return new Parser.AST(
                ast.name(),
                ast.args(),
                opt(ast.expr())
        );
    }

    private static Parser.Expr opt(Parser.Expr expr) {
        expr = switch(expr){
            case Parser.Add(var lhs, var rhs) -> new Parser.Add(opt(lhs), opt(rhs));
            case Parser.Sub(var lhs, var rhs) -> new Parser.Sub(opt(lhs), opt(rhs));
            case Parser.Mul(var lhs, var rhs) -> new Parser.Mul(opt(lhs), opt(rhs));
            case Parser.Div(var lhs, var rhs) -> new Parser.Div(opt(lhs), opt(rhs));
            case Parser.Pow(var lhs, var rhs) -> new Parser.Pow(opt(lhs), opt(rhs));
            case Parser.Neg(var lhs) -> new Parser.Neg(opt(lhs));
            case Parser.Func(var name, var args) -> new Parser.Func(name, args.stream().map(Opt::opt).toList());
            case Parser.Ident(var ident) when ident.equalsIgnoreCase("pi") -> new Parser.Val(Math.PI);
            case Parser.Ident(var ident) when ident.equals("e") -> new Parser.Val(Math.E);
            case Parser.Ident ident -> ident;
            case Parser.Val val -> val;
        };

        return switch(expr){
            case Parser.Add(Parser.Val(var lhs), Parser.Val(var rhs)) -> new Parser.Val(lhs+rhs);
            case Parser.Sub(Parser.Val(var lhs), Parser.Val(var rhs)) -> new Parser.Val(lhs-rhs);
            case Parser.Mul(Parser.Val(var lhs), Parser.Val(var rhs)) -> new Parser.Val(lhs*rhs);
            case Parser.Div(Parser.Val(var lhs), Parser.Val(var rhs)) -> new Parser.Val(lhs/rhs);
            case Parser.Pow(Parser.Val(var lhs), Parser.Val(var rhs)) -> new Parser.Val(Math.pow(lhs,rhs));
            case Parser.Neg(Parser.Val(var lhs)) -> new Parser.Val(-lhs);
            case Parser.Func(var name, var args) when name.equals("sin") && args.getFirst() instanceof Parser.Val(var val) -> new Parser.Val(Math.sin(val));
            case Parser.Func(var name, var args) when name.equals("cos") && args.getFirst() instanceof Parser.Val(var val) -> new Parser.Val(Math.cos(val));
            case Parser.Func(var name, var args) when name.equals("ln") && args.getFirst() instanceof Parser.Val(var val) -> new Parser.Val(Math.log(val));


            case Parser.Add(Parser.Neg(var lhs), Parser.Neg(var rhs)) -> new Parser.Neg(new Parser.Add(lhs, rhs));
            case Parser.Sub(Parser.Neg(var lhs), Parser.Neg(var rhs)) -> new Parser.Neg(new Parser.Sub(lhs, rhs));
            case Parser.Add(var lhs, Parser.Neg(var rhs)) -> new Parser.Sub(lhs, rhs);
            case Parser.Sub(var lhs, Parser.Neg(var rhs)) -> new Parser.Add(lhs, rhs);
            case Parser.Add(Parser.Neg(var lhs), var rhs) -> new Parser.Sub(rhs, lhs);
            case Parser.Sub(Parser.Neg(var lhs), var rhs) -> new Parser.Neg(new Parser.Add(lhs, rhs));

            case Parser.Sub(var lhs, var rhs) when lhs.equals(rhs) -> new Parser.Val(0);
            case Parser.Div(var lhs, var rhs) when lhs.equals(rhs) -> new Parser.Val(1);

            case Parser.Mul(var lhs, Parser.Div(Parser.Val(int one), var rhs)) when one==1 -> new Parser.Div(lhs, rhs);
            case Parser.Mul(Parser.Div(Parser.Val(int one), var rhs), var lhs) when one==1 -> new Parser.Div(lhs, rhs);

            case Parser.Add(var lhs, Parser.Val(int zero)) when zero==0 -> lhs;
            case Parser.Add(Parser.Val(int zero), var rhs) when zero==0 -> rhs;
            case Parser.Sub(var lhs, Parser.Val(int zero)) when zero==0 -> lhs;
            case Parser.Sub(Parser.Val(int zero), var rhs) when zero==0 -> new Parser.Neg(rhs);

            case Parser.Mul(Parser.Val(int zero), var _) when zero==0 -> new Parser.Val(0.0);
            case Parser.Mul(Parser.Val(int one), var rhs) when one==1 -> rhs;
            case Parser.Mul(Parser.Val(int one), var rhs) when one==-1 -> new Parser.Neg(rhs);
            case Parser.Mul(Parser.Val(var v1), Parser.Mul(Parser.Val(var v2), var rhs)) -> new Parser.Mul(new Parser.Val(v1*v2), rhs);
            case Parser.Mul(Parser.Val(var v1), Parser.Mul(var rhs, Parser.Val(var v2))) -> new Parser.Mul(new Parser.Val(v1*v2), rhs);

            case Parser.Div(var lhs, Parser.Val(int one)) when one==1 -> lhs;
            case Parser.Div(Parser.Val(int zero), var _) when zero==0 -> new Parser.Val(0.0);

            case Parser.Neg(Parser.Neg(var inner)) -> inner;
            case Parser.Neg(Parser.Mul(Parser.Val(var val), var rhs)) -> new Parser.Mul(new Parser.Val(-val), rhs);
            case Parser.Neg(Parser.Div(Parser.Val(var val), var rhs)) -> new Parser.Mul(new Parser.Val(-val), rhs);
            case Parser.Neg(Parser.Div(var lhs, Parser.Val(var val))) -> new Parser.Mul(lhs, new Parser.Val(-val));

            // move constant to front of mult
            case Parser.Mul(var lhs, Parser.Val(var rhs)) -> new Parser.Mul(new Parser.Val(rhs), lhs);
            // move constant to back of add
            case Parser.Add(Parser.Val(var val), var rhs) -> new Parser.Add(rhs, new Parser.Val(val));

            default -> expr;
        };
    }
}
