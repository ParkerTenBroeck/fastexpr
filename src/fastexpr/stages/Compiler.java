package fastexpr.stages;

import fastexpr.ast.AST;
import fastexpr.func.ASTFunc;

public class Compiler {
    public static ASTFunc compile(String expr) throws Parser.ParserException, CodeGen.CodeGenException {
        return compile(Parser.parse(new Lexer(expr)));
    }

    public static ASTFunc compile(AST ast) throws CodeGen.CodeGenException {
        return CodeGen.run(Opt.run(ast));
    }
}
