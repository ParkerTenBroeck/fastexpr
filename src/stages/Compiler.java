package stages;

public class Compiler {
    public static CodeGen.Result compile(String expr) throws Parser.ParserException, CodeGen.CodeGenException {
        var ast = Parser.parse(new Lexer(expr));
        Opt.run(ast);
        return CodeGen.run(ast);
    }
}
