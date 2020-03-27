/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
 *
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
package org.outerj.daisy.sync;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.outerj.daisy.repository.VariantKey;

public class VariantHelper {
  private static Logger logger = Logger.getLogger("org.outerj.daisy.sync");

  public static VariantKey extractVariantKey(String key) {
    String regex = "(.*?)@(.*?):(.*?)";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(key);
    if (matcher.matches() && matcher.groupCount() == 3) {
      return new VariantKey(matcher.group(1), Long.parseLong(matcher.group(2)), Long.parseLong(matcher.group(3)));
    } else {
      logger.warning("Could not convert value : " + key + " to a variant key");
      return null;
    }
  }

  public static String variantKeyToString (VariantKey variantKey) {
    return variantKey.getDocumentId() + "@" + variantKey.getBranchId() + ":" + variantKey.getLanguageId();
  }

}
