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

import com.google.inject.Inject;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ComputeWeirdScore
    implements Supplier<Float>
{
    private final Supplier<String> title;
    private final Supplier<String> isbn;

    @Inject
    public ComputeWeirdScore(
            @ScoreVar("title") Supplier<String> title,
            @ScoreVar("isbn") Supplier<String> isbn)
    {
        this.title = requireNonNull(title);
        this.isbn = requireNonNull(isbn);
    }

    @Override
    public Float get()
    {
        return (float) (requireNonNull(title.get()).length() + requireNonNull(isbn.get()).length());
    }
}
