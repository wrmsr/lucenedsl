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
import com.facebook.presto.bytecode.BytecodeBlock;
import com.facebook.presto.bytecode.ClassDefinition;
import com.facebook.presto.bytecode.CompilerUtils;
import com.facebook.presto.bytecode.DynamicClassLoader;
import com.facebook.presto.bytecode.FieldDefinition;
import com.facebook.presto.bytecode.MethodDefinition;
import com.facebook.presto.bytecode.Parameter;
import com.facebook.presto.bytecode.ParameterizedType;
import com.facebook.presto.bytecode.Scope;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.ClassUtils;

import javax.inject.Inject;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.facebook.presto.bytecode.Access.FINAL;
import static com.facebook.presto.bytecode.Access.PRIVATE;
import static com.facebook.presto.bytecode.Access.PUBLIC;
import static com.facebook.presto.bytecode.Access.STATIC;
import static com.facebook.presto.bytecode.Access.a;
import static com.facebook.presto.bytecode.CompilerUtils.defineClass;
import static com.facebook.presto.bytecode.Parameter.arg;
import static com.facebook.presto.bytecode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.wrmsr.search.dsl.util.ImmutableCollectors.toImmutableList;
import static com.wrmsr.search.dsl.util.ImmutableCollectors.toImmutableMap;
import static java.util.Objects.requireNonNull;

/*
TODO:
- Allowing passing {name: class} map for Supplier conversion
- unbox errwhere
*/
public final class DerivedSuppliers
{
    private DerivedSuppliers()
    {
    }

    private static final class TargetParameter
    {
        private final java.lang.reflect.Type type;
        private final java.lang.reflect.Type boxedType;
        private final ParameterizedType parameterizedType;
        private final ParameterizedType parameterizedBoxedType;
        private final List<Annotation> annotations;
        private final String name;

        public TargetParameter(java.lang.reflect.Method method, int index)
        {
            type = method.getGenericParameterTypes()[index];
            boxedType = boxType(type);
            parameterizedType = fromReflectType(type);
            parameterizedBoxedType = fromReflectType(boxedType);
            annotations = ImmutableList.copyOf(method.getParameterAnnotations()[index]);
            name = method.getParameters()[index].getName();
        }
    }

    public static <T> Class<? extends Supplier<T>> compile(java.lang.reflect.Method target, ClassLoader parentClassLoader)
            throws ReflectiveOperationException
    {
        checkArgument((target.getModifiers() & STATIC.getModifier()) > 0);
        List<TargetParameter> targetParameters = IntStream.range(0, target.getParameterCount()).boxed().map(i -> new TargetParameter(target, i)).collect(toImmutableList());

        java.lang.reflect.Type targetReturnType = target.getGenericReturnType();
        java.lang.reflect.Type suppliedType = boxType(targetReturnType);
        checkArgument(suppliedType instanceof Class);

        ClassDefinition classDefinition = new ClassDefinition(
                a(PUBLIC, FINAL),
                CompilerUtils.makeClassName("DerivedSupplier__" + target.getDeclaringClass().getName() + "__" + target.getName()),
                type(Object.class),
                type(Supplier.class, fromReflectType(suppliedType)));

        targetParameters.forEach(p -> classDefinition.addField(a(PRIVATE, FINAL), p.name, type(Supplier.class, p.parameterizedBoxedType)));
        Map<String, FieldDefinition> classFieldDefinitionMap = classDefinition.getFields().stream().collect(toImmutableMap(f -> f.getName(), f -> f));

        compileConstructor(classDefinition, classFieldDefinitionMap, targetParameters);
        compileGetter(classDefinition, classFieldDefinitionMap, target, targetParameters);

        Class clazz = defineClass(classDefinition, Object.class, ImmutableMap.of(), new DynamicClassLoader(parentClassLoader));
        return clazz;
    }

    private static void compileConstructor(ClassDefinition classDefinition, Map<String, FieldDefinition> classFieldDefinitionMap, List<TargetParameter> targetParameters)
            throws ReflectiveOperationException
    {
        List<Parameter> constructorParameters = targetParameters.stream().map(p -> arg(p.name, classFieldDefinitionMap.get(p.name).getType())).collect(toImmutableList());
        MethodDefinition methodDefinition = classDefinition.declareConstructor(a(PUBLIC), constructorParameters);
        methodDefinition.declareAnnotation(Inject.class);

        for (int i = 0; i < targetParameters.size(); ++i) {
            TargetParameter targetParameter = targetParameters.get(i);
            for (Annotation annotation : targetParameter.annotations) {
                cloneParameterAnnotation(methodDefinition, i, annotation);
            }
        }

        Scope scope = methodDefinition.getScope();
        BytecodeBlock body = methodDefinition.getBody();

        body
                .getVariable(scope.getThis())
                .invokeConstructor(Object.class);
        for (TargetParameter targetParameter : targetParameters) {
            body
                    .getVariable(scope.getThis())
                    .getVariable(scope.getVariable(targetParameter.name))
                    .putField(classFieldDefinitionMap.get(targetParameter.name));
        }
        body
                .ret();
    }

    private static void compileGetter(ClassDefinition classDefinition, Map<String, FieldDefinition> classFieldDefinitionMap, java.lang.reflect.Method target, List<TargetParameter> targetParameters)
    {
        java.lang.reflect.Type targetReturnType = target.getGenericReturnType();
        Class<?> targetReturnClass = (Class<?>) targetReturnType;
        Class<?> boxedTargetReturnClass = requireNonNull(ClassUtils.primitiveToWrapper(targetReturnClass));
        checkArgument(targetReturnClass == boxedTargetReturnClass || targetReturnClass.isPrimitive());

        MethodDefinition methodDefinition = classDefinition.declareMethod(a(PUBLIC, FINAL), "get", type(Object.class));
        methodDefinition.declareAnnotation(Override.class);
        Scope scope = methodDefinition.getScope();
        BytecodeBlock body = methodDefinition.getBody();

        if (targetReturnClass.isPrimitive()) {
            body
                    .newObject(boxedTargetReturnClass)
                    .dup();
        }
        for (TargetParameter targetParameter : targetParameters) {
            body
                    .getVariable(scope.getThis())
                    .getField(classFieldDefinitionMap.get(targetParameter.name))
                    .invokeInterface(Supplier.class, "get", Object.class)
                    .checkCast(targetParameter.parameterizedBoxedType);
            // lol
            if (targetParameter.type == boolean.class) {
                body.invokeInterface(Boolean.class, "booleanValue", boolean.class);
            }
            else if (targetParameter.type == byte.class) {
                body.invokeInterface(Byte.class, "byteValue", byte.class);
            }
            else if (targetParameter.type == short.class) {
                body.invokeInterface(Short.class, "shortValue", short.class);
            }
            else if (targetParameter.type == int.class) {
                body.invokeInterface(Integer.class, "intValue", int.class);
            }
            else if (targetParameter.type == long.class) {
                body.invokeInterface(Long.class, "longValue", long.class);
            }
            else if (targetParameter.type == float.class) {
                body.invokeInterface(Float.class, "floatValue", float.class);
            }
            else if (targetParameter.type == double.class) {
                body.invokeInterface(Double.class, "doubleValue", double.class);
            }
            else {
                checkState(!(targetParameter.type instanceof Class && ((Class) targetParameter.type).isPrimitive()));
            }
        }
        body.invokeStatic(target);
        if (targetReturnClass.isPrimitive()) {
            body
                    .invokeConstructor(boxedTargetReturnClass, targetReturnClass);
        }
        body.retObject();
    }

    private static void cloneParameterAnnotation(MethodDefinition methodDefinition, int parameterIndex, Annotation annotation)
            throws ReflectiveOperationException
    {
        Class<?> annotationInterface = getOnlyElement(ImmutableList.copyOf(annotation.getClass().getInterfaces()));
        AnnotationDefinition annotationDefinition = methodDefinition.declareParameterAnnotation(annotationInterface, parameterIndex);
        for (java.lang.reflect.Method interfaceMethod : annotationInterface.getDeclaredMethods()) {
            String name = interfaceMethod.getName();
            Object value = interfaceMethod.invoke(annotation);

            // :|
            java.lang.reflect.Method setValueMethod = AnnotationDefinition.class.getDeclaredMethod("setValue", String.class, value.getClass());
            setValueMethod.invoke(annotationDefinition, name, value);
        }
    }

    private static java.lang.reflect.Type boxType(java.lang.reflect.Type type)
    {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isPrimitive()) {
                return requireNonNull(ClassUtils.primitiveToWrapper(clazz));
            }
        }
        return type;
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
