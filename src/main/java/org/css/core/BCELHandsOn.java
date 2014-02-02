package org.css.core;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.util.ByteSequence;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: psylit
 * Date: 23.01.14
 * Time: 19:40
 * To change this template use File | Settings | File Templates.
 */

class MethodInvocation {

    private String clasz;
    private String methodName;
    private String signature;

    private  MethodInvocation caller;

    MethodInvocation(String clasz, String methodName, String signature, MethodInvocation caller) {

        this.clasz = clasz;
        this.methodName = methodName;
        this.signature = signature;

        this.caller = caller;
    }

    MethodInvocation getCaller() {
        return caller;
    }

    String getClasz() {
        return clasz;
    }

    String getMethodName() {
        return methodName;
    }

    String getSignature() {
        return signature;
    }

    private String stringRepresentation(MethodInvocation methodInvocation){

        return "[Class: " + methodInvocation.clasz + " Method: "
                + methodInvocation.methodName + " Signature: "
                +  methodInvocation.signature + "]";

    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        LinkedList<MethodInvocation> breadcump = new LinkedList();

        if(caller != null){

            //builder.append(" --> ");
            MethodInvocation temp = this.caller;

            while(temp != null){

                breadcump.add(temp);
                temp = temp.caller;
            }
        }

        Collections.reverse(breadcump);
        for(MethodInvocation invocation : breadcump){

            builder.append(stringRepresentation(invocation));
            builder.append(" --> ");
        }

        builder.append(stringRepresentation(this));

        return builder.toString();

    }

    @Override
    public int hashCode() {

        return 37* clasz.hashCode() + 25 * methodName.hashCode() + signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof MethodInvocation){

            MethodInvocation m = ((MethodInvocation)obj);

            return m.clasz.equals(clasz) &&
                   m.methodName.equals(methodName) &&
                   m.equals(signature);
        }

        return false;
    }
}

class ThreadState {

    private static final HashMap<String, HashMap<String, String>> heap = new HashMap<String, HashMap<String, String>>();
    private LinkedList stack = new LinkedList();
    private ArrayList localVariebles = new ArrayList();
   // private MethodInvocation caller;

    private String lastMethod = "";

    public void execute(Code code, MethodInvocation caller) throws IOException {

        if(code == null || (code != null && code.getCode() == null)){

            return;
        }

        ByteSequence byteCodeSequnce = new ByteSequence(code.getCode());

        try{

            do {

                executeByteCode(byteCodeSequnce, code, caller);

            } while(true);

        }catch(EOFException ex){}
    }

    private void executeByteCode(ByteSequence byteCodeSequnce, Code codeObject,
                                 MethodInvocation caller) throws IOException {

        short code = (short)byteCodeSequnce.readUnsignedByte();

        switch (code){

            case Constants.INVOKEVIRTUAL:
            case Constants.INVOKESPECIAL:
            case Constants.INVOKESTATIC:
            case Constants.INVOKEINTERFACE:

                MethodInvocation methodInvocation = createMethodInvocation(byteCodeSequnce, codeObject, caller);
                execute(findCode(methodInvocation), methodInvocation);

                System.out.println(methodInvocation.toString());

                break;

            default:

                break;
        }
    }

    private MethodInvocation createMethodInvocation(ByteSequence byteCodeSequnce, Code codeObject,
                                                    MethodInvocation caller) throws IOException {

        int constatntPoolIndex = byteCodeSequnce.readUnsignedShort();

        ConstantPool constantPool = codeObject.getConstantPool();
        Constant c = constantPool.getConstant(constatntPoolIndex, Constants.CONSTANT_Methodref);

        String clasz = (constantPool.constantToString(((ConstantCP) c).getClassIndex()
                ,Constants.CONSTANT_Class));

        String[] method = (constantPool.
                constantToString(((ConstantCP)c).getNameAndTypeIndex(),
                        Constants.CONSTANT_NameAndType)).split(" ");

        return new MethodInvocation(clasz , method[0], method[1], caller);
    }

    private Code findCode(MethodInvocation invocation){

//        if(invocation.getClasz().contains("java.lang")){
//
//            return null;
//        }

        MethodInvocation mtdInvc = invocation.getCaller();

        while(mtdInvc != null){

            if(mtdInvc.equals(invocation)){

                return null;
            }

            mtdInvc = mtdInvc.getCaller();
        }

        JavaClass javaClass =  Repository.lookupClass(invocation.getClasz());

        for(Method m : javaClass.getMethods()){

            if(m.getName().equals(invocation.getMethodName())) {

                return m.getCode();
            }
        }

        return null;
    }
}


class A {

}

class B extends A {

}

public class BCELHandsOn {

    public static void main(String[] args) throws NoSuchFieldException, IOException {

        System.out.println( new B() instanceof A);

        JavaClass javaClass =  Repository.lookupClass("org.css.core.BCELHandsOn");

        for(Method m : javaClass.getMethods()){

            if(m.getName().equals("classTest")) {


                MethodInvocation methodInvocation
                        = new MethodInvocation("org.css.core.BCELHandsOn" , m.getName() , m.getSignature(), null);

                new ThreadState().execute(m.getCode(), methodInvocation);

               //System.out.println(m.getCode().toString());
                ConstantPool constantPool = m.getCode().getConstantPool();

                Constant constant = constantPool.getConstant(15);

               // System.out.println("Happy day");
            }
        }
    }

    public void classTest(){

        dummyMethod("Here we go");
    }

    public char dummyMethod(String value){

        char c = value.charAt(1);
        return c;

    }
}
