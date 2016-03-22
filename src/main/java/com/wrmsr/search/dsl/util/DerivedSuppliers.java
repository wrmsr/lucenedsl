/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wrmsr.search.dsl.util;

import com.facebook.presto.bytecode.AnnotationDefinition;
import com.facebook.presto.bytecode.ClassDefinition;
import com.facebook.presto.bytecode.CompilerUtils;
import com.facebook.presto.bytecode.MethodDefinition;
import com.facebook.presto.bytecode.Parameter;
import com.facebook.presto.bytecode.ParameterizedType;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ClassUtils;

import javax.inject.Inject;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.facebook.presto.bytecode.Access.FINAL;
import static com.facebook.presto.bytecode.Access.PRIVATE;
import static com.facebook.presto.bytecode.Access.PUBLIC;
import static com.facebook.presto.bytecode.Access.STATIC;
import static com.facebook.presto.bytecode.Access.a;
import static com.facebook.presto.bytecode.Parameter.arg;
import static com.facebook.presto.bytecode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.wrmsr.search.dsl.util.ImmutableCollectors.toImmutableList;
import static java.util.Objects.requireNonNull;

/*
TODO:
- Allowing passing {name: class} map for Supplier conversion
*/
public final class DerivedSuppliers
{
    private DerivedSuppliers()
    {
    }

    private static final class TargetParameter
    {
        private final java.lang.reflect.Type type;
        private final ParameterizedType parameterizedType;
        private final List<Annotation> annotations;
        private final String name;

        public TargetParameter(java.lang.reflect.Method method, int index)
        {
            type = method.getGenericParameterTypes()[index];
            parameterizedType = fromReflectType(type);
            annotations = ImmutableList.copyOf(method.getParameterAnnotations()[index]);
            name = method.getParameters()[index].getName();
        }
    }

    public static <T> Class<? extends Supplier<T>> compile(java.lang.reflect.Method method, ClassLoader parentClassLoader)
            throws ReflectiveOperationException
    {
        checkArgument((method.getModifiers() & STATIC.getModifier()) > 0);

        java.lang.reflect.Type methodReturnType = method.getGenericReturnType();

        List<TargetParameter> targetParameters = IntStream.range(0, method.getParameterCount()).boxed().map(i -> new TargetParameter(method, i)).collect(toImmutableList());

        java.lang.reflect.Type suppliedType;
        if (methodReturnType instanceof Class) {
            Class<?> methodReturnClass = (Class<?>) methodReturnType;
            if (methodReturnClass.isPrimitive()) {
                suppliedType = requireNonNull(ClassUtils.primitiveToWrapper(methodReturnClass));
            }
            else {
                suppliedType = methodReturnClass;
            }
        }
        else {
            throw new IllegalArgumentException("NYI");
        }

        ClassDefinition classDefinition = new ClassDefinition(
                a(PUBLIC, FINAL),
                CompilerUtils.makeClassName("DerivedSupplier__" + method.getDeclaringClass().getName() + "__" + method.getName()),
                type(Object.class),
                type(Supplier.class, fromReflectType(suppliedType)));

        targetParameters.forEach(p -> classDefinition.addField(a(PRIVATE, FINAL), p.name, p.parameterizedType));

        compileConstructor(classDefinition, targetParameters);

        throw new IllegalStateException();
    }

    private static void compileConstructor(ClassDefinition classDefinition, List<TargetParameter> targetParameters)
            throws ReflectiveOperationException
    {
        List<Parameter> constructorParameters = targetParameters.stream().map(p -> arg(p.name, p.parameterizedType)).collect(toImmutableList());
        MethodDefinition methodDefinition = classDefinition.declareConstructor(a(PUBLIC), constructorParameters);
        methodDefinition.declareAnnotation(Inject.class);

        for (int i = 0; i < targetParameters.size(); ++i) {
            TargetParameter targetParameter = targetParameters.get(i);
            for (Annotation annotation : targetParameter.annotations) {
                cloneParameterAnnotation(methodDefinition, i, annotation);
            }
        }

//        Scope scope = constructorDefinition.getScope();
//        BytecodeBlock body = constructorDefinition.getBody();
//        body
//                .getVariable(scope.getThis())
//                .invokeConstructor(Object.class);
//        for (int i = 0; i < params.size(); ++i) {
//            body
//                    .getVariable(scope.getThis())
//                    .getVariable(scope.getVariable(params.get(i).getName()))
//                    .putField(classDef.getFields().get(i));
//        }
//        body
//                .ret();
    }

    private static void cloneParameterAnnotation(MethodDefinition methodDefinition, int parameterIndex, Annotation annotation)
            throws ReflectiveOperationException
    {
        Class<?> annotationInterface = getOnlyElement(ImmutableList.copyOf(annotation.getClass().getInterfaces()));
        AnnotationDefinition annotationDefinition = methodDefinition.declareParameterAnnotation(annotationInterface, parameterIndex);
        for (java.lang.reflect.Method interfaceMethod : annotationInterface.getDeclaredMethods()) {
            String name = interfaceMethod.getName();
            Object value = interfaceMethod.invoke(annotation);

            java.lang.reflect.Method setValueMethod = AnnotationDefinition.class.getDeclaredMethod("setValue", String.class, value.getClass());
            setValueMethod.invoke(annotationDefinition, name, value);
        }
    }

    private static ParameterizedType fromReflectType(java.lang.reflect.Type type)
    {
        if (type instanceof Class) {
            return ParameterizedType.type((Class) type);
        }
        else if (type instanceof java.lang.reflect.ParameterizedType) {
            return ParameterizedType.type(
                    (Class) ((java.lang.reflect.ParameterizedType) type).getRawType(),
                    ImmutableList.copyOf(((java.lang.reflect.ParameterizedType) type).getActualTypeArguments()).stream().map(t -> fromReflectType(t)).collect(Collectors.toList()).toArray(new ParameterizedType[0]));
        }
        else {
            throw new IllegalArgumentException("NYI");
        }
    }
}
