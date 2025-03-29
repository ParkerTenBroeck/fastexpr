public class Func {
    @FunctionalInterface
    public interface Func0{
        double eval();
    }
    @FunctionalInterface
    public interface Func1{
        double eval(double x);
    }
    @FunctionalInterface
    public interface Func2{
        double eval(double x, double y);
    }
    @FunctionalInterface
    public interface Func3{
        double eval(double x, double y, double z);
    }
    @FunctionalInterface
    public interface Func4{
        double eval(double x, double y, double z, double w);
    }
    @FunctionalInterface
    public interface Func5{
        double eval(double x, double y, double z, double w, double u);
    }
    @FunctionalInterface
    public interface Func6{
        double eval(double x, double y, double z, double w, double u, double v);
    }
    @FunctionalInterface
    public interface FuncN{
        double eval(double... args);
    }
}
