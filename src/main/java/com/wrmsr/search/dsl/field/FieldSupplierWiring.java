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
package com.wrmsr.search.dsl.field;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.wrmsr.search.dsl.scoring.ScoreVars;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

final class FieldSupplierWiring
{
    private FieldSupplierWiring()
    {
    }

    private static final class StringFieldSupplierImpl
            implements FieldSupplier<String>
    {
        private final Supplier<String> supplier;

        @Inject
        public StringFieldSupplierImpl(FieldSupplierService service, FieldName name)
        {
            supplier = service.getStringFieldSupplier(name.getValue());
        }

        @Override
        public String get()
        {
            return supplier.get();
        }
    }

    private static final class StringFieldSupplierModule
            extends PrivateModule
    {
        private final String name;
        private final Class<? extends Annotation> scopeAnnotation;

        public StringFieldSupplierModule(String name, Class<? extends Annotation> scopeAnnotation)
        {
            this.name = name;
            this.scopeAnnotation = scopeAnnotation;
        }

        @Override
        protected void configure()
        {
            bind(FieldName.class).toInstance(new FieldName(name));
            bind(new TypeLiteral<Supplier<String>>() {}).annotatedWith(ScoreVars.scoreVar(name)).to(StringFieldSupplierImpl.class).in(scopeAnnotation);
            expose(new TypeLiteral<Supplier<String>>() {}).annotatedWith(ScoreVars.scoreVar(name));
        }
    }

    public static Module createStringFieldSupplierModule(String name, Class<? extends Annotation> scopeAnnotation)
    {
        return new StringFieldSupplierModule(name, scopeAnnotation);
    }
}
