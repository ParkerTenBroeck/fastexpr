import util.Peekable;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;

public class Compiler {

    public sealed interface CompResult {}
    public record F0(Func.Func0 f) implements CompResult {}
    public record F1(Func.Func1 f) implements CompResult {}
    public record F2(Func.Func2 f) implements CompResult {}
    public record F3(Func.Func3 f) implements CompResult {}
    public record F4(Func.Func4 f) implements CompResult {}
    public record F5(Func.Func5 f) implements CompResult {}
    public record F6(Func.Func6 f) implements CompResult {}
    public record FN(Func.FuncN f) implements CompResult {}


    private Peekable<Lexer.Token> stream;
    private String funcIdent;
    private ArrayList<String> argList;

    public Compiler() {
    }

    private void expectPunk(Lexer.Punc punc) throws Exception {
        switch(stream.next()){
            case Lexer.Punc p when p.equals(punc) -> {}
            case Lexer.Token t -> throw new Exception("Expected "+punc+" Found: " + t);
        }
    }
    private boolean consumeIfPunk(Lexer.Punc punc) {
        if(stream.peek().equals(punc)){
            stream.next();
            return true;
        }
        return false;
    }

    private String expectIdent() throws Exception {
        return switch(stream.next()){
            case Lexer.Ident(var i) -> i;
            case Lexer.Token t -> throw new Exception("Expected Ident Found: " + t);
        };
    }

    static final Class<?>[] interfaces = new Class[]{Func.Func0.class, Func.Func1.class, Func.Func2.class, Func.Func3.class, Func.Func4.class, Func.Func5.class, Func.Func6.class, Func.FuncN.class};

    public CompResult compile(String expr) throws Exception{
        this.stream = new Peekable<>(new Lexer(expr));
        funcIdent = expectIdent();
        expectPunk(Lexer.Punc.LPar);
        argList = new ArrayList<>();
        while(!stream.peek().equals(Lexer.Punc.RPar)){
            argList.add(expectIdent());
            if(!consumeIfPunk(Lexer.Punc.Comma))break;
        }
        expectPunk(Lexer.Punc.RPar);
        expectPunk(Lexer.Punc.Equals);


        ClassDesc CD_Hello = ClassDesc.of(funcIdent);
        var iface = interfaces[Math.min(argList.size(), interfaces.length-1)];
        ClassDesc CD_Caller = ClassDesc.ofDescriptor(iface.descriptorString());

        var params = iface.getDeclaredMethods()[0].getParameterTypes();
        var cd_params = new ClassDesc[params.length];
        for(int i = 0; i < params.length; i ++){
            cd_params[i] = ClassDesc.ofDescriptor(params[i].descriptorString());
        }
        MethodTypeDesc MTD_interface_method = MethodTypeDesc.of(ClassDesc.ofDescriptor(double.class.descriptorString()), cd_params);
        MethodTypeDesc MTD_String = MethodTypeDesc.of(ClassDesc.ofDescriptor(String.class.descriptorString()));
        byte[] bytes = ClassFile.of().build(CD_Hello,
                clb -> clb.withFlags(ClassFile.ACC_PUBLIC)
                        .withInterfaces(clb.constantPool().classEntry(CD_Caller))
                        .withMethod(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void,
                                ClassFile.ACC_PUBLIC,
                                mb -> mb.withCode(
                                        cob -> cob.aload(0)
                                                .invokespecial(ConstantDescs.CD_Object,
                                                        ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
                                                .return_()))
                        .withMethod("eval", MTD_interface_method, ClassFile.ACC_PUBLIC,
                                mb -> mb.withCode(
                                        cb -> compile_(cb, clb).dreturn()
                                )
                        ).withMethod("toString", MTD_String, ClassFile.ACC_PUBLIC, mb -> mb.withCode(cb ->  cb.ldc(clb.constantPool().stringEntry(expr)).areturn()))
        );
        var clazz = new ClassLoader(){
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if(name.equals(funcIdent))
                    return defineClass(name, bytes, 0, bytes.length);
                return Compiler.class.getClassLoader().loadClass(name);
            }
        }.loadClass(funcIdent);
        var instance = clazz.getConstructor().newInstance();

        return switch(argList.size()){
            case 0 -> new F0((Func.Func0) instance);
            case 1 -> new F1((Func.Func1) instance);
            case 2 -> new F2((Func.Func2) instance);
            case 3 -> new F3((Func.Func3) instance);
            case 4 -> new F4((Func.Func4) instance);
            case 5 -> new F5((Func.Func5) instance);
            case 6 -> new F6((Func.Func6) instance);
            default -> new FN((Func.FuncN) instance);
        };
    }

    private void loadVar(CodeBuilder cb, String ident){
        if(argList.size()<=6){
            cb.dload(argList.indexOf(ident)*2+1);
        }else{
            cb.aload(1).loadConstant(argList.indexOf(ident)).daload();
        }
    }

    public CodeBuilder compile_(CodeBuilder cob, ClassBuilder clb){
        return this.compile_1(cob, clb);
    }

    CodeBuilder compile_1(CodeBuilder cob, ClassBuilder clb){
        this.compile_2(cob, clb);
        while(true){
            switch(stream.peek()){
                case Lexer.Punc.Add -> {
                    stream.next();
                    this.compile_2(cob, clb).dadd();
                }
                case Lexer.Punc.Sub -> {
                    stream.next();
                    this.compile_2(cob, clb).dsub();
                }
                case null, default -> {return cob;}
            }
        }
    }

    CodeBuilder compile_2(CodeBuilder cob, ClassBuilder clb){
       this.compile_3(cob, clb);
        while(true){
            switch(stream.peek()){
                case Lexer.Punc.Mul -> {
                    stream.next();
                    this.compile_3(cob, clb).dmul();
                }
                case Lexer.Punc.Div -> {
                    stream.next();
                    this.compile_3(cob, clb).ddiv();
                }
                case null, default -> {return cob;}
            }
        }
    }

    CodeBuilder compile_3(CodeBuilder cob, ClassBuilder clb){
        switch(stream.next()){
            case Lexer.Punc.LPar -> {
                this.compile_(cob, clb);
                if (stream.next() != Lexer.Punc.RPar) throw new RuntimeException("Unclosed brackets");
            }
            case Lexer.Punc.Sub -> {
                this.compile_3(cob, clb);
                cob.dneg();
            }
            case Lexer.Numeric(double val) -> {
                cob.loadConstant(val);
            }
            case Lexer.Ident(var val) -> {
                loadVar(cob, val);
            }
            case Lexer.Token t -> throw new RuntimeException("Invalid token found: " + t);
            case null -> throw new RuntimeException("Expected Token found None");
        }
        return cob;
    }
}
