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
package com.wrmsr.search.dsl.lucene;

import com.wrmsr.search.dsl.query.term.BooleanQueryTerm;
import com.wrmsr.search.dsl.query.term.NumberQueryTerm;
import com.wrmsr.search.dsl.query.term.QueryTerm;
import com.wrmsr.search.dsl.query.term.StringQueryTerm;
import org.apache.lucene.index.Term;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class QueryTermRenderer
{
    public Term renderQueryTerm(String field, QueryTerm queryTerm)
    {
        requireNonNull(field);
        final String string;
        if (queryTerm instanceof StringQueryTerm) {
            string = (String) queryTerm.getValue();
        }
        else if (queryTerm instanceof NumberQueryTerm || queryTerm instanceof BooleanQueryTerm) {
            string = queryTerm.getValue().toString();
        }
        else {
            throw new IllegalArgumentException(Objects.toString(queryTerm));
        }
        return new Term(field, string);
    }
}
