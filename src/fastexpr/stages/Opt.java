package fastexpr.stages;

import fastexpr.ast.*;

public class Opt {
    public static AST run(AST ast) {
        for(int i = 0; i < 100; i ++){
            var opt_ast = opt(ast);
            if(opt_ast.equals(ast)) return opt_ast;
            ast = opt_ast;
        }
        return ast;
    }

    private static AST opt(AST ast){
        return new AST(
                ast.name(),
                ast.args(),
                opt(ast.expr())
        );
    }

    private static Expr opt(Expr expr) {
        expr = switch(expr){
            case Add(var lhs, var rhs) -> new Add(opt(lhs), opt(rhs));
            case Sub(var lhs, var rhs) -> new Sub(opt(lhs), opt(rhs));
            case Mul(var lhs, var rhs) -> new Mul(opt(lhs), opt(rhs));
            case Div(var lhs, var rhs) -> new Div(opt(lhs), opt(rhs));
            case Pow(var lhs, var rhs) -> new Pow(opt(lhs), opt(rhs));
            case Neg(var lhs) -> new Neg(opt(lhs));
            case Func(var name, var args) -> new Func(name, args.stream().map(Opt::opt).toList());
            case Ident(var ident) when ident.equalsIgnoreCase("pi") -> new Val(Math.PI);
            case Ident(var ident) when ident.equals("e") -> new Val(Math.E);
            case Ident ident -> ident;
            case Val val -> val;
        };

        return switch(expr){
            case Add(Val(var lhs), Val(var rhs)) -> new Val(lhs+rhs);
            case Sub(Val(var lhs), Val(var rhs)) -> new Val(lhs-rhs);
            case Mul(Val(var lhs), Val(var rhs)) -> new Val(lhs*rhs);
            case Div(Val(var lhs), Val(var rhs)) -> new Val(lhs/rhs);
            case Pow(Val(var lhs), Val(var rhs)) -> new Val(Math.pow(lhs,rhs));
            case Neg(Val(var lhs)) -> new Val(-lhs);
            case Func(var name, var args) when name.equals("sin") && args.getFirst() instanceof Val(var val) -> new Val(Math.sin(val));
            case Func(var name, var args) when name.equals("cos") && args.getFirst() instanceof Val(var val) -> new Val(Math.cos(val));
            case Func(var name, var args) when name.equals("ln") && args.getFirst() instanceof Val(var val) -> new Val(Math.log(val));


            case Add(Neg(var lhs), Neg(var rhs)) -> new Neg(new Add(lhs, rhs));
            case Sub(Neg(var lhs), Neg(var rhs)) -> new Neg(new Sub(lhs, rhs));
            case Add(var lhs, Neg(var rhs)) -> new Sub(lhs, rhs);
            case Sub(var lhs, Neg(var rhs)) -> new Add(lhs, rhs);
            case Add(Neg(var lhs), var rhs) -> new Sub(rhs, lhs);
            case Sub(Neg(var lhs), var rhs) -> new Neg(new Add(lhs, rhs));

            case Sub(var lhs, var rhs) when lhs.equals(rhs) -> new Val(0);
            case Div(var lhs, var rhs) when lhs.equals(rhs) -> new Val(1);

            case Mul(var lhs, Div(Val(int one), var rhs)) when one==1 -> new Div(lhs, rhs);
            case Mul(Div(Val(int one), var rhs), var lhs) when one==1 -> new Div(lhs, rhs);

            case Add(var lhs, Val(int zero)) when zero==0 -> lhs;
            case Add(Val(int zero), var rhs) when zero==0 -> rhs;
            case Sub(var lhs, Val(int zero)) when zero==0 -> lhs;
            case Sub(Val(int zero), var rhs) when zero==0 -> new Neg(rhs);

            case Mul(Val(int zero), var _) when zero==0 -> new Val(0.0);
            case Mul(Val(int one), var rhs) when one==1 -> rhs;
            case Mul(Val(int one), var rhs) when one==-1 -> new Neg(rhs);
            case Mul(Val(var v1), Mul(Val(var v2), var rhs)) -> new Mul(new Val(v1*v2), rhs);
            case Mul(Val(var v1), Mul(var rhs, Val(var v2))) -> new Mul(new Val(v1*v2), rhs);

            case Div(var lhs, Val(int one)) when one==1 -> lhs;
            case Div(Val(int zero), var _) when zero==0 -> new Val(0.0);

            case Neg(Neg(var inner)) -> inner;
            case Neg(Mul(Val(var val), var rhs)) -> new Mul(new Val(-val), rhs);
            case Neg(Div(Val(var val), var rhs)) -> new Mul(new Val(-val), rhs);
            case Neg(Div(var lhs, Val(var val))) -> new Mul(lhs, new Val(-val));

            // move constant to front of mult
            case Mul(var lhs, Val(var rhs)) -> new Mul(new Val(rhs), lhs);
            // move constant to back of add
            case Add(Val(var val), var rhs) -> new Add(rhs, new Val(val));

            default -> expr;
        };
    }
}
