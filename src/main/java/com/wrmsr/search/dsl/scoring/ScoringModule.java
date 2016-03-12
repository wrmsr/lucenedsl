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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.wrmsr.search.dsl.SearchScoped;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.invoke.MethodHandles.lookup;

public class ScoringModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar("weird_score")).to(ComputeWeirdScore.class).in(SearchScoped.class);

        CHALLENGE ACCEPTED: do this without bytecode generation
        try {
            List<Method> methods = ImmutableList.copyOf(Computations.class.getDeclaredMethods());
            for (Method method : methods) {
                AnnotatedType returnType = method.getAnnotatedReturnType();
                List<AnnotatedType> paramTypes = ImmutableList.copyOf(method.getAnnotatedParameterTypes());
                checkState(returnType.isAnnotationPresent(ScoreVar.class));
                checkState(paramTypes.stream().allMatch(t -> t.isAnnotationPresent(ScoreVar.class)));
                lookup().unreflect(method)
            }
        }
        catch (ReflectiveOperationException e) {
            throw Throwables.propagate(e);
        }
    }
}
