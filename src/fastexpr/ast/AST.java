package fastexpr.ast;

import fastexpr.func.ASTFunc;
import fastexpr.stages.*;

import java.util.List;

public record AST(
        String name,
        List<String> args,
        Expr expr
) {
    public static AST parse(String expr) throws Parser.ParserException {
        return Parser.parse(new Lexer(expr));
    }

    @Override
    public String toString() {
        return Transformer.toString(this);
    }

    public AST derivative(String x){
        return Transformer.derive(this, x);
    }

    public AST opt(){
        return Opt.run(this);
    }

    public ASTFunc compile() throws CodeGen.CodeGenException {
        return Compiler.compile(this);
    }
}
