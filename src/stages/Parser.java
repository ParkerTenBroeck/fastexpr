package stages;

import util.Iterator;
import util.Peekable;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    public record AST(
       String name,
       List<String> args,
       Expr expr
    ){
        @Override
        public String toString() {
            return Transformer.toString(this);
        }
    }
    public sealed interface Expr{}
    public record Ident(String name) implements Expr{}
    public record Val(double value) implements Expr{}
    public sealed interface BinOp extends Expr{
        Expr lhs();
        Expr rhs();
    }
    public record Add(Expr lhs, Expr rhs) implements BinOp{}
    public record Sub(Expr lhs, Expr rhs) implements BinOp{}
    public record Mul(Expr lhs, Expr rhs) implements BinOp{}
    public record Div(Expr lhs, Expr rhs) implements BinOp{}
    public record Pow(Expr lhs, Expr rhs) implements BinOp{}
    public sealed interface UnOp extends Expr{
        Expr expr();
    }
    public record Neg(Expr expr) implements UnOp{}
    public record Func(String name, List<Expr> args) implements Expr{}

    public static class ParserException extends Exception{
        public ParserException(String s) {
            super(s);
        }
    }


    public Parser(Iterator<Lexer.Token> stream) {
        this.stream = new Peekable<>(stream);
    }

    private final Peekable<Lexer.Token> stream;

    private void expectPunk(Lexer.Punc punc) throws ParserException {
        switch(stream.next()){
            case Lexer.Punc p when p.equals(punc) -> {}
            case Lexer.Token t -> throw new ParserException("Expected "+punc+" Found: " + t);
        }
    }

    private boolean consumeIfPunk(Lexer.Punc punc) {
        if(punc.equals(stream.peek())){
            stream.next();
            return true;
        }
        return false;
    }

    private String expectIdent() throws ParserException {
        return switch(stream.next()){
            case Lexer.Ident(var i) -> i;
            case Lexer.Token t -> throw new ParserException("Expected Ident Found: " + t);
        };
    }

    public static AST parse(Iterator<Lexer.Token> stream) throws ParserException {
        return new Parser(stream).run();
    }

    private AST run() throws ParserException {
        var funcIdent = expectIdent();
        var argList = list(Lexer.Punc.LPar, Lexer.Punc.RPar, Lexer.Punc.Comma, this::expectIdent);
        expectPunk(Lexer.Punc.Equals);
        var res = new AST(
                funcIdent,
                argList,
                parse_()
        );
        if(stream.peek()!=null)
            throw new ParserException("Expected end of input found: " + stream.next());
        return res;
    }

    private interface Supplier<T>{
        T get() throws ParserException;
    }

    private <T> List<T> list(Lexer.Punc start, Lexer.Punc end, Lexer.Punc sep, Supplier<T> supply) throws ParserException {
        expectPunk(start);
        var list = new ArrayList<T>();
        while(!stream.peek().equals(end)){
            list.add(supply.get());
            if(!consumeIfPunk(sep))break;
        }
        expectPunk(end);
        return list;
    }

    private Expr parse_() throws ParserException {
        return this.parse_1();
    }

    private Expr parse_1() throws ParserException {
        var lhs = this.parse_2();
        while(true){
            switch(stream.peek()){
                case Lexer.Punc.Add -> {
                    stream.next();
                    lhs = new Add(lhs, this.parse_2());
                }
                case Lexer.Punc.Sub -> {
                    stream.next();
                    lhs = new Sub(lhs, this.parse_2());
                }
                case null, default -> {return lhs;}
            }
        }
    }

    private Expr parse_2() throws ParserException {
       var lhs = this.parse_3();
        while(true){
            switch(stream.peek()){
                case Lexer.Punc.Mul -> {
                    stream.next();
                    lhs = new Mul(lhs, this.parse_3());
                }
                case Lexer.Punc.Div -> {
                    stream.next();
                    lhs = new Div(lhs, this.parse_3());
                }
                case null, default -> {return lhs;}
            }
        }
    }

    private Expr parse_3() throws ParserException {
        var lhs = this.parse_4();
        while(true){
            switch(stream.peek()){
                case Lexer.Punc.Carrot -> {
                    stream.next();
                    lhs = new Pow(lhs, this.parse_4());
                }
                case null, default -> {return lhs;}
            }
        }
    }

    private Expr parse_4() throws ParserException {
        return switch(stream.next()){
            case Lexer.Punc.LPar -> {
                var expr = this.parse_();
                if (stream.next() != Lexer.Punc.RPar) throw new ParserException("Unclosed brackets");
                yield expr;
            }
            case Lexer.Punc.Sub -> new Neg(this.parse_4());
            case Lexer.Numeric(double val) -> new Val(val);
            case Lexer.Ident(var val) -> {
                if(Lexer.Punc.LPar.equals(stream.peek()))
                    yield new Func(val, list(Lexer.Punc.LPar, Lexer.Punc.RPar, Lexer.Punc.Comma, this::parse_));
                else
                    yield new Ident(val);
            }
            case Lexer.Token t -> throw new ParserException("Invalid token found: " + t);
            case null -> throw new ParserException("Expected Token found None");
        };
    }
}
