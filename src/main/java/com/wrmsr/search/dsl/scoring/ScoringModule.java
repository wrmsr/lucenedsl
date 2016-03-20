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
import com.facebook.presto.bytecode.Variable;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.wrmsr.search.dsl.SearchScoped;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.facebook.presto.bytecode.Access.FINAL;
import static com.facebook.presto.bytecode.Access.PRIVATE;
import static com.facebook.presto.bytecode.Access.PUBLIC;
import static com.facebook.presto.bytecode.Access.STATIC;
import static com.facebook.presto.bytecode.Access.a;
import static com.facebook.presto.bytecode.CompilerUtils.defineClass;
import static com.facebook.presto.bytecode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.invoke.MethodHandles.lookup;

public class ScoringModule
        implements Module
{
    public static class Fuck
    {
        @ScoreVar("isbn_length")
        public static float computeIsbnLength(@ScoreVar("isbn") String isbn)
        {
            return (float) isbn.length();
        }
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar("weird_score")).to(ComputeWeirdScore.class).in(SearchScoped.class);

        // CHALLENGE ACCEPTED: do this without bytecode generation
        try {
            List<Method> methods = ImmutableList.copyOf(Fuck.class.getDeclaredMethods());
            for (Method method : methods) {
                java.lang.reflect.AnnotatedType returnType = method.getAnnotatedReturnType();
                List<java.lang.reflect.Parameter> params = ImmutableList.copyOf(method.getParameters());
                List<java.lang.reflect.AnnotatedType> paramTypes = ImmutableList.copyOf(method.getAnnotatedParameterTypes());
                checkState(params.size() == paramTypes.size());
                // HURR its a method anno
                // also its fucking getParameterAnnotations not getAnnotatedParameters silly me.
                // checkState(returnType.isAnnotationPresent(ScoreVar.class));
                // checkState(paramTypes.stream().allMatch(pt -> pt.isAnnotationPresent(ScoreVar.class)));
                List<ParameterizedType> paramPts = paramTypes.stream().map(pt -> fromReflectType(pt.getType())).collect(Collectors.toList());

                // TODO parameterized params, primitive params

                ClassDefinition classDefinition = new ClassDefinition(
                        a(PUBLIC, FINAL),
                        CompilerUtils.makeClassName("get"),
                        type(Object.class));
                classDefinition.declareDefaultConstructor(a(PRIVATE));

                List<Parameter> parameters = IntStream.range(0, params.size()).boxed()
                        .map(i -> Parameter.arg(params.get(i).getName(), ParameterizedType.type(Supplier.class, paramPts.get(i))))
                        .collect(Collectors.toList());

                MethodDefinition methodDefinition = classDefinition.declareMethod(a(PUBLIC, STATIC), "get", type((Class) returnType.getType()), parameters);

                Scope scope = methodDefinition.getScope();
                BytecodeBlock body = methodDefinition.getBody();
                for (int i = 0; i < params.size(); ++i) {
                    Variable arg = scope.getVariable(params.get(i).getName());
                    body.getVariable(arg);
                    body.invokeInterface(Supplier.class, "get", params.get(i).getType());
                    // body.invokeInterface(Float.class, "floatValue", float.class);
                }
                body.invokeStatic(method);
                body.retFloat();

                Class<?> cls = defineClass(classDefinition, Object.class, ImmutableMap.of(), new DynamicClassLoader(ScoringModule.class.getClassLoader()));
                System.out.println(cls);
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
