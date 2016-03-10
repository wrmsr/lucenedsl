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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BooleanQueryNode
        extends QueryNode
{
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ShouldClause.class, name = "should"),
            @JsonSubTypes.Type(value = MustClause.class, name = "must"),
            @JsonSubTypes.Type(value = MustNotClause.class, name = "must_not"),
    })
    public static abstract class Clause
    {
        private final QueryNode query;

        protected Clause(QueryNode query)
        {
            this.query = query;
        }

        @JsonProperty("query")
        public QueryNode getQuery()
        {
            return query;
        }
    }

    public static final class ShouldClause
            extends Clause
    {
        @JsonCreator
        public ShouldClause(
                @JsonProperty("query") QueryNode query)
        {
            super(query);
        }
    }

    public static final class MustClause
            extends Clause
    {
        @JsonCreator
        public MustClause(
                @JsonProperty("query") QueryNode query)
        {
            super(query);
        }
    }

    public static final class MustNotClause
            extends Clause
    {
        @JsonCreator
        public MustNotClause(
                @JsonProperty("query") QueryNode query)
        {
            super(query);
        }
    }

    private List<Clause> clauses;
    private int minimumNumberShouldMatch;

    @JsonCreator
    public BooleanQueryNode(
            @JsonProperty("clauses") List<Clause> clauses,
            @JsonProperty("minimum_number_should_match") int minimumNumberShouldMatch)
    {
        this.clauses = requireNonNull(clauses);
        this.minimumNumberShouldMatch = minimumNumberShouldMatch;
    }

    @JsonProperty("clauses")
    public List<Clause> getClauses()
    {
        return clauses;
    }

    @JsonProperty("minumum_number_should_match")
    public int getminimumNumberShouldMatch()
    {
        return minimumNumberShouldMatch;
    }

    @Override
    public <C, R> R accept(QueryNodeVisitor<C, R> visitor, C context)
    {
        return visitor.visitBooleanQueryNode(this, context);
    }
}
