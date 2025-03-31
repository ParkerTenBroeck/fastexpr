package generator;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

public class Fun {
    public static void main(String... args) throws Exception {
        var loader = new GeneratorClassLoader(Fun.class.getClassLoader());
        loader.loadClass(Fun.class.getName()).getMethod("run").invoke(null);
    }

    public static void run(){
        System.out.println("hello world" + Fun.class.getClassLoader());
        var gen = new Gen1();
//        while(gen.next())
    }

    public static class GeneratorClassLoader extends ClassLoader{
        private final static MethodTypeDesc MTD_void_String = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);
        private final static ClassDesc CD_System = ClassDesc.ofDescriptor(System.class.descriptorString());
        private final static ClassDesc CD_PrintStream = ClassDesc.ofDescriptor(PrintStream.class.descriptorString());

        private final static ClassDesc CD_Gen = ClassDesc.ofDescriptor(Gen.class.descriptorString());
        private final static MethodTypeDesc MTD_Gen_Obj = MethodTypeDesc.of(CD_Gen, ConstantDescs.CD_Object);

        private final HashMap<String, byte[]> customClazzDefMap = new HashMap<>();
        private final HashMap<String, Class<?>> customClazzMap = new HashMap<>();

        public GeneratorClassLoader(ClassLoader parent){
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if(customClazzDefMap.get(name) instanceof byte[] bytes)
                customClazzMap.put(name, defineClass(name, bytes, 0, bytes.length));
            if(customClazzMap.get(name) instanceof Class<?> clazz)
                return clazz;
            if(name.startsWith("java"))
                return super.loadClass(name);

            var p = "/"+name.replace('.', '/') +".class";
            try(var stream = Fun.class.getResourceAsStream(p)) {
                var bytes = Objects.requireNonNull(stream).readAllBytes();
                bytes = searchForGenerators(bytes);
                Files.write(Path.of("cs/"+name+".class"), bytes);
                customClazzDefMap.put(name, bytes);
                customClazzMap.put(name, defineClass(name, bytes, 0, bytes.length));
                return customClazzMap.get(name);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        public byte[] searchForGenerators(byte[] in){

            var clm = ClassFile.of().parse(in);
            return ClassFile.of().build(clm.thisClass().asSymbol(), cb -> {
                for(var ce : clm){
                    var isGen = clm.thisClass().asSymbol().descriptorString().equals(Gen.class.descriptorString());
                    if(ce instanceof MethodModel mem && !isGen){
                        var methodRetGen = mem.methodTypeSymbol().returnType().descriptorString().equals(Gen.class.descriptorString());
                        if(methodRetGen){
                            cb.withMethod(mem.methodName().stringValue(), mem.methodTypeSymbol(), mem.flags().flagsMask(), mb -> {
                                for(var me : mem){
                                    if(me instanceof CodeModel com){
                                        mb.withCode(cob -> rebuildGeneratorMethod(clm, mem, com, cob));

                                    }else mb.with(me);
                                }
                            });
                        }else
                            cb.with(mem);


                    }else cb.with(ce);
                }
            });
        }

        private static void rebuildGeneratorMethod(ClassModel clm, MethodModel mem, CodeModel com, CodeBuilder cob) {
            var list = com.elementList();
            for (int i = 0; i < list.size(); i ++){
                var coe = list.get(i);

                if(coe instanceof InvokeInstruction is
                        && is.opcode().equals(Opcode.INVOKESTATIC)
                        && is.owner().asSymbol().equals(CD_Gen)
                        && (is.name().equalsString("yield") || is.name().equalsString("ret"))){

                    cob.getstatic(CD_System, "out", CD_PrintStream)
                            .ldc(is.name().stringValue())
                            .invokevirtual(CD_PrintStream, "println", MTD_void_String);
                }

                if(coe instanceof Instruction ins) {
                    cob.getstatic(CD_System, "out", CD_PrintStream)
                            .ldc(">"+coe)
                            .invokevirtual(CD_PrintStream, "println", MTD_void_String)
                            .with(ins);
                    continue;
                }
                cob.with(coe);

            }
        }
    }

    public static class Gen1 implements Gen<String, String>{
        @Override
        public Res<String, String> next() {
            this.yield("Yield");
            return this.ret("Ret");
        }
    }

    public static class Gen2 implements Gen<String, Void>{
        int times;
        public Gen2(int times){
            this.times = times;
        }

        @Override
        public Res<String, Void> next() {
            for(int i = 0; i < times; i ++){
                this.yield("iteration number: " + i);
            }
            return this.ret();
        }
    }

    public static class Mixer implements Gen<String, String> {
        public void test(){
            for(int i = 0; i < 10; i ++){
                System.out.println(i + "th iteration");
            }

            while(this.next() instanceof Gen.Yield(var yield)){

            }
        }

        @Override
        public Res<String, String> next() {
            return new Yield<>("12");
        }
    }
}
