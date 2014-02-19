package proteaj;

import proteaj.error.*;
import proteaj.io.*;
import proteaj.ir.*;
import proteaj.ir.tast.*;
import proteaj.pparser.*;

import java.util.*;
import java.util.Map.Entry;
import javassist.*;

public class BodyParser {
  public BodyParser(IR ir) {
    this.ir = ir;
  }

  public MethodBody parseMethodBody(CtMethod method, SourceStringReader reader, Environment env, TypeResolver resolver, UsingOperators usops) throws CompileError, CompileErrors {
    try {
      CtClass thisCls = method.getDeclaringClass();
      CtClass returnType = method.getReturnType();
      CtClass[] exceptionTypes = method.getExceptionTypes();
      initParsers_MethodBody(thisCls, returnType, method, resolver, usops);

      TypedAST mbody = MethodBodyParser.parser.applyRule(reader, env);
      if(! mbody.isFail()) {
        env.removeExceptions(exceptionTypes);

        if(env.hasException()) {
          throw createUnhandledExceptions(reader, env);
        }

        return (MethodBody)mbody;
      }

      throw new ParseError(mbody.getFailLog().getMessage(), reader.getFilePath(), mbody.getFailLog().getLine());
    } catch (NotFoundException e) {
      throw new NotFoundError(e, reader.getFilePath(), 0);
    }
  }

  public ConstructorBody parseConstructorBody(CtConstructor constructor, SourceStringReader reader, Environment env, TypeResolver resolver, UsingOperators usops) throws CompileError, CompileErrors {
    try {
      CtClass thisCls = constructor.getDeclaringClass();
      CtClass[] exceptionTypes = constructor.getExceptionTypes();
      initParsers_ConstructorBody(thisCls, constructor, resolver, usops);

      TypedAST cbody = ConstructorBodyParser.parser.applyRule(reader, env);
      if(! cbody.isFail()) {
        env.removeExceptions(exceptionTypes);

        if(env.hasException()) {
          throw createUnhandledExceptions(reader, env);
        }

        return (ConstructorBody)cbody;
      }

      throw new ParseError(cbody.getFailLog().getMessage(), reader.getFilePath(), cbody.getFailLog().getLine());
    } catch (NotFoundException e) {
      throw new NotFoundError(e, reader.getFilePath(), 0);
    }
  }

  public FieldBody parseFieldBody(CtField field, SourceStringReader reader, Environment env, TypeResolver resolver, UsingOperators usops) throws CompileError, CompileErrors {
    try {
      CtClass thisCls = field.getDeclaringClass();
      CtClass type = field.getType();
      initParsers_FieldBody(thisCls, type, field, resolver, usops);

      TypedAST fbody = FieldBodyParser.parser.applyRule(reader, env);
      if(! fbody.isFail()) {
        if(env.hasException()) {
          throw createUnhandledExceptions(reader, env);
        }
        return (FieldBody)fbody;
      }

      throw new ParseError(fbody.getFailLog().getMessage(), reader.getFilePath(), fbody.getFailLog().getLine());
    } catch (NotFoundException e) {
      throw new NotFoundError(e, reader.getFilePath(), 0);
    }
  }

  public DefaultValue parseDefaultArgument(CtMethod method, SourceStringReader reader, Environment env, TypeResolver resolver, UsingOperators usops) throws CompileError, CompileErrors {
    try {
      CtClass thisCls = method.getDeclaringClass();
      CtClass type = method.getReturnType();
      initParsers_DefaultArgs(thisCls, type, method, resolver, usops);

      TypedAST defval = DefaultArgumentParser.parser.applyRule(reader, env);
      if(! defval.isFail()) {
        if(env.hasException()) {
          throw createUnhandledExceptions(reader, env);
        }
        return (DefaultValue)defval;
      }

      throw new ParseError(defval.getFailLog().getMessage(), reader.getFilePath(), defval.getFailLog().getLine());
    } catch (NotFoundException e) {
      throw new NotFoundError(e, reader.getFilePath(), 0);
    }
  }

  public ClassInitializer parseStaticInitializer(CtConstructor constructor, SourceStringReader reader, Environment env, TypeResolver resolver, UsingOperators usops) throws CompileError, CompileErrors {
    CtClass thisCls = constructor.getDeclaringClass();
    initParsers_StaticInitializer(thisCls, constructor, resolver, usops);

    TypedAST sibody = StaticInitializerParser.parser.applyRule(reader, env);
    if(! sibody.isFail()) {
      if(env.hasException()) {
        throw createUnhandledExceptions(reader, env);
      }

      return (ClassInitializer)sibody;
    }

    throw new ParseError(sibody.getFailLog().getMessage(), reader.getFilePath(), sibody.getFailLog().getLine());
  }

  private CompileErrors createUnhandledExceptions(SourceStringReader reader, Environment env) {
    List<CompileError> errors = new ArrayList<CompileError>();
    for(Entry<CtClass, List<Integer>> entry : env.getExceptionsData().entrySet()) {
      CtClass exception = entry.getKey();
      for(int line : entry.getValue()) {
        errors.add(new ParseError("unhandled exception type " + exception.getName(), reader.getFilePath(), line));
      }
    }
    return new CompileErrors(errors);
  }

  private void initParsers_MethodBody(CtClass thisCls, CtClass returnType, CtMethod method, TypeResolver resolver, UsingOperators usops) {
    initParser_Statement(returnType);
    initParser_Expression(thisCls, method, resolver, usops);
    initParser_Basic(resolver);
  }

  private void initParsers_ConstructorBody(CtClass thisCls, CtConstructor constructor, TypeResolver resolver, UsingOperators usops) {
    ConstructorBodyParser.parser.init(thisCls);

    ThisConstructorCallParser.parser.init(thisCls, constructor);
    SuperConstructorCallParser.parser.init(thisCls);

    initParser_Statement(null);
    initParser_Expression(thisCls, constructor, resolver, usops);
    initParser_Basic(resolver);
  }

  private void initParsers_FieldBody(CtClass thisCls, CtClass type, CtField field, TypeResolver resolver, UsingOperators usops) {
    FieldBodyParser.parser.init(type);

    initParser_Expression(thisCls, field, resolver, usops);
    initParser_Basic(resolver);
  }

  private void initParsers_DefaultArgs(CtClass thisCls, CtClass type, CtMethod method, TypeResolver resolver, UsingOperators usops) {
    DefaultArgumentParser.parser.init(type);
    initParser_Expression(thisCls, method, resolver, usops);
    initParser_Basic(resolver);
  }

  private void initParsers_StaticInitializer(CtClass thisCls, CtConstructor constructor, TypeResolver resolver, UsingOperators usops) {
    initParser_Statement(null);
    initParser_Expression(thisCls, constructor, resolver, usops);
    initParser_Basic(resolver);
  }

  private void initParser_Statement(CtClass returnType) {
    if(returnType == null) ReturnStatementParser.parser.disable();
    else ReturnStatementParser.parser.init(returnType);
  }

  private void initParser_Expression(CtClass thisCls, CtMember member, TypeResolver resolver, UsingOperators usops) {
    ExpressionParser.init(usops);
    OperationParser.initAll(usops, ir);
    MethodCallParser.parser.init(thisCls);
    FieldAccessParser.parser.init(thisCls);
    AbbMethodCallParser.parser.init(thisCls, member);
    StaticMethodCallParser.parser.init(thisCls);
    StaticFieldAccessParser.parser.init(thisCls);
    NewExpressionParser.parser.init(thisCls);
    NewArrayExpressionParser.parser.init(resolver);
    CastExpressionParser.parser.init(resolver);
    ReadasOperationParser.initAll(usops);
    PrimitiveReadasOperationParser.initAll();
    ReadasOperandParser.init(usops);
  }

  private void initParser_Basic(TypeResolver resolver) {
    ClassNameParser.parser.init(resolver);
    TypeNameParser.parser.init(resolver);
    PackratParser.initialize();
  }

  private final IR ir;
}
