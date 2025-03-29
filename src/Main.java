public static void main() throws Exception {
    switch(new Compiler().compile("f()=12+44-3")){
        case Compiler.F0(var f) -> System.out.println("Result: " + f.eval());
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x) = x*x")){
        case Compiler.F1(var f) -> System.out.println("Result: " + f.eval(6));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y) = x*y+y")){
        case Compiler.F2(var f) -> System.out.println("Result: " + f.eval(6, 8));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y,z) = x*y+y+z*z")){
        case Compiler.F3(var f) -> System.out.println("Result: " + f.eval(6, 8,2));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y,z,w) = (x*y+y+z*z)/w")){
        case Compiler.F4(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y,z,w, u) = (x*y+y+z*z)/(w*u)")){
        case Compiler.F5(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y,z,w, u,v) = v-(x*y+y+z*z)/(w*u)")){
        case Compiler.F6(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
    switch(new Compiler().compile("f(x,y,z,w, u,v, othersIg) = v-(x*y+y+z*z)/(w*u)+othersIg*120e-12")){
        case Compiler.FN(var f) -> System.out.println("Result: " + f.eval(6, 8,2, 50, 0.2, 10, 3923423));
        case Compiler.CompResult c -> System.out.println("Expected only single argument got: " + c);
    }
}