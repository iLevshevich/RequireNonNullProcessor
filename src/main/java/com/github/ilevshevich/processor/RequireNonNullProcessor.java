package com.github.ilevshevich.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes("com.github.ilevshevich.annotation.RequireNonNull")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public final class RequireNonNullProcessor extends AbstractProcessor {
    private RequireNonNullProxyCreator creator = null;

    @Override
    public void init(final ProcessingEnvironment environment) {
        super.init(environment);

        if (Objects.isNull(creator)) {
            creator = RequireNonNullProxyCreator.of(environment);
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment environment) {
        for (final TypeElement annotation : annotations) {
            final Set<? extends Element> elements = environment.getElementsAnnotatedWith(annotation);
            for (final Element element : elements) {
                creator.process(element,
                        this.creator::getClassDecl,
                        this.creator::getMethodDecl,
                        this.creator::checkMethodDecl,
                        this.creator::proxyGenerate,
                        this.creator::methodGenerate);
            }
        }

        return true;
    }
}
