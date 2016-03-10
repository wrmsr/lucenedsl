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
import com.wrmsr.search.dsl.query.term.QueryTerm;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public final class RangeQueryNode
        extends QueryNode
{
    private final String field;
    private final QueryTerm lower;
    private final QueryTerm upper;
    private final boolean includeLower;
    private final boolean includeUpper;

    @JsonCreator
    public RangeQueryNode(
            @JsonProperty("field") String field,
            @JsonProperty("lower") QueryTerm lower,
            @JsonProperty("upper") QueryTerm upper,
            @JsonProperty("include_lower") boolean includeLower,
            @JsonProperty("include_upper") boolean includeUpper)
    {
        requireNonNull(lower);
        requireNonNull(upper);
        checkArgument(lower.getClass() == upper.getClass());
        this.field = field;
        this.lower = lower;
        this.upper = upper;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
    }

    @JsonProperty("field")
    public String getField()
    {
        return field;
    }

    @JsonProperty("lower")
    public QueryTerm getLower()
    {
        return lower;
    }

    @JsonProperty("upper")
    public QueryTerm getUpper()
    {
        return upper;
    }

    @JsonProperty("include_lower")
    public boolean isIncludeLower()
    {
        return includeLower;
    }

    @JsonProperty("include_upper")
    public boolean isIncludeUpper()
    {
        return includeUpper;
    }

    @Override
    public <C, R> R accept(QueryNodeVisitor<C, R> visitor, C context)
    {
        return visitor.visitRangeQueryNode(this, context);
    }
}
