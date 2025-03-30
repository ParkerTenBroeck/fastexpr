package fastexpr.stages;

import fastexpr.ast.*;
import fastexpr.func.*;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class CodeGen {

    public static class CodeGenException extends Exception{
        public CodeGenException(String message) {
            super(message);
        }

        public CodeGenException(Exception e) {
            super(e);
        }
    }

    private static class CodeGenRuntimeException extends RuntimeException{
        public CodeGenRuntimeException(CodeGenException e){
            super(e);
        }
    }

    private final AST ast;

    public CodeGen(AST ast) {
        this.ast = ast;
    }

    public static ASTFunc run(AST ast) throws CodeGenException {
        return new CodeGen(ast).run();
    }

    static final Class<?>[] interfaces = new Class[]{Func0.class, Func1.class, Func2.class, Func3.class, Func4.class, Func5.class, Func6.class, FuncN.class};
    static final ClassDesc CD_Math = ClassDesc.ofDescriptor(Math.class.descriptorString());
    static final MethodTypeDesc MD_double = MethodTypeDesc.of(ConstantDescs.CD_double);
    static final MethodTypeDesc MD_double_double = MethodTypeDesc.of(ConstantDescs.CD_double, ConstantDescs.CD_double);
    static final MethodTypeDesc MD_double_double_double = MethodTypeDesc.of(ConstantDescs.CD_double, ConstantDescs.CD_double, ConstantDescs.CD_double);
    static final MethodTypeDesc MD_double_double_double_double = MethodTypeDesc.of(ConstantDescs.CD_double, ConstantDescs.CD_double, ConstantDescs.CD_double, ConstantDescs.CD_double);
    static final MethodTypeDesc[] MD_double_doubleN = new MethodTypeDesc[]{MD_double, MD_double_double, MD_double_double_double, MD_double_double_double_double};

    public ASTFunc run() throws CodeGenException{
        ClassDesc CD_Hello = ClassDesc.of(ast.name());
        var iface = interfaces[Math.min(ast.args().size(), interfaces.length-1)];
        ClassDesc CD_Caller = ClassDesc.ofDescriptor(iface.descriptorString());

        var params = iface.getDeclaredMethods()[0].getParameterTypes();
        var cd_params = new ClassDesc[params.length];
        for(int i = 0; i < params.length; i ++){
            cd_params[i] = ClassDesc.ofDescriptor(params[i].descriptorString());
        }
        MethodTypeDesc MTD_interface_method = MethodTypeDesc.of(ConstantDescs.CD_double, cd_params);
        MethodTypeDesc MTD_String = MethodTypeDesc.of(ConstantDescs.CD_String);

        byte[] bytes;
        try{
            bytes = ClassFile.of().build(CD_Hello,
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
                                    mb -> mb.withCode(cob -> build(cob, clb, false))
                            ).withMethod("eval_s", MTD_interface_method, ClassFile.ACC_PUBLIC + ClassFile.ACC_STATIC,
                                    mb -> mb.withCode(cob -> build(cob, clb, true))
                            ).withMethod("toString", MTD_String, ClassFile.ACC_PUBLIC, mb -> mb.withCode(cb ->  cb.ldc(clb.constantPool().stringEntry(ast.toString())).areturn()))
            );
        }catch (CodeGenRuntimeException e){
            throw (CodeGenException) e.getCause();
        }

        try {
            Files.write(Path.of(ast.name()+".class"), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Object instance;
        try{
            var clazz = new ClassLoader(){
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    if(name.equals(ast.name()))
                        return defineClass(name, bytes, 0, bytes.length);
                    return CodeGen.class.getClassLoader().loadClass(name);
                }
            }.loadClass(ast.name());
            instance = clazz.getConstructor().newInstance();
        }catch (Exception e){
            throw new CodeGenException(e);
        }


        return (ASTFunc) instance;
    }

    private CodeBuilder build(CodeBuilder cob, ClassBuilder clb, boolean isStatic) {
        try {
            return new CodeGenImpl(cob, clb, isStatic).build_(ast.expr()).dreturn();
        } catch (CodeGenException e) {
            throw new CodeGenRuntimeException(e);
        }
    }

    private class CodeGenImpl{
        private final CodeBuilder cob;
        private final ClassBuilder clb;
        private final boolean isStatic;

        private CodeGenImpl(CodeBuilder cob, ClassBuilder clb, boolean isStatic) {
            this.cob = cob;
            this.clb = clb;
            this.isStatic = isStatic;
        }


        private void loadVar(String ident) throws CodeGenException{
            int idx = ast.args().indexOf(ident);
            if(idx==-1) {
                switch (ident) {
                    case "pi", "Pi", "PI" -> cob.loadConstant(Math.PI);
                    case "e" -> cob.loadConstant(Math.E);
                    default -> throw new CodeGenException("Argument " + ident + " is not defined");
                }
                return;
            }
            if(ast.args().size()<=6){
                cob.dload(idx*2+(isStatic?0:1));
            }else{
                cob.aload(isStatic?0:1).loadConstant(idx).daload();
            }
        }

        private void power_const_integer_opt(Expr lhs, long pow) throws CodeGenException {
            if(pow==0){
                cob.loadConstant(1.0d);
                return;
            }

            if(pow<0){
                cob.loadConstant(1.0d);
                build_(lhs);
                cob.ddiv();
                pow = -pow;
            }else
                build_(lhs);

            class Tmp{
                static void run(CodeBuilder cob, long p){
                    if(p==1)return;
                    if((p&1)==1) cob.dup2();
                    cob.dup2();
                    cob.dmul();
                    run(cob, p/2);
                    if((p&1)==1) cob.dmul();
                }
            }
            Tmp.run(cob, pow);
        }

        private CodeBuilder build_(Expr expr) throws CodeGenException {
            switch(expr){
                case Ident(var ident) -> loadVar(ident);
                case Val(double val) -> cob.loadConstant(val);
                case Pow(var lhs, Val(long pow)) when -50<pow && pow<50 -> // only true for whole numbers
                        power_const_integer_opt(lhs, Math.round(pow));
                case BinOp binOp -> {
                    build_(binOp.lhs());
                    build_(binOp.rhs());
                    switch (binOp) {
                        case Add _ -> cob.dadd();
                        case Sub _ -> cob.dsub();
                        case Mul _ -> cob.dmul();
                        case Div _ -> cob.ddiv();
                        case Pow _ -> cob.invokestatic(CD_Math, "pow", MD_double_double_double);
                    }
                }
                case Func func -> {
                    for(var arg : func.args()) build_(arg);
                    var name = func.name();
                    if(Objects.equals(name, "ln")) name = "log";
                    cob.invokestatic(CD_Math, name, MD_double_doubleN[func.args().size()]);
                }
                case UnOp unOp -> {
                    build_(unOp.expr());
                    switch(unOp){
                        case Neg _ -> cob.dneg();
                    }
                }
            }
            return cob;
        }
    }
}
