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

import com.google.inject.Binder;
import com.google.inject.Key;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScoreVars
{
    private ScoreVars()
    {
    }

    private static final class ScoreVarImpl
            implements ScoreVar, Serializable
    {
        private final String value;

        public ScoreVarImpl(String value)
        {
            this.value = checkNotNull(value, "value");
        }

        public String value()
        {
            return this.value;
        }

        public int hashCode()
        {
            // This is specified in java.lang.Annotation.
            return (127 * "value".hashCode()) ^ value.hashCode();
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof ScoreVar)) {
                return false;
            }

            ScoreVar other = (ScoreVar) o;
            return value.equals(other.value());
        }

        public String toString()
        {
            return "@" + ScoreVar.class.getName() + "(value=" + value + ")";
        }

        public Class<? extends Annotation> annotationType()
        {
            return ScoreVar.class;
        }

        private static final long serialVersionUID = 0;
    }

    public static ScoreVar scoreVar(String name)
    {
        return new ScoreVarImpl(name);
    }

    public static void bindProperties(Binder binder, Map<String, String> properties)
    {
        binder = binder.skipSources(ScoreVars.class);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            binder.bind(Key.get(String.class, new ScoreVarImpl(key))).toInstance(value);
        }
    }

    public static void bindProperties(Binder binder, Properties properties)
    {
        binder = binder.skipSources(ScoreVars.class);
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
            String propertyScoreVar = (String) e.nextElement();
            String value = properties.getProperty(propertyScoreVar);
            binder.bind(Key.get(String.class, new ScoreVarImpl(propertyScoreVar))).toInstance(value);
        }
    }
}
