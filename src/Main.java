import stages.CodeGen;
import stages.Compiler;

public static void main() throws Exception {

    System.out.println(Compiler.compile("f(x) = x*x^2 + sin(x)"));
    switch(Compiler.compile("f()=pi^e+sin(pi)")){
        case CodeGen.F0(var f) -> System.out.println("Result: " + f.eval());
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x) = x*x")){
        case CodeGen.F1(var f) -> System.out.println("Result: " + f.eval(6));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y) = x*y+y")){
        case CodeGen.F2(var f) -> System.out.println("Result: " + f.eval(6, 8));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y,z) = x*y+y+z*z")){
        case CodeGen.F3(var f) -> System.out.println("Result: " + f.eval(6, 8,2));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y,z,w) = (x*y+y+z*z)/w")){
        case CodeGen.F4(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y,z,w, u) = (x*y+y+z*z)/(w*u)")){
        case CodeGen.F5(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y,z,w, u,v) = v-(x*y+y+z*z)/(w*u)")){
        case CodeGen.F6(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(Compiler.compile("f(x,y,z,w, u,v, othersIg) = v-(x*y+y+z*z)/(w*u)+othersIg*120e-12")){
        case CodeGen.FN(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10, 3923423));
        case CodeGen.Result c -> System.out.println("Expected only single argument got: " + c);
    }
}