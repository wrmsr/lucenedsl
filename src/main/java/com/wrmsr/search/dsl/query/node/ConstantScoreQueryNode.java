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

public final class ConstantScoreQueryNode
        extends QueryNode
{
    private final QueryNode query;
    private final float boost;

    @JsonCreator
    public ConstantScoreQueryNode(
            @JsonProperty("query") QueryNode query,
            @JsonProperty("boost") float boost)
    {
        this.query = requireNonNull(query);
        this.boost = boost;
    }

    @JsonProperty("query")
    public QueryNode getQuery()
    {
        return query;
    }

    @JsonProperty("boost")
    public float getBoost()
    {
        return boost;
    }

    @Override
    public <C, R> R accept(QueryNodeVisitor<C, R> visitor, C context)
    {
        return visitor.visitConstantScoreQueryNode(this, context);
    }
}
