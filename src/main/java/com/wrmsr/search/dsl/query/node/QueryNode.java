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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanQueryNode.class, name = "boolean"),
        @JsonSubTypes.Type(value = MatchAllQueryNode.class, name = "match_all"),
        @JsonSubTypes.Type(value = TermQueryNode.class, name = "term"),
        @JsonSubTypes.Type(value = MatchQueryNode.class, name = "match"),
        @JsonSubTypes.Type(value = ConstantScoreQueryNode.class, name = "constant_score"),
        @JsonSubTypes.Type(value = BoostedQueryNode.class, name = "boosted"),
        @JsonSubTypes.Type(value = RangeQueryNode.class, name = "range"),
})
public abstract class QueryNode
{
    public abstract <C, R> R accept(QueryNodeVisitor<C, R> visitor, C context);
}
