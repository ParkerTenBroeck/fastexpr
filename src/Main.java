import fastexpr.ast.AST;
import fastexpr.func.*;
import fastexpr.stages.*;

public static void main() throws Exception {


    var expr = "f(x) = 1/(1+e^x)";
    var meow = AST.parse(expr).derivative("x").opt().compile();
//    System.out.println();
    System.out.println(meow);
    if(true)return;

    System.out.println(Compiler.compile("f(x) = x^3"));
    for(int i = 0; i < 50; i ++){
        switch(Compiler.compile("fff(x) = x^"+i+".2")){
            case Func1 f -> System.out.println("Result: " + i + " " + f.eval(2));
            case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
        }
    }

    switch(Compiler.compile("f0()=pi^e+sin(pi)")){
        case Func0 f -> System.out.println("Result: " + f.eval());
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f1(x) = x^1")){
        case Func1 f -> System.out.println("Result: " + f.eval(2));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f2(x,y) = x*y+y")){
        case Func2 f -> System.out.println("Result: " + f.eval(6, 8));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f3(x,y,z) = x*y+y+z*z")){
        case Func3 f -> System.out.println("Result: " + f.eval(6, 8,2));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f4(x,y,z,w) = (x*y+y+z*z)/w")){
        case Func4 f -> System.out.println("Result: " + f.eval(6, 8,2, 50));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f5(x,y,z,w, u) = (x*y+y+z*z)/(w*u)")){
        case Func5 f -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f6(x,y,z,w, u,v) = v-(x*y+y+z*z)/(w*u)")){
        case Func6 f -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f7(x,y,z,w, u,v, othersIg) = v-(x*y+y+ln(z)^2*sin(z*pi/2))/(w*u)+sqrt(othersIg*120e-12)")){
        case FuncN f -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10, 3923423));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }

    switch(Compiler.compile("g(x,y,z,w, u,v) = v-(x*y+y+ln(z)^2*sin(z*pi/2))/(w*u)+sqrt(v*120e-12/12/u)")){
        case FuncN f -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case ASTFunc c -> System.out.println("Expected only single argument got: " + c);
    }
}