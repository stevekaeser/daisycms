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
package org.outerj.daisy.books.publisher.impl.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * An appender intended to redirect logging to a PublicationLog.
 * Do not forget to clear the publicationlog from the thread afterwards.
 * (This situation screams for an AOP approach, but I did not want to introduce the dependency)
 * 
 * When there is no publication log attached to the current thread, nothing will be logged
 */
public class PublicationLogAppender extends AppenderSkeleton {
    
    private static ThreadLocal<PublicationLog> publicationLogHolder = new ThreadLocal<PublicationLog>();
    
    public static void setPublicationLog(PublicationLog publicationLog) {
        publicationLogHolder.set(publicationLog);
    }

    @Override
    protected void append(LoggingEvent event) {
        PublicationLog pubLog = publicationLogHolder.get();
        if (pubLog == null)
            return;

        Level level = event.getLevel();
        String formattedEvent = getLayout().format(event);
        if (level.equals(Level.ALL)) {
            pubLog.info(formattedEvent);
            pubLog.error(formattedEvent);
        } else if (level.equals(Level.FATAL)
            || level.equals(Level.ERROR)
            || level.equals(Level.WARN)) {
            pubLog.error(formattedEvent);
        } else {
            pubLog.info(formattedEvent);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

}