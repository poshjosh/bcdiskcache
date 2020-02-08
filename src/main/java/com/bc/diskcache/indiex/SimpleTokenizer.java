/*
 * Copyright 2018 NUROX Ltd.
 *
 * Licensed under the NUROX Ltd Software License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.looseboxes.com/legal/licenses/software.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bc.diskcache.indiex;

import java.util.Arrays;
import java.util.Collection;

public class SimpleTokenizer implements CacheIndex.Tokenizer<String, String> {

    private final int minTokenSizeChars;

    private final Collection<String> forbidden;

    public SimpleTokenizer(int minTokenSizeChars) {
        this(minTokenSizeChars,
                Arrays.asList("the", "this", "that", "then", "those", "has", "have", "had",
                        "her", "hers", "him", "his", "them", "it", "its", "you", "i", "we",
                        "us", "my", "your", "yours", "our", "ours", "their", "theirs",
                        "for", "of"));
    }

    public SimpleTokenizer(int minTokenSizeChars, Collection<String> forbidden) {
        this.minTokenSizeChars = minTokenSizeChars;
        this.forbidden = java.util.Objects.requireNonNull(forbidden);
    }

    @Override
    public String [] tokenize(String text) {

        final String [] parts = text.split("\\s");

        final String [] buffer = new String[parts.length];

        int added = 0;

        for(int i=0; i<parts.length; i++) {

            String part = parts[i];

            if(part == null || (part = part.trim()).isEmpty()) {
                continue;
            }

            if(part.length() < minTokenSizeChars) {
                continue;
            }

            if(forbidden.contains(part) || forbidden.contains(part.toLowerCase())) {
                continue;
            }

            buffer[added] = part;

            ++added;
        }

        final String [] result = new String[added];

        System.arraycopy(buffer, 0, result, 0, added);

        return result;
    }
}
