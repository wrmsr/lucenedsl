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

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.wrmsr.search.dsl.DocSpecific;
import com.wrmsr.search.dsl.SearchScope;
import com.wrmsr.search.dsl.SearchScoped;

import java.util.List;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class FieldModule
        implements Module
{
    public static final List<String> STRING_FIELD_NAMES = ImmutableList.of("isbn", "title");

    @Override
    public void configure(Binder binder)
    {
        binder.bind(FieldSupplierService.class).to(FieldSupplierServiceImpl.class).in(SearchScoped.class);
        newSetBinder(binder, DocSpecific.class).addBinding().to(FieldSupplierServiceImpl.class).in(SearchScoped.class);
        binder.bind(FieldSupplierServiceImpl.class).in(SearchScoped.class);

        for (String stringFieldName : STRING_FIELD_NAMES) {
            binder.install(FieldSupplierWiring.createStringFieldSupplierModule(stringFieldName, SearchScoped.class));
        }
    }
}
