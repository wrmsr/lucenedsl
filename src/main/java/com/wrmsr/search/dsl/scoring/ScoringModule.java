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
package com.wrmsr.search.dsl.scoring;

import com.facebook.presto.bytecode.BytecodeBlock;
import com.facebook.presto.bytecode.ClassDefinition;
import com.facebook.presto.bytecode.CompilerUtils;
import com.facebook.presto.bytecode.DynamicClassLoader;
import com.facebook.presto.bytecode.MethodDefinition;
import com.facebook.presto.bytecode.Parameter;
import com.facebook.presto.bytecode.ParameterizedType;
import com.facebook.presto.bytecode.Scope;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.wrmsr.search.dsl.SearchScoped;
import com.wrmsr.search.dsl.util.DerivedSuppliers;

import javax.inject.Inject;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.facebook.presto.bytecode.Access.FINAL;
import static com.facebook.presto.bytecode.Access.PRIVATE;
import static com.facebook.presto.bytecode.Access.PUBLIC;
import static com.facebook.presto.bytecode.Access.a;
import static com.facebook.presto.bytecode.CompilerUtils.defineClass;
import static com.facebook.presto.bytecode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkState;

public class ScoringModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar("weird_score")).to(ComputeWeirdScore.class).in(SearchScoped.class);

        // CHALLENGE ACCEPTED: do this without bytecode generation
        try {
            List<Method> methods = ImmutableList.copyOf(Computations.class.getDeclaredMethods());
            for (Method method : methods) {
                java.lang.reflect.AnnotatedType returnType = method.getAnnotatedReturnType();
                List<java.lang.reflect.Parameter> params = ImmutableList.copyOf(method.getParameters());
                List<java.lang.reflect.AnnotatedType> paramTypes = ImmutableList.copyOf(method.getAnnotatedParameterTypes());
                checkState(params.size() == paramTypes.size());
                ScoreVar outVar = Arrays.stream(method.getDeclaredAnnotations())
                        .filter(ScoreVar.class::isInstance)
                        .findFirst()
                        .map(ScoreVar.class::cast)
                        .get();
                Annotation[][] paramAnnos = method.getParameterAnnotations();
                List<ScoreVar> inVars = Arrays.stream(paramAnnos).map(as -> Arrays.stream(as).filter(ScoreVar.class::isInstance).findFirst().map(ScoreVar.class::cast).get()).collect(Collectors.toList());
                // HURR its a method anno
                // also its fucking getParameterAnnotations not getAnnotatedParameters silly me.
                // checkState(returnType.isAnnotationPresent(ScoreVar.class));
                // checkState(paramTypes.stream().allMatch(pt -> pt.isAnnotationPresent(ScoreVar.class)));
                List<ParameterizedType> paramPts = paramTypes.stream().map(pt -> fromReflectType(pt.getType())).collect(Collectors.toList());

                DerivedSuppliers.compile(method, ScoringModule.class.getClassLoader());

                // TODO parameterized params, primitive params

                Type suppType;
                if (method.getReturnType().isPrimitive()) {
                    suppType = method.getReturnType();
                }
                else {
                    suppType = method.getGenericReturnType();
                }

                ClassDefinition classDef = new ClassDefinition(
                        a(PUBLIC, FINAL),
                        CompilerUtils.makeClassName("get"),
                        type(Object.class),
                        // FIXME box primitives to Supplier typeparams
                        type(Supplier.class, fromReflectType(suppType)));
                classDef.declareAnnotation(ScoreVar.class).setValue("value", outVar.value());

                List<Parameter> parameters = IntStream.range(0, params.size()).boxed()
                        .map(i -> Parameter.arg(params.get(i).getName(), ParameterizedType.type(Supplier.class, paramPts.get(i))))
                        .collect(Collectors.toList());
                parameters.forEach(p -> classDef.addField(a(PRIVATE, FINAL), p.getName(), p.getType()));

                MethodDefinition methodDef = classDef.declareConstructor(a(PUBLIC), parameters);
                for (int i = 0; i < params.size(); ++i) {
                    methodDef.declareParameterAnnotation(ScoreVar.class, i).setValue("value", inVars.get(i).value());
                }
                methodDef.declareAnnotation(Inject.class);
                Scope scope = methodDef.getScope();
                BytecodeBlock body = methodDef.getBody();
                body
                        .getVariable(scope.getThis())
                        .invokeConstructor(Object.class);
                for (int i = 0; i < params.size(); ++i) {
                    body
                            .getVariable(scope.getThis())
                            .getVariable(scope.getVariable(params.get(i).getName()))
                            .putField(classDef.getFields().get(i));
                }
                body
                        .ret();

                methodDef = classDef.declareMethod(a(PUBLIC, FINAL), "get", type(Object.class)); //type((Class) returnType.getType()));
                methodDef.declareAnnotation(Override.class);
                scope = methodDef.getScope();
                body = methodDef.getBody();
                if (method.getReturnType() == float.class) {
                    body
                            .newObject(Float.class)
                            .dup();
                }
                for (int i = 0; i < params.size(); ++i) {
                    body
                            .getVariable(scope.getThis())
                            .getField(classDef.getFields().get(i))
                            .invokeInterface(Supplier.class, "get", Object.class)
                            .checkCast(params.get(i).getType());
                    if (params.get(i).getType() == float.class) {
                        body.invokeInterface(Float.class, "floatValue", float.class);
                    }
                }
                body.invokeStatic(method);
                if (method.getReturnType() == float.class) {
                    body
                            .invokeConstructor(Float.class, float.class);
                }
                body.retObject();

                Class cls = defineClass(classDef, Object.class, ImmutableMap.<Long, MethodHandle>of(), new DynamicClassLoader(ScoringModule.class.getClassLoader()));

                binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar(outVar.value())).to(cls).in(SearchScoped.class);
            }
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static ParameterizedType fromReflectType(java.lang.reflect.Type type)
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
            throw new IllegalArgumentException();
        }
    }
}
