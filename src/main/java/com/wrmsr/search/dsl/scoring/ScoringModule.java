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
import com.wrmsr.search.dsl.util.DerivedSuppliers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ScoringModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar("weird_score")).to(ComputeWeirdScore.class).in(SearchScoped.class);

        try {
            List<Method> methods = ImmutableList.copyOf(Computations.class.getDeclaredMethods());
            for (Method method : methods) {
                Optional<ScoreVar> suppliedScoreVar = Arrays.stream(method.getDeclaredAnnotations()).filter(ScoreVar.class::isInstance).findFirst().map(ScoreVar.class::cast);
                if (suppliedScoreVar.isPresent()) {
                    Class supplierClazz = DerivedSuppliers.compile(method, ScoringModule.class.getClassLoader());
                    binder.bind(new TypeLiteral<Supplier<Float>>() {}).annotatedWith(ScoreVars.scoreVar(suppliedScoreVar.get().value())).to(supplierClazz).in(SearchScoped.class);
                }
            }
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
