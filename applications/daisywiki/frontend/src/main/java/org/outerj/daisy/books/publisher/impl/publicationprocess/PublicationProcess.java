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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

public class PublicationProcess {
    PublicationProcessTask[] tasks;

    public PublicationProcess(PublicationProcessTask[] tasks) {
        this.tasks = tasks.clone();
    }

    public void run(PublicationContext publicationContext) throws Exception {
        for (int i = 0; i < tasks.length; i++) {
            tasks[i].run(publicationContext);
        }
    }

    public boolean buildsPackage() {
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] instanceof ZipTask)
                return true;
        }
        return false;
    }
}
