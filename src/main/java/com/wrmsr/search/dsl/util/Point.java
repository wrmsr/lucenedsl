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
package com.wrmsr.search.dsl.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Point
{
    private final float latitude;
    private final float longitude;

    @JsonCreator
    public Point(
            @JsonProperty("latitude") float latitude,
            @JsonProperty("longitude") float longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @JsonProperty("latitude")
    public float getLatitude()
    {
        return latitude;
    }

    @JsonProperty("longitude")
    public float getLongitude()
    {
        return longitude;
    }
}
