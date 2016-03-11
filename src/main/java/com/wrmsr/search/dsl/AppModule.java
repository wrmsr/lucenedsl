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
package com.wrmsr.search.dsl;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.wrmsr.search.dsl.scoring.ScoringModule;
import com.wrmsr.search.dsl.utils.ScopeListeners;
import org.apache.lucene.search.IndexSearcher;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class AppModule
        implements Module
{

    @Override
    public void configure(Binder binder)
    {
        binder.install(new ScoringModule());

        binder.bind(SearchService.class).to(SearchServiceImpl.class).asEagerSingleton();

        SearchScope searchScope = new SearchScope();
        binder.bindScope(SearchScoped.class, searchScope);
        binder.bind(SearchScope.class).toInstance(searchScope);
        binder.bind(new TypeLiteral<ScopeListeners<SearchScope>>() {}).asEagerSingleton();

        newSetBinder(binder, DocSpecific.class);

        binder.bind(IndexSearcher.class).toProvider(SearchScope.<IndexSearcher>seededKeyProvider()).in(SearchScoped.class);
        binder.bind(Searcher.class).to(SearcherImpl.class).in(SearchScoped.class);

        binder.bind(FieldSupplierService.class).to(FieldSupplierServiceImpl.class).in(SearchScoped.class);
        newSetBinder(binder, DocSpecific.class).addBinding().to(FieldSupplierServiceImpl.class).in(SearchScoped.class);

        /*
        List<String> stringFieldNames = ImmutableList.of("isbn", "title");

        binder.bind(new TypeLiteral<ScoreVarSupplier<String>>() {}).annotatedWith(ScoreVars.scoreVar("title")).toProvider(new Provider<ScoreVarSupplier<String>>() {
            private final FieldSupplierServiceImpl fieldSupplierService;

            @Override
            public ScoreVarSupplier<String> get()
            {
                return null;
            }
        })
        */
    }
}
