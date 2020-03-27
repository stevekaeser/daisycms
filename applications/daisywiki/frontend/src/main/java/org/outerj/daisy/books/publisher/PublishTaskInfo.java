/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.books.publisher;

public class PublishTaskInfo {
    private final String taskId;
    private final String bookInstanceName;
    private final long startingUserId;
    private final String startingUser;
    private final String state[];
    private final String started;

    public PublishTaskInfo(String taskId, String bookInstanceName, long startingUserId, String startingUser, String[] state, String started) {
        this.taskId = taskId;
        this.bookInstanceName = bookInstanceName;
        this.startingUserId = startingUserId;
        this.startingUser = startingUser;
        this.state = state;
        this.started = started;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getBookInstanceName() {
        return bookInstanceName;
    }

    public long getStartingUserId() {
        return startingUserId;
    }

    public String getStartingUser() {
        return startingUser;
    }

    public String[] getState() {
        return state;
    }

    public String getStarted() {
        return started;
    }
}
