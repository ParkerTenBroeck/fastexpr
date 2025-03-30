import fastexpr.stages.*;

public static void main() throws Exception {
//    System.out.println(Parser.parse(new Lexer("f(x) = x^3")));
//    System.out.println(Transformer.derive(Parser.parse(new Lexer("f(x) = x^3")), "x"));
//
//    System.out.println(Parser.parse(new Lexer("f(x) = x^3+x^2+x/2-512+sin(x)")));
//    System.out.println(Opt.run(Transformer.derive(Parser.parse(new Lexer("f(x) = x^3+x^2+x/2-512+sin(x)")), "x")));
    var func = Parser.parse(new Lexer("f(x) = 1/(1+e^x)"));
    System.out.println(func);
    System.out.println(Opt.run(Transformer.derive(func, "x")));
    if(true)return;

    System.out.println(Compiler.compile("f(x) = x^3"));
    for(int i = 0; i < 50; i ++){
        switch(Compiler.compile("fff(x) = x^"+i+".2")){
            case CodeGen.F1(var f) -> System.out.println("Result: " + i + " " + f.eval(2));
            case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
        }
    }

    switch(Compiler.compile("f0()=pi^e+sin(pi)")){
        case CodeGen.F0(var f) -> System.out.println("Result: " + f.eval());
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f1(x) = x^1")){
        case CodeGen.F1(var f) -> System.out.println("Result: " + f.eval(2));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f2(x,y) = x*y+y")){
        case CodeGen.F2(var f) -> System.out.println("Result: " + f.eval(6, 8));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f3(x,y,z) = x*y+y+z*z")){
        case CodeGen.F3(var f) -> System.out.println("Result: " + f.eval(6, 8,2));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f4(x,y,z,w) = (x*y+y+z*z)/w")){
        case CodeGen.F4(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f5(x,y,z,w, u) = (x*y+y+z*z)/(w*u)")){
        case CodeGen.F5(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f6(x,y,z,w, u,v) = v-(x*y+y+z*z)/(w*u)")){
        case CodeGen.F6(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f7(x,y,z,w, u,v, othersIg) = v-(x*y+y+ln(z)^2*sin(z*pi/2))/(w*u)+sqrt(othersIg*120e-12)")){
        case CodeGen.FN(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10, 3923423));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }

    switch(Compiler.compile("g(x,y,z,w, u,v) = v-(x*y+y+ln(z)^2*sin(z*pi/2))/(w*u)+sqrt(v*120e-12/12/u)")){
        case CodeGen.F6(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
}