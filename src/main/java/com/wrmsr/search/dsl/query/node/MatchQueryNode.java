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
package com.wrmsr.search.dsl.query.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Objects.requireNonNull;

public final class MatchQueryNode
        extends QueryNode
{
    private final String field;
    private final String text;

    @JsonCreator
    public MatchQueryNode(
            @JsonProperty("field") String field,
            @JsonProperty("text") String text)
    {
        this.field = requireNonNull(field);
        this.text = requireNonNull(text);
    }

    @JsonProperty("field")
    public String getField()
    {
        return field;
    }

    @JsonProperty("text")
    public String getText()
    {
        return text;
    }

    @Override
    public <C, R> R accept(QueryNodeVisitor<C, R> visitor, C context)
    {
        return visitor.visitMatchQueryNode(this, context);
    }
}
