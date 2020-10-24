package com.github.ilevshevich.processor;

import com.github.ilevshevich.annotation.RequireNonNull;
import com.github.ilevshevich.function.HeptaConsumer;
import com.github.ilevshevich.function.TetraConsumer;
import com.github.ilevshevich.misc.Inheritance;
import com.github.ilevshevich.util.Cast;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

final class RequireNonNullProxyCreator {
    private static final String DEFAULT_NAME_POSTFIX = "RequireNonNullProxy";

    private Messager messager;

    private Trees mTrees;
    private TreeMaker mTreeMaker;

    private Names names;

    static RequireNonNullProxyCreator of(final ProcessingEnvironment processingEnvironment) {
        return new RequireNonNullProxyCreator(processingEnvironment);
    }

    private RequireNonNullProxyCreator(final ProcessingEnvironment processingEnvironment) {
        this.messager = processingEnvironment.getMessager();

        this.mTreeMaker = TreeMaker.instance(Cast.cast(processingEnvironment, JavacProcessingEnvironment.class).getContext());
        this.mTrees = Trees.instance(processingEnvironment);

        this.names = Names.instance(Cast.cast(processingEnvironment, JavacProcessingEnvironment.class).getContext());
    }

    void process(final Element element,
                 final Function<Element, JCTree.JCClassDecl> classDecl,
                 final Function<Element, JCTree.JCMethodDecl> methodDecl,
                 final Consumer<JCTree.JCMethodDecl> check,
                 final TetraConsumer<String, Inheritance, Inheritance, JCTree.JCMethodDecl> proxyGenerate,
                 final HeptaConsumer<Name, JCTree.JCModifiers, List<JCTree.JCVariableDecl>, Inheritance, Inheritance, JCTree.JCClassDecl, JCTree.JCMethodDecl> methodGenerate) {
        try {
            final JCTree.JCClassDecl clazz = Objects.requireNonNull(classDecl.apply(element));
            final JCTree.JCMethodDecl method = Objects.requireNonNull(methodDecl.apply(element));
            {
                check.accept(method);
            }
            final Name name = Objects.requireNonNull(method.getName());
            final JCTree.JCModifiers modifiers = Objects.requireNonNull(Cast.cast(method.getModifiers().clone(), JCTree.JCModifiers.class));
            final List<JCTree.JCVariableDecl> parameters = Objects.requireNonNull(method.getParameters());
            final RequireNonNull annotation = Objects.requireNonNull(element.getAnnotation(RequireNonNull.class));
            final Inheritance methodAnnotationsInheritance = Objects.requireNonNull(annotation.methodAnnotations());
            final Inheritance parameterAnnotationsInheritance = Objects.requireNonNull(annotation.parameterAnnotations());
            final String namePostfix = Objects.requireNonNull(annotation.namePostfix());
            {
                proxyGenerate.accept(namePostfix, methodAnnotationsInheritance, parameterAnnotationsInheritance, method);
                methodGenerate.accept(name, modifiers, parameters, methodAnnotationsInheritance, parameterAnnotationsInheritance, clazz, method);
            }
            messager.printMessage(Diagnostic.Kind.NOTE, String.format("Generated proxy '%s' into '%s'...", method.name, clazz.name), element);
        } catch (final Exception ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), element);
        }
    }

    JCTree.JCClassDecl getClassDecl(final Element element) {
        return Cast.cast(mTrees.getTree(Cast.cast(element, Symbol.MethodSymbol.class).owner), JCTree.JCClassDecl.class);
    }

    JCTree.JCMethodDecl getMethodDecl(final Element element) {
        return Cast.cast(mTrees.getTree(element), JCTree.JCMethodDecl.class);
    }

    void checkMethodDecl(final JCTree.JCMethodDecl method) {
        final JCTree.JCExpression type = Cast.cast(method.getReturnType(), JCTree.JCExpression.class);
        if ((type instanceof JCTree.JCPrimitiveTypeTree) &&
                Cast.cast(type, JCTree.JCPrimitiveTypeTree.class).typetag.equals(TypeTag.VOID)) {
            throw new RuntimeException("Return type must not be VOID");
        }
    }

    void proxyGenerate(final String namePostfix,
                       final Inheritance methodAnnotationsInheritance,
                       final Inheritance parameterAnnotationsInheritance,
                       final JCTree.JCMethodDecl method) {
        method.mods.flags &= ~Flags.PUBLIC;
        method.mods.flags &= ~Flags.PROTECTED;
        method.mods = isProxyMethodAnnotationsAvailable(methodAnnotationsInheritance) ?
                mTreeMaker.Modifiers(method.mods.flags |= Flags.PRIVATE, method.mods.annotations) :
                mTreeMaker.Modifiers(method.mods.flags |= Flags.PRIVATE);
        if (!isProxyParameterAnnotationsInheritanceAvailable(parameterAnnotationsInheritance)) {
            method.params = prepare(method.params);
        }
        method.name = names.fromString(String.format("%s$$%s", method.name, (Objects.nonNull(namePostfix) && !namePostfix.isEmpty()) ? namePostfix : DEFAULT_NAME_POSTFIX));
    }

    void methodGenerate(final Name methodName,
                        final JCTree.JCModifiers modifiers,
                        final List<JCTree.JCVariableDecl> parameters,
                        final Inheritance methodAnnotationsInheritance,
                        final Inheritance parameterAnnotationsInheritance,
                        final JCTree.JCClassDecl clazz,
                        final JCTree.JCMethodDecl method) {
        final JCTree.JCExpression returnType = Cast.cast(method.getReturnType(), JCTree.JCExpression.class);
        final List<JCTree.JCTypeParameter> generics = method.getTypeParameters();
        final List<JCTree.JCExpression> throwz = method.getThrows();
        {
            final JCTree.JCBlock methodBody =
                    mTreeMaker.Block(0, List.from(new JCTree.JCStatement[]{
                            mTreeMaker.Return(
                                    mTreeMaker.Apply(
                                            List.<JCTree.JCExpression>nil(),
                                            mTreeMaker.Select(
                                                    mTreeMaker.Select(
                                                            mTreeMaker.Select(
                                                                    mTreeMaker.Ident(names.fromString("java")), names.fromString("util")), names.fromString("Objects")), names.fromString("requireNonNull")),
                                            List.of(mTreeMaker.Apply(
                                                    List.<JCTree.JCExpression>nil(),
                                                    mTreeMaker.Select(((modifiers.flags & Flags.STATIC) == Flags.STATIC) ? mTreeMaker.Ident(clazz.name) : mTreeMaker.Ident(names.fromString("this")), method.name),
                                                    List.from(parameters.stream().map(arg -> mTreeMaker.Ident(arg.name)).collect(toList()))
                                            ))))
                    }));
            final JCTree.JCMethodDecl methodDecl =
                    mTreeMaker.MethodDef(
                            isWrapperMethodAnnotationsAvailable(methodAnnotationsInheritance) ?
                                    modifiers :
                                    mTreeMaker.Modifiers(modifiers.flags),
                            methodName,
                            returnType,
                            generics,
                            isWrapperParameterAnnotationsAvailable(parameterAnnotationsInheritance) ?
                                    parameters :
                                    prepare(parameters),
                            throwz,
                            methodBody,
                            null);
            {
                clazz.defs = clazz.defs.append(methodDecl);
            }
        }
    }

    private List<JCTree.JCVariableDecl> prepare(final List<JCTree.JCVariableDecl> parameters) {
        final ListBuffer<JCTree.JCVariableDecl> result = new ListBuffer<>();
        {
            for (final JCTree.JCVariableDecl parameter : parameters) {
                final JCTree.JCVariableDecl variableDecl = Cast.cast(parameter.clone(), JCTree.JCVariableDecl.class);
                {
                    variableDecl.mods = mTreeMaker.Modifiers(parameter.mods.flags);
                }
                result.append(variableDecl);
            }
        }
        return result.toList();
    }

    private boolean isProxyMethodAnnotationsAvailable(final Inheritance inheritance) {
        return inheritance.equals(Inheritance.ONLY_PROXY);
    }

    private boolean isProxyParameterAnnotationsInheritanceAvailable(final Inheritance inheritance) {
        return inheritance.equals(Inheritance.ONLY_PROXY);
    }

    private boolean isWrapperMethodAnnotationsAvailable(final Inheritance inheritance) {
        return inheritance.equals(Inheritance.ONLY_WRAPPER);
    }

    private boolean isWrapperParameterAnnotationsAvailable(final Inheritance inheritance) {
        return inheritance.equals(Inheritance.ONLY_WRAPPER);
    }
}
