package stages;

public class Compiler {
    public static CodeGen.Result compile(String expr) throws Parser.ParserException, CodeGen.CodeGenException {
        return compile(Parser.parse(new Lexer(expr)));
    }

    public static CodeGen.Result compile(Parser.AST ast) throws CodeGen.CodeGenException {
        Opt.run(ast);
        return CodeGen.run(ast);
    }
}
