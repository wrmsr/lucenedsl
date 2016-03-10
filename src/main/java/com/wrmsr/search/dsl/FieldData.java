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

import org.apache.lucene.index.AtomicReaderContext;

import java.io.IOException;

public class FieldData<T>
{
    private final AtomicReaderContext context;
    private final String fieldName;

    public FieldData(AtomicReaderContext context, String fieldName)
    {
        this.context = context;
        this.fieldName = fieldName;
    }

    public T get(Integer integer)
            throws IOException
    {
        return null;
    }
}
