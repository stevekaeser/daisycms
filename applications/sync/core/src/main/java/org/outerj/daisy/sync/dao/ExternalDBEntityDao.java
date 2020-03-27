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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class ExternalDBEntityDao implements ExternalEntityDao {
    private final String entitySql = "select last_modified from entity where entity_name = ? and ext_id = ?";

    private final String attributeSql = "select attribute_name, value from entity join entity_attribute on entity.id = entity_attribute.entity_id where entity_name = ? and ext_id = ?";

    private final String getAssociatedEntitiesSql = "select entity.ext_id from entity join entity_attribute as attr on entity.id = attr.entity_id where entity_name = ? and attr.attribute_name = ? and attr.value = ?";

    private SimpleJdbcTemplate jdbcTemplate;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public ExternalDBEntityDao(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> getEntityIds(final String entityName) {
        String sql = "select ext_id from entity where entity_name = ?";

        ParameterizedRowMapper<Long> rowMapper = new ParameterizedRowMapper<Long>() {

            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong("ext_id");
            }

        };

        return jdbcTemplate.query(sql, rowMapper, new Object[] { entityName });
    }

    public List<Entity> getEntity(final String entityName, final long externalId) {
        List<Entity> entities = new ArrayList<Entity>();

        ParameterizedRowMapper<Entity> entityMapper = new ParameterizedRowMapper<Entity>() {
            public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
                Entity entity = new EntityImpl();
                entity.setName(entityName);
                entity.setExternalId(externalId);
                entity.setExternalLastModified(rs.getDate("last_modified"));
                return entity;
            }
        };

        Object[] args = new Object[] { entityName, externalId };
        Entity entity = null;
        try {
            entity = jdbcTemplate.queryForObject(entitySql, entityMapper, args);
        } catch (EmptyResultDataAccessException e) {
            // do nothing just return null
        }
        if (entity != null) {
            entity.setAttributes(jdbcTemplate.query(attributeSql, new AttributeMapper(entity), args));
            entities.add(entity);
        }
        return entities;
    }

    public List<Entity> getAssociatedEntities(long externalId, final String associatedEntityName, String joinKey) {
        ParameterizedRowMapper<Entity> entityMapper = new ParameterizedRowMapper<Entity>() {
            public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
                long extId = rs.getLong("entity.ext_id");
                List<Entity> entities = getEntity(associatedEntityName, extId);
                if (entities.size() > 0)
                    return entities.get(0);
                else
                    return null;
            }
        };
        return jdbcTemplate.query(getAssociatedEntitiesSql, entityMapper, new Object[] { associatedEntityName, joinKey, Long.toString(externalId) });
    }

    private class AttributeMapper implements ParameterizedRowMapper<Attribute> {
        private Entity entity;

        public AttributeMapper(Entity entity) {
            this.entity = entity;
        }

        public Attribute mapRow(ResultSet rs, int rowNum) throws SQLException {
            Attribute attribute = new AttributeImpl(rs.getString("attribute_name"), rs.getString("value"));
            entity.addAttribute(attribute);
            return attribute;
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

}
