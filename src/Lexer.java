
import util.Iterator;


public class Lexer implements Iterator<Lexer.Token> {
    String input;
    int curr;
    int last;

    enum State{
        Default,
        Numeric,
        NumericE,
        Ident,
    }

    public Lexer(String input){
        this.input = input;
        this.curr = 0;
        this.last = 0;
    }

    public sealed interface Token{}
    public enum Punc implements Token{
        LPar,
        RPar,
        Add,
        Sub,
        Div,
        Mul,
        Equals, Comma
    }

    public record Ident(String ident) implements Token{}
    public record Numeric(double val) implements Token{}

    @Override
    public Token next() {
        State state = State.Default;
        this.last = this.curr;
        while(true){
            char next;
            if (this.input.length() > this.curr) {
                next = this.input.charAt(this.curr);
                curr++;
            }else{
                curr = this.input.length()+1;
                next = 0;
            }

            switch (state){
                case Default -> {
                    if (this.input.length() == this.curr-1)
                        return null;
                    switch (next){
                        case '(':return Punc.LPar;
                        case ')':return Punc.RPar;
                        case '+':return Punc.Add;
                        case '-':return Punc.Sub;
                        case '/':return Punc.Div;
                        case '*':return Punc.Mul;
                        case ',':return Punc.Comma;
                        case '=':return Punc.Equals;
                        default: {
                            if ('0' <= next && next <= '9'){
                                state = State.Numeric;
                            }else if(Character.isWhitespace(next)){
                                this.last = this.curr;
                            }else if(Character.isAlphabetic(next)){
                                state = State.Ident;
                            }else{
                                this.last = this.curr;
                                throw new RuntimeException("Invalid char " + next);
                            }
                        }
                    }
                }
                case Ident -> {
                    if (Character.isLetter(next) || Character.isLetterOrDigit(next) || next == '_'){
                        continue;
                    }
                    this.curr--;
                    return new Ident(this.input.substring(this.last, this.curr));
                }
                case NumericE -> state = State.Numeric;
                case Numeric -> {
                    if (('0' <= next && next <= '9') || next == '_' || next == '.'){
                        continue;
                    }
                    if(next == 'e' || next == 'E'){
                        state = State.NumericE;
                        continue;
                    }
                    this.curr--;
                    return new Numeric(Double.parseDouble(this.input.substring(this.last, this.curr).replace("_", "")));
                }
            }
        }
    }
}
