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
package org.outerj.daisy.repository.serverimpl;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.jdbcutil.JdbcHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.CharArrayWriter;
import java.io.IOException;

public class EventHelper {
    private LocalRepositoryManager.Context context;
    private JdbcHelper jdbcHelper;

    public EventHelper(LocalRepositoryManager.Context context, JdbcHelper jdbcHelper) {
        this.context = context;
        this.jdbcHelper = jdbcHelper;
    }

    /**
     * Creates an event record, this event will then be picked up by the event dispatcher component
     * which will forward it to JMS. The event record should usually be created in one database
     * transaction together with the changes causing the event.
     */
    public void createEvent(XmlObject eventDescription, String eventName, Connection conn) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into events(seqnr, message_type, message) values(?,?,?)");
            stmt.setLong(1, context.getNextEventId());
            stmt.setString(2, eventName);
            stmt.setString(3, eventToString(eventDescription));
            stmt.execute();
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }

    }

    private String eventToString(XmlObject xmlObject) throws RepositoryException {
        CharArrayWriter writer = new CharArrayWriter(5000);
        try {
            xmlObject.save(writer);
        } catch (IOException e) {
            throw new RepositoryException("Error serializing event description.", e);
        }
        return writer.toString();
    }

}
