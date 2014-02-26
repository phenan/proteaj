package proteaj;

import proteaj.error.*;
import proteaj.io.*;
import proteaj.ir.*;
import proteaj.tast.*;
import proteaj.util.*;

import java.util.*;
import javassist.*;

public class BodyCompiler {
  public BodyCompiler(IR ir) {
    this.ir = ir;
    this.parser = new BodyParser();
  }

  public Program compile() {
    Program program = new Program(ir);

    program.addConstructors(compileConstructors());
    program.addMethods(compileMethods());
    program.addFields(compileFields());
    program.addDefaultValues(compileDefaultArguments());
    program.addClassInitializers(compileStaticInitializers());

    return program;
  }

  private List<Pair<CtMethod, MethodBody>> compileMethods() {
    List<Pair<CtMethod, MethodBody>> methods = new ArrayList<Pair<CtMethod, MethodBody>>();

    for(IRMethodBody irbody : ir.getMethods()) {
      CtMethod method = irbody.getCtMethod();
      Environment env = new Environment(ir, method);
      SourceStringReader reader = new SourceStringReader(irbody.getSource(), env.filePath, irbody.getLine());

      try {
        env.addParams(irbody.getParamNames(), irbody.getParamTypes());
      } catch (NotFoundException e) {
        ErrorList.addError(new NotFoundError(e, env.filePath, reader.getLine()));
      }

      try {
        MethodBody body = parser.parseMethodBody(method, reader, env);
        methods.add(new Pair<CtMethod, MethodBody>(method, body));
      } catch (CompileError e) {
        ErrorList.addError(e);
      } catch (CompileErrors es) {
        for(CompileError e : es.getErrors()) ErrorList.addError(e);
      }
    }

    return methods;
  }

  private List<Pair<CtConstructor, ConstructorBody>> compileConstructors() {
    List<Pair<CtConstructor, ConstructorBody>> constructors = new ArrayList<Pair<CtConstructor, ConstructorBody>>();

    for(IRConstructorBody irbody : ir.getConstructors()) {
      CtConstructor constructor = irbody.getCtConstructor();
      Environment env = new Environment(ir, constructor);
      SourceStringReader reader = new SourceStringReader(irbody.getSource(), env.filePath, irbody.getLine());

      try {
        env.addParams(irbody.getParamNames(), irbody.getParamTypes());
      } catch (NotFoundException e) {
        ErrorList.addError(new NotFoundError(e, reader.getFilePath(), reader.getLine()));
      }

      try {
        ConstructorBody body = parser.parseConstructorBody(constructor, reader, env);
        constructors.add(new Pair<CtConstructor, ConstructorBody>(constructor, body));
      } catch (CompileError e) {
        ErrorList.addError(e);
      } catch (CompileErrors es) {
        for(CompileError e : es.getErrors()) ErrorList.addError(e);
      }
    }

    return constructors;
  }

  private List<Pair<CtField, FieldBody>> compileFields() {
    List<Pair<CtField, FieldBody>> fields = new ArrayList<Pair<CtField, FieldBody>>();

    for(IRFieldBody irbody : ir.getFields()) {
      CtField field = irbody.getCtField();
      Environment env = new Environment(ir, field);
      SourceStringReader reader = new SourceStringReader(irbody.getSource(), env.filePath, irbody.getLine());

      try {
        FieldBody body = parser.parseFieldBody(field, reader, env);
        //irbody.setAST(fbody);
        fields.add(new Pair<CtField, FieldBody>(field, body));
      } catch (CompileError e) {
        ErrorList.addError(e);
      } catch (CompileErrors es) {
        for(CompileError e : es.getErrors()) ErrorList.addError(e);
      }
    }

    return fields;
  }

  private List<Pair<CtMethod, DefaultValue>> compileDefaultArguments() {
    List<Pair<CtMethod, DefaultValue>> map = new ArrayList<Pair<CtMethod, DefaultValue>>();

    for(IRDefaultArgument irbody : ir.getDefaultArguments()) {
      CtMethod method = irbody.getCtMethod();
      Environment env = new Environment(ir, method);
      SourceStringReader reader = new SourceStringReader(irbody.getSource(), env.filePath, irbody.getLine());

      try {
        DefaultValue defval = parser.parseDefaultArgument(method, reader, env);
        map.add(new Pair<CtMethod, DefaultValue>(method, defval));
      } catch (CompileError e) {
        ErrorList.addError(e);
      } catch (CompileErrors es) {
        for(CompileError e : es.getErrors()) ErrorList.addError(e);
      }
    }

    return map;
  }

  private List<Pair<CtConstructor, ClassInitializer>> compileStaticInitializers() {
    List<Pair<CtConstructor, ClassInitializer>> list = new ArrayList<Pair<CtConstructor, ClassInitializer>>();

    for(IRStaticInitializer sinit : ir.getStaticInitializers()) {
      CtConstructor clinit = sinit.getCtConstructor();
      Environment env = new Environment(ir, clinit);
      SourceStringReader reader = new SourceStringReader(sinit.getSource(), env.filePath, sinit.getLine());

      try {
        ClassInitializer body = parser.parseStaticInitializer(reader, env);
        list.add(new Pair<CtConstructor, ClassInitializer>(clinit, body));
      } catch (CompileError e) {
        ErrorList.addError(e);
      } catch (CompileErrors es) {
        for(CompileError e : es.getErrors()) ErrorList.addError(e);
      }
    }

    return list;
  }

  private IR ir;
  private BodyParser parser;
}
