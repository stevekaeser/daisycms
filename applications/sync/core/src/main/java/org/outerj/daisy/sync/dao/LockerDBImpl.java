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
package org.outerj.daisy.sync.dao;

import org.outerj.daisy.sync.SystemState;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class LockerDBImpl implements Locker {
  private final String getLock = "select propvalue from sync_system where propname = 'lockstate'";
  private final String updateLock = "update sync_system set propvalue = ? where propname = 'lockstate' and propvalue = ?";
  
  private SimpleJdbcTemplate jdbcTemplate;
  
  public LockerDBImpl(SimpleJdbcTemplate template) {
    this.jdbcTemplate = template;
  }

  public boolean changeLockState(SystemState oldState, SystemState newState) {
    String oldvalue = new Integer(oldState.getValue()).toString();
    String newvalue = new Integer(newState.getValue()).toString();
    return jdbcTemplate.update(updateLock, new Object[]{newvalue, oldvalue}) == 1;
  }

  public SystemState getLockState() {
    String stateStringValue = jdbcTemplate.queryForObject(getLock, String.class, new Object[0]);
    return SystemState.ValueOf(Integer.parseInt(stateStringValue));    
  }

}
