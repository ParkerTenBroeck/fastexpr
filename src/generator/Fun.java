package generator;


import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Fun {
    public static void main(String... args) throws Exception {
        var loader = new GeneratorClassLoader(Fun.class.getClassLoader());
        loader.loadClass(Fun.class.getName()).getMethod("run").invoke(null);
    }

    public static void run() {
        System.out.println("hello world" + Fun.class.getClassLoader());

        var gen = Test.gen(10, 1.5);
        var res = gen.next();
        while (true) {
            System.out.println(res);
            if (res instanceof Gen.Ret || res == null) break;
            res = gen.next();
        }
    }

    public static class GeneratorClassLoader extends ClassLoader {
        private final static MethodTypeDesc MTD_void_String = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);
        private final static ClassDesc CD_System = ClassDesc.ofDescriptor(System.class.descriptorString());
        private final static ClassDesc CD_PrintStream = ClassDesc.ofDescriptor(PrintStream.class.descriptorString());

        private final static ClassDesc CD_Gen = ClassDesc.ofDescriptor(Gen.class.descriptorString());
        private final static ClassDesc CD_Res = ClassDesc.ofDescriptor(Gen.Res.class.descriptorString());
        private final static MethodTypeDesc MTD_Res = MethodTypeDesc.of(CD_Res);
        private final static MethodTypeDesc MTD_Gen_Obj = MethodTypeDesc.of(CD_Gen, ConstantDescs.CD_Object);

        private final HashMap<String, byte[]> customClazzDefMap = new HashMap<>();
        private final HashMap<String, Class<?>> customClazzMap = new HashMap<>();

        public GeneratorClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (customClazzDefMap.get(name) instanceof byte[] bytes)
                customClazzMap.put(name, defineClass(name, bytes, 0, bytes.length));
            if (customClazzMap.get(name) instanceof Class<?> clazz)
                return clazz;
            if (name.startsWith("java"))
                return super.loadClass(name);

            var p = "/" + name.replace('.', '/') + ".class";
            try (var stream = Fun.class.getResourceAsStream(p)) {
                var bytes = Objects.requireNonNull(stream).readAllBytes();
                bytes = searchForGenerators(bytes);
                Files.write(Path.of("cs/" + name + ".class"), bytes);
                customClazzDefMap.put(name, bytes);
                customClazzMap.put(name, defineClass(name, bytes, 0, bytes.length));
                return customClazzMap.get(name);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        public byte[] searchForGenerators(byte[] in) {
            var clm = ClassFile.of().parse(in);
            return ClassFile.of().build(clm.thisClass().asSymbol(), cb -> {
                for (var ce : clm) {
                    var isGen = clm.thisClass().asSymbol().descriptorString().equals(Gen.class.descriptorString());
                    if (ce instanceof MethodModel mem && !isGen) {
                        var methodRetGen = mem.methodTypeSymbol().returnType().descriptorString().equals(Gen.class.descriptorString());
                        if (methodRetGen) {
                            cb.withMethod(mem.methodName().stringValue(), mem.methodTypeSymbol(), mem.flags().flagsMask(), mb -> {
                                for (var me : mem) {
                                    if (me instanceof CodeModel com) {
                                        mb.withCode(cob -> rebuildGeneratorMethod(clm, mem, com, cob));

                                    } else mb.with(me);
                                }
                            });
                        } else
                            cb.with(mem);


                    } else cb.with(ce);
                }
            });
        }

        private ClassDesc generateGeneratorFromGenMethod(ClassModel clm, MethodModel mem, CodeModel com, CodeBuilder scob) {
            var cd = ClassDesc.of("Gen" + customClazzDefMap.size());

            var bytes = ClassFile.of().build(cd, clb -> {

                        clb.withInterfaces(List.of(clb.constantPool().classEntry(CD_Gen)));

                        scob.new_(cd).dup();
                        var mts = mem.methodTypeSymbol();
                        mts = mts.changeReturnType(ConstantDescs.CD_void);
                        if (!mem.flags().has(AccessFlag.STATIC)) {
                            mts = mts.insertParameterTypes(0, clm.thisClass().asSymbol());
                        }

                        int offset = 0;
                        var mts_params = mts.parameterArray();
                        for (var param : mts_params) {
                            clb.withField("param_" + offset, param, ClassFile.ACC_PRIVATE);
                            var tk = TypeKind.fromDescriptor(param.descriptorString());
                            scob.loadLocal(tk, offset);
                            offset += tk.slotSize();
                        }
                        var count = offset;
                        scob.invokespecial(cd, ConstantDescs.INIT_NAME, mts).areturn();


                        clb.withMethod(ConstantDescs.INIT_NAME, mts, ClassFile.ACC_PUBLIC,
                                mb -> mb.withCode(cob -> {
                                    cob.aload(0).invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void);
                                    int offset2 = 0;
                                    for (var param : mts_params) {
                                        var tk = TypeKind.fromDescriptor(param.descriptorString());
                                        cob.aload(0).loadLocal(tk, offset2 + 1).putfield(cd, "param_" + offset2, param);
                                        offset2 += tk.slotSize();
                                    }
                                    cob.return_();
                                })
                        );
                        clb.withMethod("next", MTD_Res, ClassFile.ACC_PUBLIC, mb -> mb.withCode(cob -> {
                            generateStateMachine(cd, clb, com, cob, count);
                        }));
                    }
            );
            try {
                Files.write(Path.of("cs/" + cd.displayName() + ".class"), bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            customClazzDefMap.put(cd.displayName(), bytes);
            return cd;
        }

        private sealed interface Block{}
        private record YieldBlock(List<CodeElement> block) implements Block{}
        private record RetBlock(List<CodeElement> block) implements Block{}

        private void generateStateMachine(ClassDesc cd, ClassBuilder clb, CodeModel com, CodeBuilder cob, int count) {
            clb.withField("___state___", TypeKind.INT.upperBound(), ClassFile.ACC_PRIVATE);
            var stateSwitchCases = new ArrayList<SwitchCase>();
            var invalidState = cob.newLabel();
            stateSwitchCases.add(SwitchCase.of(0, cob.newLabel()));
            int switchCase = 1;
            for (CodeElement coe : com){
                if(coe instanceof InvokeInstruction is && is.opcode().equals(Opcode.INVOKESTATIC) && is.owner().asSymbol().equals(CD_Gen) && (is.name().equalsString("yield"))){
                    stateSwitchCases.add(SwitchCase.of(switchCase, cob.newLabel()));
                    switchCase++;
                }
            }
            cob.aload(0).getfield(cd, "___state___", TypeKind.INT.upperBound()).lookupswitch(invalidState, stateSwitchCases);
            var start = cob.startLabel();
            var end = cob.newLabel();
            cob.localVariable(0, "this", cd, start, end);

            var stackTypes = new ArrayList<TypeKind>();
            var localVarTypes = new HashMap<Integer, TypeKind>();

            switchCase = 1;
            cob.labelBinding(stateSwitchCases.removeFirst().target());
            final boolean[] ignore_next_return = {false};
            final boolean[] ignore_next_pop = {false};
            for (CodeElement coe : com) {
                switch (coe) {
                    case InvokeInstruction is when is.opcode().equals(Opcode.INVOKESTATIC) && is.owner().asSymbol().equals(CD_Gen) && (is.name().equalsString("yield") || is.name().equalsString("ret")) -> {
                        if (MethodTypeDesc.ofDescriptor(is.method().type().stringValue()).parameterArray().length == 0) {
                            cob.aconst_null();
                        }

                        if (is.name().equalsString("ret")) {
                            cob.aload(0).loadConstant(-1).putfield(cd, "___state___", TypeKind.INT.upperBound()).areturn();
                            ignore_next_return[0] = true;
                        } else {
                            cob.aload(0).loadConstant(switchCase).putfield(cd, "___state___", TypeKind.INT.upperBound()).areturn();
                            switchCase++;
                            cob.labelBinding(stateSwitchCases.removeFirst().target());
                            ignore_next_pop[0] = true;
                        }
                    }
                    case Instruction ins when ins.opcode() == Opcode.POP && ignore_next_pop[0] ->
                            ignore_next_pop[0] = false;
                    case ReturnInstruction _ when ignore_next_return[0] ->
                            ignore_next_return[0] = false;
                    case BranchInstruction b -> cob.with(b);
                    case LocalVariable lv when lv.slot() < count -> {}
                    case LocalVariable lv ->
                            cob.localVariable(lv.slot() - count + 1, lv.name(), lv.type(), lv.startScope(), lv.endScope());

                    case IncrementInstruction ii when ii.slot() < count -> throw new RuntimeException();
                    case IncrementInstruction ii -> cob.iinc(ii.slot() - count + 1, ii.constant());

                    case LoadInstruction li when li.slot() < count ->
                            cob.aload(0).getfield(cd, "param_" + li.slot(), li.typeKind().upperBound());
                    case LoadInstruction li ->
                        cob.loadLocal(li.typeKind(), li.slot() - count + 1);

                    case StoreInstruction ls when ls.slot() < count && ls.typeKind().slotSize()==2 ->
                            cob.aload(0).dup_x2().pop().putfield(cd, "param_" + ls.slot(), ls.typeKind().upperBound());
                    case StoreInstruction ls when ls.slot() < count ->
                            cob.aload(0).swap().putfield(cd, "param_" + ls.slot(), ls.typeKind().upperBound());

                    case StoreInstruction ls -> {
                        var prev = localVarTypes.put(ls.slot() - count + 1, ls.typeKind());
                        if(prev !=null && !prev.equals(ls.typeKind()))
                            throw new RuntimeException("Type miss match");
                        cob.storeLocal(ls.typeKind(), ls.slot() - count + 1);
                    }
                    case ConstantInstruction ci -> cob.loadConstant(ci.constantValue());

                    case null, default -> cob.with(coe);
                }
            }
            cob.labelBinding(invalidState);
            cob.aconst_null().areturn();
            cob.labelBinding(end);
        }

        private void rebuildGeneratorMethod(ClassModel clm, MethodModel mem, CodeModel com, CodeBuilder cob) {
            generateGeneratorFromGenMethod(clm, mem, com, cob);
        }
    }

//    public static Gen<String, String> meow() {
//        for (int i = 0; i < 2; i++) {
//            System.out.println(i + "asldkjasd");
//        }
//        return null;
//    }

//    public Gen<String, String> gen(double l, int v) {
//        Gen.yield("Yield");
//        return Gen.ret("Ret");
//    }
//
//    public Gen<String, String> gen2() {
//        Gen.yield("Yield");
//        return Gen.ret("Ret");
//    }


    public static class Mixer implements Gen<String, String> {
        public void test() {
            for (int i = 0; i < 10; i++) {
                System.out.println(i + "th iteration");
            }

            while (this.next() instanceof Gen.Yield(var yield)) {

            }
        }

        @Override
        public Res<String, String> next() {
            return new Yield<>("12");
        }
    }
}
