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

public enum SystemState {
  IDLE (0), 
  EXT_UPDATE(1), 
  AWAITING_SYNC(2), 
  SYNCING(3);
  
  private static SystemState[] states = new SystemState[] {IDLE, EXT_UPDATE, AWAITING_SYNC, SYNCING};
  
  private int value;
  
  SystemState(int val) {
    this.value = val;
  }
  
  public int getValue() {
    return value;
  }
  
  public static SystemState ValueOf(int value) {
    return states[value];
  }
}
